package com.chatbi.util;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.chatbi.dto.ConversationTurn;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class PromptBuilder {

    private static final Pattern TECH_TABLE_NAME = Pattern.compile("(?i)\\bt[a-z]?_[a-z0-9_]+\\b");
    private static final Pattern TECH_FIELD_NAME = Pattern.compile("(?i)\\b[a-z]+_[a-z0-9_]+\\b");

    public String buildSqlGenerationPrompt(String question, String schemaDescription,
                                             List<ConversationTurn> history, String dbType) {
        return buildSqlGenerationPrompt(question, schemaDescription, history, dbType, null);
    }

    public String buildSqlGenerationPrompt(String question, String schemaDescription,
                                             List<ConversationTurn> history, String dbType, String timeHint) {
        String dialect = resolveDialectLabel(dbType);
        String fewShot = ResourceUtil.readUtf8Str("prompt/few-shot-examples.txt");
        StringBuilder sb = new StringBuilder();
        sb.append("你是 ").append(dialect).append(" SQL 专家。根据用户问题和数据库 Schema 生成一条可执行的 SELECT 语句。\n");
        sb.append("规则：\n");
        sb.append("1. 只输出 SQL，不要解释，不要 markdown 代码块\n");
        sb.append("2. 只允许 SELECT\n");
        sb.append("3. 必须带 LIMIT，默认不超过 100\n");
        sb.append("4. 表名、字段名必须与 Schema 一致\n");
        sb.append("5. 必须使用 ").append(dialect).append(" 语法，禁止混用其他数据库函数\n");
        sb.append("6. 多表查询时优先使用 Schema 中的表间关系进行 JOIN，单次最多 JOIN 3 张表\n");
        appendDialectDateRules(sb, dbType);
        sb.append("8. SELECT 输出列必须使用中文 AS 别名（如 product_name AS 商品名称, COUNT(*) AS 销量），便于业务人员阅读\n");
        sb.append("9. 若当前问题是延续上一轮（如换图表、追问细节），请结合历史对话理解意图，沿用相同业务主题与数据范围\n");
        sb.append("\nSchema:\n").append(schemaDescription).append("\n\n");
        if (StrUtil.isNotBlank(timeHint)) {
            sb.append(timeHint).append("\n\n");
        }
        appendConversationHistory(sb, history);
        sb.append("Few-shot 示例:\n").append(fewShot).append("\n\n");
        sb.append("用户问题: ").append(question);
        return sb.toString();
    }

    public String buildSqlFixPrompt(String question, String schemaDescription, String failedSql,
                                    String errorMessage, String dbType) {
        return buildSqlFixPrompt(question, schemaDescription, failedSql, errorMessage, dbType, null);
    }

    public String buildSqlFixPrompt(String question, String schemaDescription, String failedSql,
                                    String errorMessage, String dbType, String timeHint) {
        String dialect = resolveDialectLabel(dbType);
        StringBuilder sb = new StringBuilder();
        sb.append("之前生成的 ").append(dialect).append(" SQL 执行失败，请修正后只输出一条可执行的 SELECT。\n");
        sb.append("错误信息: ").append(errorMessage).append("\n");
        sb.append("失败 SQL:\n").append(failedSql).append("\n\n");
        appendDialectDateRules(sb, dbType);
        sb.append("\nSchema:\n").append(schemaDescription).append("\n\n");
        if (StrUtil.isNotBlank(timeHint)) {
            sb.append(timeHint).append("\n\n");
        }
        sb.append("用户问题: ").append(question);
        return sb.toString();
    }

    private void appendDialectDateRules(StringBuilder sb, String dbType) {
        String type = dbType == null ? "mysql" : dbType.toLowerCase(Locale.ROOT);
        if (type.startsWith("pg")) {
            sb.append("7. 日期格式化用 TO_CHAR(col, 'YYYY-MM')，按月分组用 TO_CHAR(col, 'YYYY-MM')\n");
            sb.append("8. 禁止使用 DATE_FORMAT、CURDATE、DATE_SUB 等 MySQL 函数\n");
            return;
        }
        if ("oracle".equals(type)) {
            sb.append("7. 日期格式化用 TO_CHAR(col, 'YYYY-MM')，按月分组用 TO_CHAR(col, 'YYYY-MM')\n");
            sb.append("8. 禁止使用 DATE_FORMAT、CURDATE、DATE_SUB 等 MySQL 函数\n");
            return;
        }
        sb.append("7. 日期格式化用 DATE_FORMAT(col, '%Y-%m')，按月分组用 DATE_FORMAT(col, '%Y-%m')\n");
        sb.append("8. 禁止使用 TO_CHAR、NVL、DECODE、DUAL、ROWNUM 等 Oracle 函数\n");
    }

    private String resolveDialectLabel(String dbType) {
        String type = dbType == null ? "mysql" : dbType.toLowerCase(Locale.ROOT);
        if (type.startsWith("pg")) {
            return "PostgreSQL";
        }
        if ("oracle".equals(type)) {
            return "Oracle";
        }
        return "MySQL";
    }

    public String buildSqlGenerationPrompt(String question, String schemaDescription, List<ConversationTurn> history) {
        return buildSqlGenerationPrompt(question, schemaDescription, history, "mysql");
    }

    public String buildExplanationPrompt(String question, String sql, String resultSummary) {
        return StrUtil.format(
                "用户问题：{}\n执行的SQL：{}\n查询结果摘要：\n{}\n\n"
                        + "请根据摘要中的具体数值与样本行，用简洁中文解释查询结果。"
                        + "必须引用关键数字（如合计、最大、最小、典型行），不要泛泛而谈。"
                        + "可使用 Markdown 列表，但不要出现数据库表名或字段名，禁止使用 ** 加粗。",
                question, sql, resultSummary
        );
    }

    public String buildDescriptiveAnswerPrompt(String question, String businessSchemaDescription, List<ConversationTurn> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是面向业务人员的数据分析助手。用户正在询问当前数据库的业务含义。\n");
        sb.append("请根据下方业务数据说明，用清晰、友好的中文回答。\n\n");
        sb.append("输出要求（必须严格遵守）：\n");
        sb.append("1. 使用 Markdown 格式，结构清晰：先写 1 句总述，再用 `### 小标题` + 无序列表分 3～4 点说明，最后 1 句总结\n");
        sb.append("2. 每个列表项单独一行，小标题与列表之间空一行\n");
        sb.append("3. 禁止出现任何数据库表名、字段名（如 tb_user、user_id）\n");
        sb.append("4. 只用中文业务名称描述\n");
        sb.append("5. 整体 150～250 字，简洁不堆砌\n");
        sb.append("6. 若当前问题是对话延续，请结合历史对话理解用户意图\n");
        sb.append("7. 禁止使用 ** 加粗语法，强调内容直接用中文引号或书面语即可\n\n");
        sb.append("业务数据说明：\n").append(businessSchemaDescription).append("\n");
        appendConversationHistory(sb, history);
        sb.append("\n用户问题: ").append(question);
        return sb.toString();
    }

    private void appendConversationHistory(StringBuilder sb, List<ConversationTurn> history) {
        if (history == null || history.isEmpty()) {
            return;
        }
        sb.append("\n历史对话（供理解上下文，最近 ").append(history.size()).append(" 轮）：\n");
        for (ConversationTurn turn : history) {
            sb.append("用户: ").append(turn.getQuestion()).append("\n");
            if (StrUtil.isNotBlank(turn.getAnswerText())) {
                String answer = turn.getAnswerText();
                if (answer.length() > 200) {
                    answer = answer.substring(0, 200) + "...";
                }
                sb.append("助手: ").append(answer).append("\n");
            }
            if ("query".equals(turn.getAnswerType()) && StrUtil.isNotBlank(turn.getGeneratedSql())) {
                sb.append("SQL: ").append(turn.getGeneratedSql()).append("\n");
            }
            if (StrUtil.isNotBlank(turn.getChartType())) {
                sb.append("图表类型: ").append(turn.getChartType()).append("\n");
            }
            sb.append("\n");
        }
    }

    /** 仅过滤技术表名/字段名，保留 Markdown 供前端渲染 */
    public String formatDescriptiveAnswer(String raw) {
        if (StrUtil.isBlank(raw)) {
            return raw;
        }
        String text = raw.trim();
        text = TECH_TABLE_NAME.matcher(text).replaceAll("");
        text = TECH_FIELD_NAME.matcher(text).replaceAll("");
        text = text.replaceAll("（\\s*[、，]\\s*）", "");
        text = text.replaceAll("（\\s*）", "");
        text = text.replaceAll("\\(\\s*\\)", "");
        text = text.replaceAll("[、，]{2,}", "，");
        // 去掉 Markdown 加粗标记，避免前端出现裸 ** 符号
        text = text.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        text = text.replace("**", "");
        // 只压缩行内多余空格，保留换行以维持 Markdown 段落/列表结构
        text = text.replaceAll("[ \\t]{2,}", " ");
        text = text.replaceAll("\\n{3,}", "\n\n");
        return text.trim();
    }

    public String extractSql(String llmOutput) {
        if (llmOutput == null) {
            return "";
        }
        String text = llmOutput.trim();
        if (text.startsWith("```")) {
            text = text.replaceAll("(?s)^```\\w*\\n?", "").replaceAll("(?s)\\n?```$", "").trim();
        }
        int selectIdx = text.toLowerCase().indexOf("select");
        if (selectIdx > 0) {
            text = text.substring(selectIdx);
        }
        return text.trim();
    }
}
