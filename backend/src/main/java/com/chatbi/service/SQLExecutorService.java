package com.chatbi.service;

import com.chatbi.config.SqlConfig;
import com.chatbi.util.SQLValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SQLExecutorService {

    private final JdbcTemplate jdbcTemplate;
    private final SQLValidator sqlValidator;
    private final SqlConfig sqlConfig;
    private final DataSourceService dataSourceService;

    public List<Map<String, Object>> execute(String sql, Set<String> allowedTables, Long dataSourceId) {
        String safeSql = sqlValidator.validateAndRewrite(sql, allowedTables);
        JdbcTemplate template = resolveJdbcTemplate(dataSourceId);
        template.setQueryTimeout(sqlConfig.getQueryTimeoutMs() / 1000);
        try {
            return template.queryForList(safeSql);
        } catch (Exception e) {
            throw new com.chatbi.exception.BusinessException(501, "SQL 执行失败：" + e.getMessage());
        }
    }

    public String getExecutedSql(String sql, Set<String> allowedTables) {
        return sqlValidator.validateAndRewrite(sql, allowedTables);
    }

    private JdbcTemplate resolveJdbcTemplate(Long dataSourceId) {
        if (dataSourceId == null) {
            return jdbcTemplate;
        }
        return dataSourceService.getJdbcTemplate(dataSourceId);
    }
}
