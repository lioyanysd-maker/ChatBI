-- ChatBI 系统表 + 演示业务表
-- 执行前请先创建数据库: CREATE DATABASE chatbi DEFAULT CHARSET utf8mb4;

USE chatbi;

-- ========== 系统表 ==========

CREATE TABLE IF NOT EXISTS system_config (
    config_key VARCHAR(64) PRIMARY KEY COMMENT '配置键',
    config_value TEXT COMMENT '配置值 JSON',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL UNIQUE COMMENT '会话唯一标识',
    user_id VARCHAR(32) COMMENT '用户ID（后续扩展）',
    title VARCHAR(200) COMMENT '会话标题（首问截取）',
    data_source_id BIGINT NULL COMMENT '绑定的数据源',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chat_query_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL COMMENT '所属会话',
    data_source_id BIGINT NULL COMMENT '查询时使用的数据源',
    question VARCHAR(500) NOT NULL COMMENT '用户问题',
    generated_sql TEXT COMMENT '生成的SQL',
    executed_sql VARCHAR(1000) COMMENT '实际执行的SQL（可能被改写）',
    is_safe BOOLEAN DEFAULT TRUE COMMENT '是否通过安全校验',
    error_message VARCHAR(500) COMMENT '错误信息',
    row_count INT DEFAULT 0 COMMENT '返回行数',
    execution_time_ms INT COMMENT '执行耗时',
    chart_type VARCHAR(20) COMMENT '使用的图表类型',
    chart_data MEDIUMTEXT COMMENT '图表/表格数据 JSON',
    answer_text TEXT COMMENT '回答文本',
    answer_type VARCHAR(20) COMMENT 'query/chat',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    INDEX idx_created (created_at)
);

CREATE TABLE IF NOT EXISTS data_source (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '数据源名称',
    db_type VARCHAR(20) DEFAULT 'mysql' COMMENT 'mysql/pg/oracle',
    host VARCHAR(100) NOT NULL,
    port INT NOT NULL,
    database_name VARCHAR(100) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(200) NOT NULL COMMENT '加密存储',
    schema_info TEXT COMMENT '缓存的Schema信息',
    status TINYINT DEFAULT 1 COMMENT '1-启用 0-禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS table_whitelist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    data_source_id BIGINT NOT NULL DEFAULT 1,
    table_name VARCHAR(100) NOT NULL,
    table_comment VARCHAR(200) COMMENT '中文描述',
    is_active TINYINT DEFAULT 1,
    UNIQUE KEY uk_ds_table (data_source_id, table_name)
);

CREATE TABLE IF NOT EXISTS column_semantic (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    data_source_id BIGINT NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    business_name VARCHAR(100) COMMENT '中文业务名',
    description VARCHAR(500) COMMENT '口径/说明',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ds_table_col (data_source_id, table_name, column_name)
);

CREATE TABLE IF NOT EXISTS query_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    data_source_id BIGINT NULL COMMENT '关联数据源，空为通用',
    title VARCHAR(100) NOT NULL COMMENT '展示标题',
    question VARCHAR(500) NOT NULL COMMENT '问题内容',
    category VARCHAR(20) DEFAULT 'query' COMMENT 'query-数据查询 chat-描述问答',
    chart_type VARCHAR(20) DEFAULT 'auto',
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cache_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cache_key VARCHAR(200),
    hit_count INT DEFAULT 0,
    miss_count INT DEFAULT 0,
    last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ========== 演示业务表 ==========

CREATE TABLE IF NOT EXISTS products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '商品名称',
    category VARCHAR(50) NOT NULL COMMENT '分类',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '用户名',
    city VARCHAR(50) COMMENT '城市',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL COMMENT '订单金额',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_product (product_id),
    INDEX idx_create_time (create_time)
);

-- ========== 种子数据 ==========

INSERT INTO table_whitelist (data_source_id, table_name, table_comment, is_active) VALUES
(1, 'products', '商品表', 1),
(1, 'users', '用户表', 1),
(1, 'orders', '订单表', 1);

INSERT INTO query_template (data_source_id, title, question, category, chart_type, sort_order) VALUES
(NULL, '商品总数', '总共有多少个商品？', 'query', 'auto', 1),
(NULL, '分类统计', '每个分类的商品数量是多少？', 'query', 'bar', 2),
(NULL, '库表介绍', '这个数据库主要存储了什么信息？', 'chat', 'auto', 3);

INSERT INTO products (name, category, price) VALUES
('iPhone 15', '手机', 6999.00),
('华为 Mate 60', '手机', 5999.00),
('小米 14', '手机', 3999.00),
('MacBook Pro', '电脑', 12999.00),
('ThinkPad X1', '电脑', 8999.00);

INSERT INTO users (name, city) VALUES
('张三', '上海'),
('李四', '北京'),
('王五', '上海'),
('赵六', '深圳');

INSERT INTO orders (user_id, product_id, amount, create_time) VALUES
(1, 1, 6999.00, DATE_SUB(NOW(), INTERVAL 15 DAY)),
(2, 2, 5999.00, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(3, 1, 6999.00, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(1, 3, 3999.00, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(4, 4, 12999.00, DATE_SUB(NOW(), INTERVAL 35 DAY)),
(3, 2, 5999.00, DATE_SUB(NOW(), INTERVAL 40 DAY));
