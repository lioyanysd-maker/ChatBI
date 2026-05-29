package com.chatbi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseMigrationRunner {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateOnStartup() {
        tryAddColumn("chat_session", "data_source_id",
                "ALTER TABLE chat_session ADD COLUMN data_source_id BIGINT NULL COMMENT '绑定的数据源' AFTER title");
        tryAddColumn("chat_query_log", "data_source_id",
                "ALTER TABLE chat_query_log ADD COLUMN data_source_id BIGINT NULL COMMENT '查询时使用的数据源' AFTER session_id");
        tryCreateTable("column_semantic", """
                CREATE TABLE column_semantic (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    data_source_id BIGINT NOT NULL,
                    table_name VARCHAR(100) NOT NULL,
                    column_name VARCHAR(100) NOT NULL,
                    business_name VARCHAR(100) COMMENT '中文业务名',
                    description VARCHAR(500) COMMENT '口径/说明',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_ds_table_col (data_source_id, table_name, column_name)
                )
                """);
        tryCreateTable("query_template", """
                CREATE TABLE query_template (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    data_source_id BIGINT NULL COMMENT '关联数据源，空为通用',
                    title VARCHAR(100) NOT NULL COMMENT '展示标题',
                    question VARCHAR(500) NOT NULL COMMENT '问题内容',
                    category VARCHAR(20) DEFAULT 'query' COMMENT 'query-数据查询 chat-描述问答',
                    chart_type VARCHAR(20) DEFAULT 'auto',
                    sort_order INT DEFAULT 0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);
        tryAddColumn("query_template", "category",
                "ALTER TABLE query_template ADD COLUMN category VARCHAR(20) DEFAULT 'query' COMMENT 'query-数据查询 chat-描述问答' AFTER question");
        seedQueryTemplates();
    }

    private void tryAddColumn(String table, String column, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """,
                Integer.class,
                table,
                column
        );
        if (count != null && count == 0) {
            jdbcTemplate.execute(ddl);
            log.info("Applied migration: added {}.{}", table, column);
        }
    }

    private void tryCreateTable(String table, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """,
                Integer.class,
                table
        );
        if (count != null && count == 0) {
            jdbcTemplate.execute(ddl);
            log.info("Applied migration: created table {}", table);
        }
    }

    private void seedQueryTemplates() {
        if (!tableExists("query_template")) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM query_template", Integer.class);
        if (count != null && count == 0) {
            jdbcTemplate.update("""
                    INSERT INTO query_template (data_source_id, title, question, category, chart_type, sort_order) VALUES
                    (NULL, '商品总数', '总共有多少个商品？', 'query', 'auto', 1),
                    (NULL, '分类统计', '每个分类的商品数量是多少？', 'query', 'bar', 2),
                    (NULL, '库表介绍', '这个数据库主要存储了什么信息？', 'chat', 'auto', 3)
                    """);
            log.info("Applied migration: seeded query_template");
        }
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """,
                Integer.class,
                table
        );
        return count != null && count > 0;
    }
}
