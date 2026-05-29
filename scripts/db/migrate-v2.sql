-- 已有数据库升级：若字段已存在可忽略报错
USE chatbi;

ALTER TABLE chat_query_log ADD COLUMN answer_text TEXT NULL COMMENT '回答文本' AFTER chart_type;
ALTER TABLE chat_query_log ADD COLUMN answer_type VARCHAR(20) NULL COMMENT 'query/chat' AFTER answer_text;
