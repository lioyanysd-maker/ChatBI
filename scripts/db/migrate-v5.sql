-- 会话与查询日志绑定数据源
USE chatbi;

ALTER TABLE chat_session
    ADD COLUMN data_source_id BIGINT NULL COMMENT '绑定的数据源' AFTER title;

ALTER TABLE chat_query_log
    ADD COLUMN data_source_id BIGINT NULL COMMENT '查询时使用的数据源' AFTER session_id;
