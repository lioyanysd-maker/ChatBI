-- 已有数据库升级：保存查询结果图表/表格数据，切换对话后可恢复展示
USE chatbi;

ALTER TABLE chat_query_log ADD COLUMN chart_data MEDIUMTEXT NULL COMMENT '图表/表格数据 JSON' AFTER chart_type;
