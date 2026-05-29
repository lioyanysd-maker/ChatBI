# 脚本

本地开发与运维辅助脚本（建库、种子数据、部署等）。

- `db/init.sql` — ChatBI 系统库初始化入口
- `db/migrate-*.sql` — 系统库增量迁移
- **`test-db/`** — **独立复杂测试库 `chatbi_bench`**（模拟用户自建库，见 [test-db/README.md](test-db/README.md)）

数据库 DDL 参考 `docs/design/ChatBI开发文档.md` 第五节；若表结构有改动，请同步更新 `docs/STRUCTURE-CHANGELOG.md`。
