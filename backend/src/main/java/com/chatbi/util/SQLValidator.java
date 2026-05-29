package com.chatbi.util;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import com.alibaba.druid.util.JdbcConstants;
import com.chatbi.config.SqlConfig;
import com.chatbi.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class SQLValidator {

    private static final Pattern DANGEROUS = Pattern.compile(
            "\\b(DELETE|DROP|UPDATE|INSERT|ALTER|TRUNCATE|CREATE|GRANT|REVOKE|EXEC|EXECUTE)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private final SqlConfig sqlConfig;

    public String validateAndRewrite(String sql, Set<String> allowedTables) {
        if (sql == null || sql.isBlank()) {
            throw new BusinessException(403, "SQL 为空");
        }
        String normalized = sql.trim();
        if (DANGEROUS.matcher(normalized).find()) {
            throw new BusinessException(403, "SQL 被拦截：仅允许 SELECT 查询");
        }

        List<SQLStatement> statements;
        try {
            statements = SQLUtils.parseStatements(normalized, JdbcConstants.MYSQL);
        } catch (Exception e) {
            throw new BusinessException(403, "SQL 语法非法：" + e.getMessage());
        }
        if (statements.size() != 1 || !(statements.get(0) instanceof SQLSelectStatement)) {
            throw new BusinessException(403, "SQL 被拦截：只允许单条 SELECT 语句");
        }

        SchemaStatVisitor visitor = SQLUtils.createSchemaStatVisitor(JdbcConstants.MYSQL);
        statements.get(0).accept(visitor);
        Set<String> referencedTables = new HashSet<>();
        for (TableStat.Name name : visitor.getTables().keySet()) {
            referencedTables.add(name.getName().toLowerCase(Locale.ROOT));
        }
        for (String table : referencedTables) {
            if (!allowedTables.contains(table)) {
                throw new BusinessException(403, "SQL 被拦截：未授权访问表 " + table);
            }
        }

        return ensureLimit(normalized);
    }

    private String ensureLimit(String sql) {
        String lower = sql.toLowerCase(Locale.ROOT);
        if (lower.contains(" limit ")) {
            return sql;
        }
        int maxLimit = sqlConfig.getMaxLimit();
        int applied = Math.min(100, maxLimit);
        if (sql.endsWith(";")) {
            return sql.substring(0, sql.length() - 1) + " LIMIT " + applied;
        }
        return sql + " LIMIT " + applied;
    }
}
