-- 字段语义层 + 常用问题模板
USE chatbi;

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

INSERT INTO query_template (data_source_id, title, question, category, chart_type, sort_order) VALUES
(NULL, '商品总数', '总共有多少个商品？', 'query', 'auto', 1),
(NULL, '分类统计', '每个分类的商品数量是多少？', 'query', 'bar', 2),
(NULL, '库表介绍', '这个数据库主要存储了什么信息？', 'chat', 'auto', 3);
