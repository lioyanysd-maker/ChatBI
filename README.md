# ChatBI

**自然语言数据分析平台（NL2SQL）** — 用中文提问，自动生成 SQL、查询数据库、渲染图表，并给出基于真实数据的 AI 解释。

> 本目录为 **GitHub 发布版**：已排除开发密钥、构建产物与 IDE 配置。使用前请复制 `.env.example` / `application-local.yml.example` 并填入自己的配置。

---

## 📑 快速导航

- [🎯 项目亮点](#项目亮点)
- [✨ 核心功能](#核心功能)
- [🚀 快速开始](#快速开始)
- [📋 项目结构](#项目结构)
- [⚙️ 配置指南](#配置指南)
- [🔌 API 概览](#api-概览)
- [🔒 安全说明](#安全说明)
- [❓ 常见问题](#常见问题)

---

## 🎯 项目亮点

### 自然语言转 SQL
<img width="1865" height="920" alt="自然语言转SQL" src="https://github.com/user-attachments/assets/f3351a5e-d5cb-4712-b17f-f022fb0fd383" />
<img width="1865" height="920" alt="查询结果展示" src="https://github.com/user-attachments/assets/43120b7e-1e40-4f49-a301-48f37c503fca" />

### 大模型与数据库配置
<img width="1860" height="915" alt="配置界面" src="https://github.com/user-attachments/assets/e9f49db8-83b9-4087-9938-4df7fff0bf3a" />

### 多轮对话与上下文理解
<img width="1802" height="920" alt="上下文理解" src="https://github.com/user-attachments/assets/98f114be-94f3-4df7-acc6-1e29355f50d0" />

---

## ✨ 核心功能

### 数据查询与分析

| 功能 | 说明 |
|------|------|
| **自然语言转 SQL** | 基于 Schema + Few-shot + 多轮上下文生成 `SELECT` 语句 |
| **流式对话** | SSE 推送步骤、SQL、图表、解释文本（`/api/chatbi/query/stream`） |
| **智能图表** | 自动识别柱状图/折线图/饼图/表格，支持追问换图表 |
| **描述性问答** | 「这个库存了什么？」类问题，无需 SQL，基于业务语义回答 |
| **多轮上下文** | 最近 5 轮历史，支持「画折线图」等短追问复用上一轮 SQL |

### 数据安全与管理

| 功能 | 说明 |
|------|------|
| **多数据源支持** | 动态配置 MySQL/PostgreSQL/Oracle 连接 |
| **表白名单控制** | 限制 AI 可查询的表范围 |
| **字段语义层** | 为字段配置中文业务名与口径说明，提升 SQL 准确率 |
| **SQL 执行安全** | Druid 校验，仅允许 `SELECT`，表白名单拦截 |
| **敏感信息加密** | 数据源密码、LLM Key 用 AES-GCM 加密存储（`ENC:v1:`） |
| **API 鉴权** | 可选 HTTP Header `X-API-Key` 身份验证 |

### 用户体验与工程化

| 功能 | 说明 |
|------|------|
| **常用问题模板** | 欢迎页快捷问题，支持按数据源配置 |
| **SQL 编辑再执行** | 用户可修改 SQL 后重新跑查询 |
| **结果导出** | SQL 复制、CSV 导出、图表 PNG 下载 |
| **查询缓存** | Redis 缓存相同问题；Redis 不可用时自动降级内存 |
| **SQL 准确率增强** | 表间关系推断、相对时间解析、执行失败自动修复 |

### 技术栈

| 层级 | 技术选型 |
|------|---------|
| **后端** | Java 17、Spring Boot 3.2、MyBatis-Plus、Druid SQL Parser |
| **前端** | Vue 3、TypeScript、Vite、Element Plus、ECharts |
| **数据库** | MySQL 8（系统表 + 演示业务表） |
| **缓存** | Redis 6+（可选，用于分布式缓存） |
| **大模型** | DeepSeek API（OpenAI 兼容接口，可替换） |

---

## 🚀 快速开始

### 1️⃣ 环境检查

确保已安装以下依赖：

| 组件 | 版本 | 备注 |
|------|------|------|
| JDK | 17+ | 必需 |
| Maven | 3.8+ | 必需 |
| Node.js | 18+ | 必需 |
| MySQL | 8.0+ | 必需 |
| Redis | 6+ | 推荐（查询缓存） |
| DeepSeek API Key | — | 必需 |

### 2️⃣ 克隆仓库

```bash
git clone <your-repo-url>
cd ChatBI
```

### 3️⃣ 初始化数据库

**创建数据库：**

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS chatbi DEFAULT CHARSET utf8mb4;"
```

**导入 DDL 与演示数据：**

```bash
mysql -u root -p chatbi < backend/src/main/resources/db/schema.sql
```

**升级旧库（可选）：**

若已有旧版本，按版本顺序执行增量迁移：

```bash
mysql -u root -p chatbi < scripts/db/migrate-v5.sql
mysql -u root -p chatbi < scripts/db/migrate-v6.sql
```

> ℹ️ 新安装直接执行 `schema.sql` 即可；后端启动时还会自动执行轻量迁移（`DatabaseMigrationRunner`）。

### 4️⃣ 配置密钥

**方式 A：本地 YAML（推荐开发环境）**

```bash
cp backend/src/main/resources/application-local.yml.example \
   backend/src/main/resources/application-local.yml
```

编辑 `application-local.yml`，填入 MySQL 密码与 DeepSeek API Key：

```yaml
spring:
  datasource:
    password: your-mysql-password

chatbi:
  llm:
    api-key: your-deepseek-api-key
```

**方式 B：环境变量（推荐生产环境）**

```bash
# PowerShell
$env:MYSQL_PASSWORD="your-mysql-password"
$env:CHATBI_LLM_API_KEY="your-deepseek-api-key"

# Bash / Linux
export MYSQL_PASSWORD=your-mysql-password
export CHATBI_LLM_API_KEY=your-deepseek-api-key
```

完整环境变量列表见 [`.env.example`](.env.example)。

### 5️⃣ 启动 Redis（可选）

```bash
# Docker 启动 Redis
docker run -d --name chatbi-redis -p 6379:6379 redis:7-alpine
```

> 💡 不启动 Redis 时，查询缓存自动降级为进程内内存，不影响主流程。

### 6️⃣ 启动后端服务

```bash
cd backend
mvn spring-boot:run
```

✅ 默认地址：**http://localhost:8080**

### 7️⃣ 启动前端应用

```bash
cd frontend
npm install
npm run dev
```

✅ 浏览器访问：**http://localhost:5173**

### 8️⃣ 首次使用

1. 点击右上角 **⚙️ 设置**，配置 LLM（若未通过 YAML/环境变量配置）
2. 右侧 **📊 数据源** 面板可添加外部数据库，或使用 **默认数据库**（MySQL）
3. 在输入框用中文提问，例如：

   > "总共有多少个商品？"  
   > "最近7天的销售趋势是什么？"  
   > "库存预警商品有哪些？"

---

## 📋 项目结构

```
ChatBI/
├── README.md                      # 项目文档（本文件）
├── .env.example                   # 环境变量模板
├── .gitignore
│
├── backend/                       # Spring Boot 后端服务
│   ├── src/main/java/com/chatbi/
│   │   ├── controller/            # REST API 端点
│   │   ├── service/               # 业务逻辑服务
│   │   ├── service/cache/         # Redis/内存缓存管理
│   │   ├── config/                # Spring 配置与数据库迁移
│   │   ├── util/                  # 工具类（Prompt、SQL 校验、时间解析等）
│   │   ├── entity/                # JPA 实体类
│   │   └── mapper/                # MyBatis 数据访问层
│   ├── src/main/resources/
│   │   ├── application.yml        # 主配置文件（无密钥）
│   │   ├── application-local.yml.example  # 本地开发配置模板
│   │   ├── db/schema.sql          # 完整 DDL + 演示数据
│   │   └── prompt/                # Few-shot 示例
│   └── pom.xml                    # Maven 依赖
│
├── frontend/                      # Vue 3 前端应用
│   ├── src/
│   │   ├── views/ChatBI.vue       # 主页面组件
│   │   ├── components/            # 图表、侧栏、对话框等 UI 组件
│   │   ├── api/chatbi.ts          # HTTP/SSE 接口封装
│   │   ├── types/chatbi.ts        # TypeScript 类型定义
│   │   └── router/index.ts        # 路由配置
│   ├── .env.example               # 前端环境变量模板
│   └── vite.config.ts             # Vite 构建配置
│
├── docs/
│   ├── design/ChatBI开发文档.md   # 原始设计文档（参考）
│   └── STRUCTURE-CHANGELOG.md     # 相对设计文档的变更记录
│
└── scripts/
    ├── db/
    │   ├── migrate-v5.sql         # v5 版本增量迁移脚本
    │   ├── migrate-v6.sql         # v6 版本增量迁移脚本
    │   └── ...
    └── test-db/                   # 压测库脚本（可选）
```

---

## ⚙️ 配置指南

### 后端环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `MYSQL_HOST` | MySQL 主机地址 | `localhost` |
| `MYSQL_PORT` | MySQL 端口 | `3306` |
| `MYSQL_DATABASE` | 系统库名 | `chatbi` |
| `MYSQL_USERNAME` | MySQL 用户名 | `root` |
| `MYSQL_PASSWORD` | MySQL 密码 | 空（需配置） |
| `REDIS_HOST` | Redis 主机地址 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `CHATBI_LLM_API_KEY` | 大模型 API Key | 空（必须配置） |
| `CHATBI_API_KEY` | HTTP 接口鉴权 Key | 空（不启用） |
| `CHATBI_ENCRYPTION_KEY` | AES 加密密钥 | 空（开发可用内置占位） |

### 后端配置文件关键项

编辑 `backend/src/main/resources/application.yml` 或 `application-local.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chatbi
    username: root
    password: ${MYSQL_PASSWORD}
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}

chatbi:
  cache:
    redis-enabled: true              # 使用 Redis；false 则纯内存缓存
  security:
    query-cache-ttl-minutes: 30      # 查询缓存 TTL，单位分钟；0 关闭
  llm:
    api-key: ${CHATBI_LLM_API_KEY}
    model: deepseek-chat             # 模型名称
    base-url: https://api.deepseek.com/v1  # API 地址
  cors:
    allowed-origins: "http://localhost:5173"  # 前端跨域来源
```

### 前端环境变量

创建 `frontend/.env.local`：

```env
# 后端 API 地址（开发环境通过 Vite 代理）
VITE_CHATBI_API_KEY=          # 与后端 CHATBI_API_KEY 一致；留空不鉴权
```

---

## 🗄️ 数据库初始化

### 系统表说明

| 表名 | 用途 |
|------|------|
| `chat_session` | 对话会话记录 |
| `chat_query_log` | 查询历史日志 |
| `data_source` | 外部数据源配置 |
| `table_whitelist` | 表访问白名单 |
| `column_semantic` | 字段语义映射（中文业务名）|
| `query_template` | 常用问题模板 |
| `system_config` | 系统全局配置 |

### 演示业务表

系统自带三张演示表（含种子数据）：

- **`products`** — 商品信息表
- **`users`** — 用户信息表
- **`orders`** — 订单表

可直接用中文提问进行 NL2SQL 体验。

---

## 🔌 API 概览

### 查询与执行

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chatbi/query` | 同步查询（一次性响应） |
| POST | `/api/chatbi/query/stream` | SSE 流式查询（推送步骤、SQL、图表、解释） |
| POST | `/api/chatbi/execute-sql` | 手动 SQL 执行 |
| GET | `/api/chatbi/schema` | Schema 摘要（表、字段列表） |

### 会话管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/chatbi/sessions` | 会话列表 |
| GET | `/api/chatbi/sessions/{id}/messages` | 单个会话的消息历史 |

### 数据源管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST | `/api/chatbi/datasources` | 数据源列表与创建 |
| GET/PUT/DELETE | `/api/chatbi/datasources/{id}` | 数据源详情、更新、删除 |
| GET/PUT | `/api/chatbi/datasources/{id}/whitelist` | 表白名单 |
| GET/PUT | `/api/chatbi/datasources/{id}/semantics/**` | 字段语义 CRUD |

### 常用问题模板

| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST | `/api/chatbi/templates` | 模板列表与创建 |
| GET/PUT/DELETE | `/api/chatbi/templates/{id}` | 模板详情、更新、删除 |

### 身份验证

启用 API Key 时，所有请求需要在 HTTP Header 中携带：

```http
X-API-Key: <your-api-key>
```

---

## 🔒 安全说明

### 查询执行安全

✅ **仅允许 SELECT 语句** — Druid SQL Parser 拦截写操作（INSERT/UPDATE/DELETE）  
✅ **表白名单限制** — 控制 AI 可查询的表范围  
✅ **字段级访问控制** — 可配置字段是否可被 AI 查询

### 敏感信息保护

✅ **密码加密存储** — 数据源密码用 AES-GCM 加密（`ENC:v1:` 前缀）  
✅ **LLM Key 加密** — API Key 加密存储，不落地明文  
✅ **日志脱敏** — 查询日志中敏感字段已脱敏

### 生成加密密钥

```bash
# 生成 32 字节 AES-256 密钥（Base64 编码）
openssl rand -base64 32
```

输出示例：
```
aBcD1e2fGhIjKlMnOpQrStUvWxYzAbCdEfGhIjKl=
```

将其填入环境变量：
```bash
export CHATBI_ENCRYPTION_KEY="aBcD1e2fGhIjKlMnOpQrStUvWxYzAbCdEfGhIjKl="
```

---

## 🛠️ 开发与测试

### 后端单元测试

```bash
cd backend
mvn test
```

### 前端构建与测试

```bash
cd frontend
npm install
npm run dev          # 开发模式启动
npm run build        # 生产构建，产物在 frontend/dist/
npm run preview      # 预览生产构建
```

### 生产环境后端打包

```bash
cd backend
mvn clean package -DskipTests
java -jar target/chatbi-backend-*.jar
```

---

## ❓ 常见问题

### 启动与连接

**Q：启动报错 `Connection refused` 连接 Redis？**

A：两种解决方案：
- 启动 Redis：`docker run -d --name chatbi-redis -p 6379:6379 redis:7-alpine`
- 或禁用 Redis 缓存：设置 `chatbi.cache.redis-enabled: false` 使用纯内存缓存

**Q：8080 端口被占用？**

A：
- **Windows**：`netstat -ano | findstr :8080` 找到 PID 后 `taskkill /PID <pid> /F`
- **Linux/Mac**：`lsof -i :8080` 找到进程后 `kill -9 <PID>`

**Q：前端无法连接后端？**

A：检查以下项：
- 后端是否正在运行（http://localhost:8080）
- 跨域配置是否正确（`chatbi.cors.allowed-origins`）
- 前端 Vite 代理是否配置（通常自动）

### LLM 与查询

**Q：查询返回「LLM API Key 未配置」？**

A：配置 `CHATBI_LLM_API_KEY` 环境变量或 `application-local.yml` 中的 `chatbi.llm.api-key`。

**Q：如何切换其他大模型（不用 DeepSeek）？**

A：修改后端配置：

```yaml
chatbi:
  llm:
    base-url: https://api.openai.com/v1  # 改为 OpenAI 或其他兼容接口
    model: gpt-4                         # 改为对应模型名
    api-key: ${CHATBI_LLM_API_KEY}
```

**Q：生成的 SQL 不准确？**

A：尝试：
1. 配置**字段语义**：右侧数据源面板 → 字段管理，添加中文业务名与口径说明
2. 增加**Few-shot 示例**：编辑 `backend/src/main/resources/prompt/` 中的示例
3. 检查**表白名单**：确保必要的表已加入白名单

### 多轮对话与图表

**Q：多轮追问图表不生效？**

A：确保上一轮是**数据查询**（返回了 SQL），短句如「画折线图」或「用柱状图显示」才会复用上一轮 SQL 和数据。

**Q：为什么不能执行的是什么场景？**

A：描述性问答（「这个库存了什么？」）不执行 SQL，直接基于 Schema 回答，故无法切换图表。

### 数据缓存

**Q：如何清空查询缓存？**

A：
- **Redis 缓存**：`redis-cli FLUSHDB` 或在 Redis 管理工具中删除
- **内存缓存**：重启后端应用

**Q：缓存 TTL 如何设置？**

A：编辑配置：

```yaml
chatbi:
  security:
    query-cache-ttl-minutes: 30  # 30 分钟；0 表示不缓存
```

---

## 📞 获取帮助

- 📖 **设计文档**：见 `docs/design/ChatBI开发文档.md`
- 📝 **更新记录**：见 `docs/STRUCTURE-CHANGELOG.md`
- 🐛 **提交 Issue**：GitHub Issues
- 💬 **讨论建议**：GitHub Discussions

---

**祝您使用愉快！** 🎉
