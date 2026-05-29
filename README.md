# ChatBI

**自然语言数据分析平台（NL2SQL）** — 用中文提问，自动生成 SQL、查询数据库、渲染图表，并给出基于真实数据的 AI 解释。

> 本目录为 **GitHub 发布版**：已排除开发密钥、构建产物与 IDE 配置。使用前请复制 `.env.example` / `application-local.yml.example` 并填入自己的配置。

---

## 目录
- [项目预览](#项目预览)
- [功能特性](#功能特性)
- [项目结构](#项目结构)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [数据库初始化](#数据库初始化)
- [API 概览](#api-概览)
- [安全说明](#安全说明)
- [开发与测试](#开发与测试)
- [常见问题](#常见问题)


---
## 项目预览
### 自然语言转 SQL
<img width="1865" height="920" alt="image" src="https://github.com/user-attachments/assets/f3351a5e-d5cb-4712-b17f-f022fb0fd383" />
<img width="1865" height="920" alt="image" src="https://github.com/user-attachments/assets/43120b7e-1e40-4f49-a301-48f37c503fca" />
### 大模型与数据库配置
<img width="1860" height="915" alt="image" src="https://github.com/user-attachments/assets/e9f49db8-83b9-4087-9938-4df7fff0bf3a" />
### 能够理解上下文
<img width="1802" height="920" alt="image" src="https://github.com/user-attachments/assets/98f114be-94f3-4df7-acc6-1e29355f50d0" />

## 功能特性

### 核心能力

| 模块 | 说明 |
|------|------|
| **自然语言转 SQL** | 基于 Schema + Few-shot + 多轮上下文生成 `SELECT` 语句 |
| **流式对话** | SSE 推送步骤、SQL、图表、解释文本（`/api/chatbi/query/stream`） |
| **智能图表** | 自动识别柱状图 / 折线图 / 饼图 / 表格，支持追问换图表 |
| **描述性问答** | 「这个库存了什么？」类问题，不执行 SQL，基于业务语义回答 |
| **多轮上下文** | 最近 5 轮历史；支持「画折线图」等短追问复用上一轮 SQL |

### 数据与安全

| 模块 | 说明 |
|------|------|
| **多数据源** | 动态配置 MySQL / PostgreSQL / Oracle 连接 |
| **表白名单** | 控制 AI 可查询的表范围 |
| **字段语义层** | 为字段配置中文业务名与口径说明，提升 SQL 准确率 |
| **SQL 安全** | Druid 校验，仅允许 `SELECT`，表白名单拦截 |
| **敏感信息加密** | 数据源密码、LLM Key AES-GCM 加密存储（`ENC:v1:`） |
| **API Key 鉴权** | 可选 HTTP Header `X-API-Key` |

### 体验与工程

| 模块 | 说明 |
|------|------|
| **常用问题模板** | 欢迎页快捷问题，支持按数据源配置 |
| **SQL 编辑再执行** | 用户可修改 SQL 后重新跑查询 |
| **结果导出** | SQL 复制、CSV 导出、图表 PNG |
| **Redis 查询缓存** | 相同问题 TTL 内命中缓存；Redis 不可用时降级内存 |
| **SQL 准确率增强** | 表间关系推断、相对时间解析、执行失败自动修复 1 次 |

### 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17、Spring Boot 3.2、MyBatis-Plus、Druid SQL Parser |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、ECharts |
| 数据库 | MySQL 8（系统表 + 演示业务表） |
| 缓存 | Redis 6+（可选） |
| 大模型 | DeepSeek API（OpenAI 兼容接口，可替换） |

---

## 项目结构

```
ChatBI/
├── README.md                      # 本文件
├── .env.example                   # 环境变量模板
├── .gitignore
├── backend/                       # Spring Boot 后端
│   ├── src/main/java/com/chatbi/
│   │   ├── controller/            # REST API
│   │   ├── service/               # 业务逻辑
│   │   ├── service/cache/         # Redis / 内存缓存
│   │   ├── config/                # 配置与迁移
│   │   ├── util/                  # Prompt、SQL 校验、时间解析等
│   │   └── entity/ / mapper/      # 持久层
│   ├── src/main/resources/
│   │   ├── application.yml        # 主配置（无密钥）
│   │   ├── application-local.yml.example
│   │   ├── db/schema.sql          # 全量 DDL + 演示数据
│   │   └── prompt/                # Few-shot 示例
│   └── pom.xml
├── frontend/                      # Vue 3 前端
│   ├── src/views/ChatBI.vue       # 主界面
│   ├── src/components/            # 图表、侧栏、对话框等
│   ├── src/api/                   # HTTP / SSE 封装
│   └── .env.example
├── docs/
│   ├── design/ChatBI开发文档.md   # 原始设计文档（参考）
│   └── STRUCTURE-CHANGELOG.md     # 相对设计文档的变更记录
├── scripts/
    ├── db/                        # migrate-v5.sql、migrate-v6.sql 等
    └── test-db/                     # 压测库脚本（可选）
```

---

## 环境要求

| 组件 | 版本 | 必需 |
|------|------|------|
| JDK | 17+ | 是 |
| Maven | 3.8+ | 是 |
| Node.js | 18+ | 是 |
| MySQL | 8.0+ | 是 |
| Redis | 6+ | 否（推荐，用于分布式缓存） |
| DeepSeek / 兼容 LLM API Key | — | 是 |

---

## 快速开始

### 1. 克隆仓库

```bash
git clone <your-repo-url>
cd ChatBI
```

### 2. 初始化数据库

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS chatbi DEFAULT CHARSET utf8mb4;"
mysql -u root -p chatbi < backend/src/main/resources/db/schema.sql
```

已有旧库可额外执行增量脚本（按版本顺序）：

```bash
mysql -u root -p chatbi < scripts/db/migrate-v5.sql
mysql -u root -p chatbi < scripts/db/migrate-v6.sql
```

> 新安装直接执行 `schema.sql` 即可；后端启动时还会自动执行部分轻量迁移（`DatabaseMigrationRunner`）。

### 3. 配置密钥

**方式 A：本地 YAML**

```bash
cp backend/src/main/resources/application-local.yml.example \
   backend/src/main/resources/application-local.yml
# 编辑 application-local.yml，填入 MySQL 密码与 DeepSeek API Key
```

**方式 B：环境变量**

```bash
# PowerShell
$env:MYSQL_PASSWORD="your-mysql-password"
$env:CHATBI_LLM_API_KEY="your-deepseek-api-key"

# Bash
export MYSQL_PASSWORD=your-mysql-password
export CHATBI_LLM_API_KEY=your-deepseek-api-key
```

完整变量列表见 [`.env.example`](.env.example)。

### 4. 启动 Redis（可选）

```bash
# Docker 示例
docker run -d --name chatbi-redis -p 6379:6379 redis:7-alpine
```

未启动 Redis 时，查询缓存自动降级为进程内内存，不影响主流程。

### 5. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认地址：http://localhost:8080

### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器访问：http://localhost:5173

### 7. 首次使用

1. 点击右上角 **设置**，配置 LLM（若未通过 YAML / 环境变量配置）
2. 右侧 **数据源** 面板可添加外部数据库，或使用 **默认数据库**（`application.yml` 中的 MySQL）
3. 在输入框用中文提问，例如：「总共有多少个商品？」

---

## 配置说明

### 后端环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `MYSQL_HOST` | MySQL 主机 | `localhost` |
| `MYSQL_PORT` | MySQL 端口 | `3306` |
| `MYSQL_DATABASE` | 系统库名 | `chatbi` |
| `MYSQL_USERNAME` | MySQL 用户名 | `root` |
| `MYSQL_PASSWORD` | MySQL 密码 | 空 |
| `REDIS_HOST` | Redis 主机 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `CHATBI_LLM_API_KEY` | 大模型 API Key | 空（必须配置） |
| `CHATBI_API_KEY` | HTTP 接口鉴权 Key | 空（不启用） |
| `CHATBI_ENCRYPTION_KEY` | AES 加密密钥 | 空（开发可用内置占位） |

### 后端 `application.yml` 关键项

| 配置项 | 说明 |
|--------|------|
| `chatbi.cache.redis-enabled` | `true` 使用 Redis+内存降级；`false` 纯内存 |
| `chatbi.security.query-cache-ttl-minutes` | 查询缓存 TTL（分钟），`0` 关闭 |
| `chatbi.llm.model` | 模型名，默认 `deepseek-chat` |
| `chatbi.llm.base-url` | API 地址，默认 DeepSeek |
| `chatbi.cors.allowed-origins` | 前端跨域来源 |

### 前端环境变量

| 变量 | 说明 |
|------|------|
| `VITE_CHATBI_API_KEY` | 与后端 `CHATBI_API_KEY` 一致；留空不鉴权 |

复制 `frontend/.env.example` 为 `frontend/.env.local` 后填写。

---

## 数据库初始化

### 系统表

| 表名 | 用途 |
|------|------|
| `chat_session` | 对话会话 |
| `chat_query_log` | 查询日志 |
| `data_source` | 外部数据源配置 |
| `table_whitelist` | 表白名单 |
| `column_semantic` | 字段语义 |
| `query_template` | 常用问题模板 |
| `system_config` | 系统配置 |

### 演示业务表

`products`、`users`、`orders` — 含种子数据，可直接体验 NL2SQL。

---

## API 概览

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chatbi/query` | 同步查询 |
| POST | `/api/chatbi/query/stream` | SSE 流式查询 |
| POST | `/api/chatbi/execute-sql` | 手动 SQL 执行 |
| GET | `/api/chatbi/schema` | Schema 摘要 |
| GET | `/api/chatbi/sessions` | 会话列表 |
| GET | `/api/chatbi/sessions/{id}/messages` | 会话消息 |
| GET/POST/PUT/DELETE | `/api/chatbi/datasources/**` | 数据源 CRUD |
| GET/PUT | `/api/chatbi/datasources/{id}/whitelist` | 表白名单 |
| GET/PUT | `/api/chatbi/datasources/{id}/semantics/**` | 字段语义 |
| GET/POST/DELETE | `/api/chatbi/templates/**` | 常用问题模板 |

启用 API Key 时，请求头需携带：`X-API-Key: <your-key>`

---

## 安全说明

### 运行时安全

- 仅允许 `SELECT` 语句，写操作会被 Druid 拦截
- 表白名单限制可查询表
- 数据源密码与 LLM Key 加密落库
- 可选 HTTP API Key 鉴权

### 生成加密密钥示例

```bash
# 生成 32 字节 AES 密钥（Base64）
openssl rand -base64 32
```

将输出填入 `CHATBI_ENCRYPTION_KEY`。

---

## 开发与测试

### 后端单元测试

```bash
cd backend
mvn test
```

### 前端构建

```bash
cd frontend
npm run build
# 产物在 frontend/dist/
```

### 生产构建后端 JAR

```bash
cd backend
mvn -DskipTests package
java -jar target/chatbi-backend-*.jar
```

---

## 常见问题

**Q：启动报错 `Connection refused` 连接 Redis？**  
A：启动 Redis，或设置 `chatbi.cache.redis-enabled: false` 使用纯内存缓存。

**Q：查询返回「LLM API Key 未配置」？**  
A：配置 `CHATBI_LLM_API_KEY` 或 `application-local.yml` 中的 `chatbi.llm.api-key`。

**Q：8080 端口被占用？**  
A：Windows：`netstat -ano | findstr :8080` 找到 PID 后 `taskkill /PID <pid> /F`。

**Q：多轮追问图表不生效？**  
A：确保上一轮是数据查询（有 SQL），短句如「画折线图」会复用上一轮 SQL。

**Q：如何切换其他大模型？**  
A：修改 `chatbi.llm.base-url` 与 `model` 为 OpenAI 兼容接口即可。

