-- 系统配置表：持久化大模型等运行时配置
USE chatbi;

CREATE TABLE IF NOT EXISTS system_config (
    config_key VARCHAR(64) PRIMARY KEY COMMENT '配置键',
    config_value TEXT COMMENT '配置值 JSON',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
