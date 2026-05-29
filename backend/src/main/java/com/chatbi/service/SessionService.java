package com.chatbi.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.dto.response.ChartData;
import com.chatbi.dto.response.ChatMessageResponse;
import com.chatbi.dto.response.SessionMessagesResponse;
import com.chatbi.dto.response.SessionSummaryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chatbi.entity.ChatQueryLog;
import com.chatbi.entity.ChatSession;
import com.chatbi.exception.BusinessException;
import com.chatbi.mapper.ChatQueryLogMapper;
import com.chatbi.mapper.ChatSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatQueryLogMapper chatQueryLogMapper;
    private final ObjectMapper objectMapper;

    public List<SessionSummaryResponse> listSessions(int limit) {
        List<ChatSession> sessions = chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .orderByDesc(ChatSession::getUpdatedAt)
                        .last("LIMIT " + Math.min(limit, 50))
        );
        if (sessions.isEmpty()) {
            return List.of();
        }

        List<String> sessionIds = sessions.stream().map(ChatSession::getSessionId).toList();
        List<ChatQueryLog> counts = chatQueryLogMapper.selectList(
                new LambdaQueryWrapper<ChatQueryLog>().in(ChatQueryLog::getSessionId, sessionIds)
        );
        Map<String, Long> countMap = counts.stream()
                .collect(Collectors.groupingBy(ChatQueryLog::getSessionId, Collectors.counting()));

        return sessions.stream().map(s -> SessionSummaryResponse.builder()
                .sessionId(s.getSessionId())
                .title(StrUtil.blankToDefault(s.getTitle(), "新对话"))
                .dataSourceId(s.getDataSourceId())
                .updatedAt(s.getUpdatedAt() != null ? s.getUpdatedAt() : s.getCreatedAt())
                .messageCount(countMap.getOrDefault(s.getSessionId(), 0L).intValue())
                .build()).toList();
    }

    public SessionMessagesResponse getSessionMessages(String sessionId) {
        ChatSession session = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getSessionId, sessionId)
        );
        if (session == null) {
            throw new BusinessException(404, "对话不存在");
        }

        List<ChatQueryLog> logs = chatQueryLogMapper.selectList(
                new LambdaQueryWrapper<ChatQueryLog>()
                        .eq(ChatQueryLog::getSessionId, sessionId)
                        .orderByAsc(ChatQueryLog::getCreatedAt)
        );

        List<ChatMessageResponse> messages = new ArrayList<>();
        for (ChatQueryLog log : logs) {
            messages.add(ChatMessageResponse.builder()
                    .role("user")
                    .content(log.getQuestion())
                    .build());

            String content = resolveAssistantContent(log);
            messages.add(ChatMessageResponse.builder()
                    .role("assistant")
                    .content(content)
                    .answerType(log.getAnswerType())
                    .sql(StrUtil.blankToDefault(log.getExecutedSql(), log.getGeneratedSql()))
                    .rowCount(log.getRowCount())
                    .executionTimeMs(log.getExecutionTimeMs() != null ? log.getExecutionTimeMs().longValue() : null)
                    .chartData(parseChartData(log.getChartData()))
                    .build());
        }
        return SessionMessagesResponse.builder()
                .sessionId(session.getSessionId())
                .dataSourceId(session.getDataSourceId())
                .messages(messages)
                .build();
    }

    @Transactional
    public void deleteSessions(List<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            throw new BusinessException(400, "请选择要删除的对话");
        }
        List<String> ids = sessionIds.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            throw new BusinessException(400, "请选择要删除的对话");
        }

        chatQueryLogMapper.delete(
                new LambdaQueryWrapper<ChatQueryLog>().in(ChatQueryLog::getSessionId, ids)
        );
        chatSessionMapper.delete(
                new LambdaQueryWrapper<ChatSession>().in(ChatSession::getSessionId, ids)
        );
    }

    private String resolveAssistantContent(ChatQueryLog log) {
        if (StrUtil.isNotBlank(log.getAnswerText())) {
            return log.getAnswerText();
        }
        if (StrUtil.isNotBlank(log.getErrorMessage())) {
            return log.getErrorMessage();
        }
        if (log.getRowCount() != null && log.getRowCount() > 0) {
            return "查询完成，返回 " + log.getRowCount() + " 行数据。";
        }
        return "已完成";
    }

    private ChartData parseChartData(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ChartData.class);
        } catch (Exception ex) {
            log.warn("Failed to parse chart data: {}", ex.getMessage());
            return null;
        }
    }
}
