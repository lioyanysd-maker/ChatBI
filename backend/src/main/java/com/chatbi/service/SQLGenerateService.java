package com.chatbi.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.dto.ConversationTurn;
import com.chatbi.dto.request.ExecuteSqlRequest;
import com.chatbi.dto.request.QueryRequest;
import com.chatbi.dto.response.ChartData;
import com.chatbi.dto.response.QueryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chatbi.entity.ChatQueryLog;
import com.chatbi.entity.ChatSession;
import com.chatbi.exception.BusinessException;
import com.chatbi.mapper.ChatQueryLogMapper;
import com.chatbi.mapper.ChatSessionMapper;
import com.chatbi.util.PromptBuilder;
import com.chatbi.util.QuestionIntentClassifier;
import com.chatbi.util.TimeParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class SQLGenerateService {

    private final LLMService llmService;
    private final SchemaService schemaService;
    private final SQLExecutorService sqlExecutorService;
    private final ChartService chartService;
    private final PromptBuilder promptBuilder;
    private final QuestionIntentClassifier intentClassifier;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatQueryLogMapper chatQueryLogMapper;
    private final ObjectMapper objectMapper;
    private final DataSourceService dataSourceService;
    private final QueryCacheService queryCacheService;
    private final TimeParser timeParser;

    public QueryResponse query(QueryRequest request) {
        long start = System.currentTimeMillis();
        String sessionId = ensureSession(request.getSessionId(), request.getQuestion(), request.getDataSourceId());

        Optional<QueryResponse> cached = queryCacheService.get(
                request.getDataSourceId(), request.getQuestion(), request.getChartType(), sessionId);
        if (cached.isPresent()) {
            return mergeCachedResponse(cached.get(), sessionId, request, start);
        }

        QueryResponse response = doQuery(request, sessionId, start, null);
        queryCacheService.put(
                request.getDataSourceId(), request.getQuestion(), request.getChartType(), sessionId, response);
        return response;
    }

    public void queryStream(QueryRequest request, SseEmitter emitter) {
        CompletableFuture.runAsync(() -> {
            SseQueryStreamSink sink = new SseQueryStreamSink(objectMapper, emitter);
            try {
                long start = System.currentTimeMillis();
                String sessionId = ensureSession(request.getSessionId(), request.getQuestion(), request.getDataSourceId());

                Optional<QueryResponse> cached = queryCacheService.get(
                        request.getDataSourceId(), request.getQuestion(), request.getChartType(), sessionId);
                if (cached.isPresent()) {
                    QueryResponse response = mergeCachedResponse(cached.get(), sessionId, request, start);
                    sink.sendStep("命中缓存，正在返回结果…");
                    emitCachedPreview(sink, response);
                    sink.sendDone(response);
                    emitter.complete();
                    return;
                }

                QueryResponse response = doQuery(request, sessionId, start, sink);
                queryCacheService.put(
                        request.getDataSourceId(), request.getQuestion(), request.getChartType(), sessionId, response);
                sink.sendDone(response);
                emitter.complete();
            } catch (BusinessException ex) {
                completeStreamWithError(emitter, sink, ex.getMessage());
            } catch (Exception ex) {
                log.error("Stream query failed", ex);
                completeStreamWithError(emitter, sink, ex.getMessage() != null ? ex.getMessage() : "查询失败");
            }
        });
    }

    public QueryResponse executeSql(ExecuteSqlRequest request) {
        long start = System.currentTimeMillis();
        String question = StrUtil.blankToDefault(request.getQuestion(), "手动执行 SQL");

        QueryRequest queryRequest = new QueryRequest();
        queryRequest.setQuestion(question);
        queryRequest.setSessionId(request.getSessionId());
        queryRequest.setChartType(request.getChartType());
        queryRequest.setDataSourceId(request.getDataSourceId());

        String sessionId = ensureSession(request.getSessionId(), question, request.getDataSourceId());
        Set<String> allowedTables = schemaService.getAllowedTableNames(request.getDataSourceId());
        String effectiveChartType = chartService.inferPreferredChartType(question, request.getChartType());

        String executedSql = sqlExecutorService.getExecutedSql(request.getSql(), allowedTables);
        List<Map<String, Object>> rows = sqlExecutorService.execute(
                request.getSql(), allowedTables, request.getDataSourceId());

        var chartData = chartService.buildChartData(
                rows, effectiveChartType, question,
                schemaService.resolveColumnLabels(request.getDataSourceId(), extractColumnKeys(rows)));
        String explanation = buildExplanation(question, executedSql, rows, request.getDataSourceId(), null);
        explanation = promptBuilder.formatDescriptiveAnswer(explanation);

        saveLog(sessionId, queryRequest, request.getSql(), executedSql, true, null, rows.size(), start,
                chartData.getType(), explanation, "query", chartData);

        return QueryResponse.builder()
                .sessionId(sessionId)
                .question(question)
                .answerType("query")
                .generatedSql(executedSql)
                .chartData(chartData)
                .naturalExplanation(explanation)
                .rowCount(rows.size())
                .executionTimeMs(System.currentTimeMillis() - start)
                .rawRows(rows)
                .build();
    }

    private QueryResponse doQuery(QueryRequest request, String sessionId, long start, QueryStreamSink sink) {
        List<ConversationTurn> history = loadHistoryTurns(sessionId);

        QuestionIntentClassifier.Intent intent = intentClassifier.classify(request.getQuestion());
        if (intent == QuestionIntentClassifier.Intent.DESCRIPTIVE
                && intentClassifier.isContextFollowUp(request.getQuestion())
                && hasQueryTurn(history)) {
            intent = QuestionIntentClassifier.Intent.SQL_QUERY;
        }
        if (intent == QuestionIntentClassifier.Intent.DESCRIPTIVE) {
            return answerDescriptiveQuestion(request, sessionId, start, history, sink);
        }
        return answerSqlQuestion(request, sessionId, start, history, sink);
    }

    private QueryResponse answerDescriptiveQuestion(QueryRequest request, String sessionId, long start,
                                                      List<ConversationTurn> history, QueryStreamSink sink) {
        sendStep(sink, "正在理解您的问题…");
        String businessSchema = schemaService.buildBusinessSchemaDescription(request.getDataSourceId());
        String prompt = promptBuilder.buildDescriptiveAnswerPrompt(
                request.getQuestion(), businessSchema, history);

        String answer;
        if (sink == null) {
            answer = promptBuilder.formatDescriptiveAnswer(llmService.chat(prompt));
        } else {
            StringBuilder raw = new StringBuilder();
            llmService.chatStream(prompt, chunk -> {
                raw.append(chunk);
                sendDelta(sink, chunk);
            });
            answer = promptBuilder.formatDescriptiveAnswer(raw.toString());
        }

        saveLog(sessionId, request, null, null, true, null, 0, start, "chat", answer, "chat", null);

        return QueryResponse.builder()
                .sessionId(sessionId)
                .question(request.getQuestion())
                .answerType("chat")
                .naturalExplanation(answer)
                .rowCount(0)
                .executionTimeMs(System.currentTimeMillis() - start)
                .build();
    }

    private QueryResponse answerSqlQuestion(QueryRequest request, String sessionId, long start,
                                          List<ConversationTurn> history, QueryStreamSink sink) {
        Set<String> allowedTables = schemaService.getAllowedTableNames(request.getDataSourceId());
        String schema = schemaService.buildSchemaDescription(request.getDataSourceId());
        String dbType = dataSourceService.resolveDbType(request.getDataSourceId());
        String effectiveChartType = chartService.inferPreferredChartType(
                request.getQuestion(), request.getChartType());

        ConversationTurn lastQuery = findLastQueryTurn(history);
        if (lastQuery != null && intentClassifier.isChartOnlyFollowUp(request.getQuestion())) {
            return answerWithExistingSql(request, sessionId, start, lastQuery, effectiveChartType, allowedTables, sink);
        }

        sendStep(sink, "正在生成 SQL…");
        String effectiveQuestion = resolveEffectiveQuestion(request.getQuestion(), history);
        String timeHint = timeParser.buildSqlTimeHint(effectiveQuestion);
        String prompt = promptBuilder.buildSqlGenerationPrompt(
                effectiveQuestion, schema, history, dbType, timeHint);
        String llmOutput = llmService.chat(prompt);
        String generatedSql = promptBuilder.extractSql(llmOutput);
        sendSql(sink, generatedSql);

        sendStep(sink, "正在查询数据库…");
        ExecutedQuery executed;
        try {
            executed = runSqlWithRetry(
                    effectiveQuestion, schema, dbType, generatedSql, allowedTables, request.getDataSourceId(), sink, timeHint);
        } catch (BusinessException ex) {
            saveLog(sessionId, request, generatedSql, null, false, ex.getMessage(), 0, start,
                    effectiveChartType, ex.getMessage(), "query", null);
            throw ex;
        }
        generatedSql = executed.generatedSql();
        String executedSql = executed.executedSql();
        List<Map<String, Object>> rows = executed.rows();

        var chartData = chartService.buildChartData(
                rows, effectiveChartType, effectiveQuestion,
                schemaService.resolveColumnLabels(request.getDataSourceId(), extractColumnKeys(rows)));
        sendChart(sink, chartData);

        sendStep(sink, "正在生成解释…");
        String explanation = buildExplanation(effectiveQuestion, executedSql, rows, request.getDataSourceId(), sink);
        explanation = promptBuilder.formatDescriptiveAnswer(explanation);

        saveLog(sessionId, request, generatedSql, executedSql, true, null, rows.size(), start,
                chartData.getType(), explanation, "query", chartData);

        return QueryResponse.builder()
                .sessionId(sessionId)
                .question(request.getQuestion())
                .answerType("query")
                .generatedSql(executedSql)
                .chartData(chartData)
                .naturalExplanation(explanation)
                .rowCount(rows.size())
                .executionTimeMs(System.currentTimeMillis() - start)
                .rawRows(rows)
                .build();
    }

    private String buildExplanation(String question, String sql, List<Map<String, Object>> rows,
                                    Long dataSourceId, QueryStreamSink sink) {
        try {
            Map<String, String> columnLabels = schemaService.resolveColumnLabels(
                    dataSourceId, extractColumnKeys(rows));
            String summary = chartService.summarizeRows(rows, columnLabels);
            String prompt = promptBuilder.buildExplanationPrompt(question, sql, summary);
            if (sink == null) {
                return llmService.chat(prompt);
            }
            StringBuilder raw = new StringBuilder();
            llmService.chatStream(prompt, chunk -> {
                raw.append(chunk);
                sendDelta(sink, chunk);
            });
            return raw.toString();
        } catch (Exception ex) {
            log.warn("Explanation generation skipped: {}", ex.getMessage());
            String fallback = chartService.summarizeRows(rows);
            sendDelta(sink, fallback);
            return fallback;
        }
    }

    private QueryResponse answerWithExistingSql(QueryRequest request, String sessionId, long start,
                                                ConversationTurn lastQuery, String chartType,
                                                Set<String> allowedTables, QueryStreamSink sink) {
        sendStep(sink, "正在复用上一轮查询…");
        String sqlToRun = StrUtil.isNotBlank(lastQuery.getGeneratedSql())
                ? lastQuery.getGeneratedSql()
                : null;
        if (StrUtil.isBlank(sqlToRun)) {
            throw new BusinessException(400, "无法复用上一轮查询，请重新描述您的数据需求");
        }

        sendSql(sink, sqlToRun);
        sendStep(sink, "正在查询数据库…");
        String executedSql = sqlExecutorService.getExecutedSql(sqlToRun, allowedTables);
        List<Map<String, Object>> rows = sqlExecutorService.execute(
                sqlToRun, allowedTables, request.getDataSourceId());

        String effectiveQuestion = lastQuery.getQuestion() + "（" + request.getQuestion() + "）";
        var chartData = chartService.buildChartData(
                rows, chartType, effectiveQuestion,
                schemaService.resolveColumnLabels(request.getDataSourceId(), extractColumnKeys(rows)));
        sendChart(sink, chartData);

        sendStep(sink, "正在生成解释…");
        String explanation = buildExplanation(effectiveQuestion, executedSql, rows, request.getDataSourceId(), sink);
        explanation = promptBuilder.formatDescriptiveAnswer(explanation);

        saveLog(sessionId, request, sqlToRun, executedSql, true, null, rows.size(), start,
                chartData.getType(), explanation, "query", chartData);

        return QueryResponse.builder()
                .sessionId(sessionId)
                .question(request.getQuestion())
                .answerType("query")
                .generatedSql(executedSql)
                .chartData(chartData)
                .naturalExplanation(explanation)
                .rowCount(rows.size())
                .executionTimeMs(System.currentTimeMillis() - start)
                .rawRows(rows)
                .build();
    }

    private QueryResponse mergeCachedResponse(QueryResponse cached, String sessionId, QueryRequest request, long start) {
        return cached.toBuilder()
                .sessionId(sessionId)
                .question(request.getQuestion())
                .executionTimeMs(System.currentTimeMillis() - start)
                .build();
    }

    private void emitCachedPreview(QueryStreamSink sink, QueryResponse response) {
        if (StrUtil.isNotBlank(response.getGeneratedSql())) {
            sink.sendSql(response.getGeneratedSql());
        }
        if (response.getChartData() != null) {
            sink.sendChart(response.getChartData());
        }
        if (StrUtil.isNotBlank(response.getNaturalExplanation())) {
            sink.sendDelta(response.getNaturalExplanation());
        }
    }

    private void completeStreamWithError(SseEmitter emitter, SseQueryStreamSink sink, String message) {
        try {
            sink.sendError(message);
            emitter.complete();
        } catch (Exception ex) {
            emitter.completeWithError(ex);
        }
    }

    private void sendStep(QueryStreamSink sink, String message) {
        if (sink != null) {
            sink.sendStep(message);
        }
    }

    private void sendSql(QueryStreamSink sink, String sql) {
        if (sink != null && StrUtil.isNotBlank(sql)) {
            sink.sendSql(sql);
        }
    }

    private void sendChart(QueryStreamSink sink, ChartData chartData) {
        if (sink != null && chartData != null) {
            sink.sendChart(chartData);
        }
    }

    private void sendDelta(QueryStreamSink sink, String text) {
        if (sink != null && StrUtil.isNotBlank(text)) {
            sink.sendDelta(text);
        }
    }

    private String ensureSession(String sessionId, String question, Long dataSourceId) {
        if (StrUtil.isNotBlank(sessionId)) {
            bindSessionDataSource(sessionId, dataSourceId);
            return sessionId;
        }
        String newSessionId = "session_" + IdUtil.fastSimpleUUID();
        ChatSession session = new ChatSession();
        session.setSessionId(newSessionId);
        session.setTitle(StrUtil.sub(question, 0, 50));
        session.setDataSourceId(dataSourceId);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.insert(session);
        return newSessionId;
    }

    private void bindSessionDataSource(String sessionId, Long dataSourceId) {
        if (dataSourceId == null) {
            return;
        }
        ChatSession session = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getSessionId, sessionId)
        );
        if (session == null) {
            throw new BusinessException(404, "对话不存在");
        }
        if (session.getDataSourceId() == null) {
            session.setDataSourceId(dataSourceId);
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionMapper.updateById(session);
            return;
        }
        if (!session.getDataSourceId().equals(dataSourceId)) {
            throw new BusinessException(400, "当前对话绑定其他数据源，请新建对话或切换回对应数据源");
        }
    }

    private boolean hasQueryTurn(List<ConversationTurn> history) {
        return findLastQueryTurn(history) != null;
    }

    private ConversationTurn findLastQueryTurn(List<ConversationTurn> history) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            ConversationTurn turn = history.get(i);
            if ("query".equals(turn.getAnswerType()) && StrUtil.isNotBlank(turn.getGeneratedSql())) {
                return turn;
            }
        }
        return null;
    }

    private String resolveEffectiveQuestion(String question, List<ConversationTurn> history) {
        if (!intentClassifier.isContextFollowUp(question) || history == null || history.isEmpty()) {
            return question;
        }
        ConversationTurn last = history.get(history.size() - 1);
        if (StrUtil.isBlank(last.getQuestion())) {
            return question;
        }
        return last.getQuestion() + "。" + question;
    }

    private List<ConversationTurn> loadHistoryTurns(String sessionId) {
        List<ChatQueryLog> logs = chatQueryLogMapper.selectList(
                new LambdaQueryWrapper<ChatQueryLog>()
                        .eq(ChatQueryLog::getSessionId, sessionId)
                        .eq(ChatQueryLog::getIsSafe, true)
                        .orderByDesc(ChatQueryLog::getCreatedAt)
                        .last("LIMIT 5")
        );
        List<ConversationTurn> history = new ArrayList<>();
        for (int i = logs.size() - 1; i >= 0; i--) {
            ChatQueryLog log = logs.get(i);
            history.add(ConversationTurn.builder()
                    .question(log.getQuestion())
                    .answerType(log.getAnswerType())
                    .answerText(log.getAnswerText())
                    .generatedSql(StrUtil.isNotBlank(log.getExecutedSql())
                            ? log.getExecutedSql()
                            : log.getGeneratedSql())
                    .chartType(log.getChartType())
                    .build());
        }
        return history;
    }

    private void saveLog(String sessionId, QueryRequest request, String generatedSql, String executedSql,
                       boolean safe, String error, int rowCount, long start, String chartType,
                       String answerText, String answerType, ChartData chartData) {
        ChatQueryLog logEntity = new ChatQueryLog();
        logEntity.setSessionId(sessionId);
        logEntity.setDataSourceId(request.getDataSourceId());
        logEntity.setQuestion(request.getQuestion());
        logEntity.setGeneratedSql(generatedSql);
        logEntity.setExecutedSql(executedSql);
        logEntity.setIsSafe(safe);
        logEntity.setErrorMessage(error);
        logEntity.setRowCount(rowCount);
        logEntity.setExecutionTimeMs((int) (System.currentTimeMillis() - start));
        logEntity.setChartType(chartType);
        logEntity.setChartData(serializeChartData(chartData));
        logEntity.setAnswerText(answerText);
        logEntity.setAnswerType(answerType);
        logEntity.setCreatedAt(LocalDateTime.now());
        chatQueryLogMapper.insert(logEntity);

        chatSessionMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ChatSession>()
                .eq(ChatSession::getSessionId, sessionId)
                .set(ChatSession::getUpdatedAt, LocalDateTime.now()));
    }

    private ExecutedQuery runSqlWithRetry(String effectiveQuestion, String schema, String dbType,
                                          String initialSql, Set<String> allowedTables, Long dataSourceId,
                                          QueryStreamSink sink, String timeHint) {
        String sql = initialSql;
        BusinessException lastError = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String executedSql = sqlExecutorService.getExecutedSql(sql, allowedTables);
                List<Map<String, Object>> rows = sqlExecutorService.execute(sql, allowedTables, dataSourceId);
                return new ExecutedQuery(sql, executedSql, rows);
            } catch (BusinessException ex) {
                lastError = ex;
                if (attempt == 0 && isRetryableSqlError(ex)) {
                    sendStep(sink, "SQL 执行失败，正在自动修复…");
                    String fixPrompt = promptBuilder.buildSqlFixPrompt(
                            effectiveQuestion, schema, sql, ex.getMessage(), dbType, timeHint);
                    sql = promptBuilder.extractSql(llmService.chat(fixPrompt));
                    sendSql(sink, sql);
                    continue;
                }
                throw ex;
            }
        }
        throw lastError != null ? lastError : new BusinessException(501, "SQL 执行失败");
    }

    private record ExecutedQuery(String generatedSql, String executedSql, List<Map<String, Object>> rows) {}

    private boolean isRetryableSqlError(BusinessException ex) {
        if (ex.getCode() != 501 || ex.getMessage() == null) {
            return false;
        }
        String message = ex.getMessage().toLowerCase(Locale.ROOT);
        return message.contains("bad sql grammar")
                || message.contains("syntax error")
                || message.contains("sql syntax")
                || message.contains("unknown column")
                || message.contains("doesn't exist")
                || message.contains("does not exist")
                || message.contains("not found")
                || message.contains("ambiguous")
                || message.contains("invalid")
                || message.contains("to_char")
                || message.contains("date_format")
                || message.contains("group by")
                || message.contains("aggregate");
    }

    private List<String> extractColumnKeys(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(rows.get(0).keySet());
    }

    private String serializeChartData(ChartData chartData) {
        if (chartData == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(chartData);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize chart data: {}", ex.getMessage());
            return null;
        }
    }
}
