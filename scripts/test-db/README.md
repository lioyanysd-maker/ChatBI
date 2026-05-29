# ChatBI 复杂测试数据库（chatbi_bench）

与 ChatBI **系统库 `chatbi` 完全分离**，用于模拟用户自建业务数据库，测试多表 JOIN、聚合、时间序列、树形分类等复杂 NL2SQL 场景。

## 数据规模（默认 medium）

| 指标 | small | medium（默认） | large |
|------|-------|----------------|-------|
| 客户 | 500 | 5,000 | 20,000 |
| 商品/SKU | 300 / ~600 | 2,000 / ~4,000 | 8,000 / ~16,000 |
| 订单 | 2,000 | 25,000 | 100,000 |
| 订单明细 | ~5,000 | ~60,000 | ~250,000 |
| 页面浏览 | 8,000 | 80,000 | 500,000 |

## 表结构（32 张业务表）

- **地理与客户**：区域、城市、会员等级、客户、标签、客户-标签（N:M）
- **商品供应链**：分类树（自关联）、品牌、供应商、商品、SKU、仓库、库存、库存流水、价格历史
- **交易**：优惠券、订单、订单明细、支付、退款、评价
- **组织营销**：部门树、员工、员工-区域（N:M）、营销渠道、活动、活动-渠道（N:M）、营销归因
- **行为**：页面浏览、购物车、购物车明细

表名统一 `tb_` 前缀，与真实用户库命名习惯一致。

## 一键安装

### Windows（PowerShell）

```powershell
cd scripts/test-db
.\setup.ps1
# 小规模快速验证
.\setup.ps1 -Scale small
```

### Linux / macOS

```bash
cd scripts/test-db
chmod +x setup.sh
./setup.sh
# 大规模
SCALE=large ./setup.sh
```

### 手动步骤

```bash
mysql -u root -p < 00-init.sql
mysql -u root -p chatbi_bench < 01-schema.sql
mysql -u root -p chatbi_bench < 02-seed-dimensions.sql
python generate_facts.py --scale medium
mysql -u root -p chatbi_bench < 03-seed-facts.sql
```

## 在 ChatBI 中连接

1. 打开右上角 **设置 → 数据库**
2. 添加数据源：

| 字段 | 值 |
|------|-----|
| 名称 | 复杂测试库 |
| 类型 | MySQL |
| 主机 | 127.0.0.1 |
| 端口 | 3306 |
| 数据库 | **chatbi_bench** |
| 用户名 | root（或 bench） |
| 密码 | 你的 MySQL 密码 |

3. 测试连接 → 保存
4. 在右侧数据源栏选中该库，即可提问

## 推荐测试问题

- 按月统计销售额折线图（`tb_voucher_order` + `pay_value`）
- 各城市客户数量排名
- 各品牌商品平均售价与销量
- 优惠券使用率最高的活动
- 各部门员工负责区域的订单总额
- 页面浏览量最高的商品 Top 10
- 分类树各层级商品数量（需 JOIN 分类自关联）

## 目录说明

```
scripts/test-db/
├── README.md              # 本文件
├── 00-init.sql            # 创建 chatbi_bench 库
├── 01-schema.sql          # 32 张表 DDL
├── 02-seed-dimensions.sql # 维度/主数据
├── generate_facts.py      # 大量事实数据生成器
├── 03-seed-facts.sql      # 生成产物（gitignore）
├── setup.ps1              # Windows 安装脚本
└── setup.sh               # Unix 安装脚本
```

## 注意事项

- **不要**把 `chatbi_bench` 当作 ChatBI 系统库；系统表仍在 `chatbi` 库
- 重新安装会先 TRUNCATE 再导入维度数据；事实数据由脚本重新生成
- `03-seed-facts.sql` 体积较大（medium 约 30~80MB），首次导入需数分钟
