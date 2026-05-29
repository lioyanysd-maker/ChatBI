-- ChatBI 复杂数据库压测库（与 chatbi 系统库完全独立）
-- 用途：模拟用户自建业务库，测试 NL2SQL / 多表 JOIN / 聚合分析
-- 数据库名：chatbi_bench

CREATE DATABASE IF NOT EXISTS chatbi_bench
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- 可选：创建专用账号（本地开发用，生产请自行调整权限）
-- CREATE USER IF NOT EXISTS 'bench'@'%' IDENTIFIED BY 'bench123';
-- GRANT SELECT ON chatbi_bench.* TO 'bench'@'%';
-- FLUSH PRIVILEGES;

USE chatbi_bench;
