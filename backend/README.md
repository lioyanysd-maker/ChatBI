# 后端

Spring Boot 3.2 + MyBatis-Plus + MySQL + Redis + Druid SQL Parser。

完整文档见仓库根目录 [README.md](../README.md)。

## 快速启动

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# 编辑 application-local.yml 填入 MySQL 密码与 LLM API Key

mvn spring-boot:run
```

默认 http://localhost:8080

## 配置要点

- 密钥通过 `application-local.yml` 或环境变量注入，勿提交真实配置
- 查询缓存：`chatbi.cache.redis-enabled` + `chatbi.security.query-cache-ttl-minutes`
- 详见根目录 `.env.example`
