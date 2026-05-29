#!/usr/bin/env python3
"""
生成 chatbi_bench 大量事实数据，输出 03-seed-facts.sql
仅依赖 Python 标准库。用法: python generate_facts.py [--scale small|medium|large]
"""

from __future__ import annotations

import argparse
import random
import sys
from datetime import datetime, timedelta
from pathlib import Path

SEED = 42
OUTPUT = Path(__file__).with_name("03-seed-facts.sql")

SCALES = {
    "small": {"customers": 500, "products": 300, "orders": 2000, "page_views": 8000},
    "medium": {
        "customers": 5000,
        "products": 2000,
        "orders": 25000,
        "page_views": 80000,
    },
    "large": {
        "customers": 20000,
        "products": 8000,
        "orders": 100000,
        "page_views": 500000,
    },
}

SURNAMES = list("赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨朱秦尤许何吕施张孔曹严华金魏陶姜")
GIVEN = ["伟", "芳", "娜", "敏", "静", "丽", "强", "磊", "洋", "勇", "艳", "杰", "涛", "明", "超", "秀英", "华", "慧", "鹏", "飞"]
SPECS = ["标准版", "升级版", "Pro", "Max", "Lite", "青春版", "旗舰版", "经典款", "2024款", "2025款"]
PAGE_TYPES = ["home", "product", "category", "cart", "checkout", "search"]
PAY_CHANNELS = ["alipay", "wechat", "card", "unionpay"]
REFUND_REASONS = ["质量问题", "发错货", "不喜欢", "七天无理由", "物流损坏", "描述不符"]


def rand_dt(start: datetime, end: datetime) -> datetime:
    span = int((end - start).total_seconds())
    return start + timedelta(seconds=random.randint(0, max(span, 1)))


def sql_str(v: str | None) -> str:
    if v is None:
        return "NULL"
    return "'" + v.replace("\\", "\\\\").replace("'", "''") + "'"


def sql_num(v) -> str:
    if v is None:
        return "NULL"
    return str(v)


def write_insert(f, table: str, columns: list[str], rows: list[tuple], batch: int = 400) -> None:
    if not rows:
        return
    cols = ", ".join(columns)
    for i in range(0, len(rows), batch):
        chunk = rows[i : i + batch]
        f.write(f"INSERT INTO {table} ({cols}) VALUES\n")
        lines = []
        for row in chunk:
            vals = ", ".join(
                sql_str(x) if isinstance(x, str) else sql_num(x) for x in row
            )
            lines.append(f"({vals})")
        f.write(",\n".join(lines))
        f.write(";\n")


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate chatbi_bench fact data SQL")
    parser.add_argument(
        "--scale", choices=SCALES.keys(), default="medium", help="数据规模"
    )
    parser.add_argument("-o", "--output", type=Path, default=OUTPUT)
    args = parser.parse_args()
    cfg = SCALES[args.scale]

    random.seed(SEED)
    now = datetime(2026, 5, 29, 12, 0, 0)
    start = datetime(2023, 1, 1)

    num_customers = cfg["customers"]
    num_products = cfg["products"]
    num_orders = cfg["orders"]
    num_page_views = cfg["page_views"]

    city_ids = list(range(1, 36))
    level_ids = [1, 2, 3, 4]
    tag_ids = list(range(1, 21))
    category_ids = [20, 21, 22, 23, 24, 25, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19]
    brand_ids = list(range(1, 51))
    supplier_ids = list(range(1, 21))
    warehouse_ids = list(range(1, 9))
    voucher_ids = [1, 2, 3, 4, 8]
    channel_ids = list(range(1, 9))
    campaign_ids = [5, 6, 7, 8]

    print(f"Generating scale={args.scale} -> {args.output}")

    with args.output.open("w", encoding="utf-8") as f:
        f.write("-- 由 generate_facts.py 自动生成，请勿手工编辑\n")
        f.write("USE chatbi_bench;\n")
        f.write("SET FOREIGN_KEY_CHECKS = 0;\n\n")

        # ---- customers ----
        customers = []
        for i in range(1, num_customers + 1):
            name = random.choice(SURNAMES) + random.choice(GIVEN) + (random.choice(GIVEN) if random.random() < 0.3 else "")
            reg = rand_dt(start, now - timedelta(days=30))
            customers.append(
                (
                    f"C{i:06d}",
                    name,
                    random.choice([0, 1, 2]),
                    f"1{random.randint(3,9)}{random.randint(100000000,999999999)}",
                    f"user{i}@example.com",
                    random.choice(city_ids),
                    random.choices(level_ids, weights=[50, 25, 18, 7])[0],
                    random.randint(0, 50000),
                    reg.strftime("%Y-%m-%d %H:%M:%S"),
                    rand_dt(reg, now).strftime("%Y-%m-%d %H:%M:%S") if random.random() < 0.85 else None,
                    1 if random.random() < 0.97 else 0,
                )
            )
        write_insert(
            f,
            "tb_customer",
            [
                "customer_no",
                "customer_name",
                "gender",
                "phone",
                "email",
                "city_id",
                "level_id",
                "points",
                "register_time",
                "last_login",
                "status",
            ],
            customers,
        )
        print(f"  customers: {num_customers}")

        # ---- customer tags ----
        ct_rows = []
        for cid in range(1, num_customers + 1):
            for tid in random.sample(tag_ids, k=random.randint(1, 4)):
                ct_rows.append((cid, tid))
        write_insert(f, "tb_customer_tag", ["customer_id", "tag_id"], ct_rows)
        print(f"  customer_tags: {len(ct_rows)}")

        # ---- products & skus ----
        products = []
        skus: list[tuple] = []
        sku_id = 0
        product_prices: dict[int, float] = {}
        for i in range(1, num_products + 1):
            cat = random.choice(category_ids)
            price = round(random.uniform(29, 8999), 2)
            cost = round(price * random.uniform(0.35, 0.75), 2)
            products.append(
                (
                    f"P{i:06d}",
                    f"测试商品{i:05d}-{random.choice(['旗舰','经典','热销','新品'])}",
                    cat,
                    random.choice(brand_ids),
                    random.choice(supplier_ids),
                    price,
                    cost,
                    1 if random.random() < 0.92 else 0,
                )
            )
            product_prices[i] = price
            sku_count = random.choices([1, 2, 3], weights=[55, 35, 10])[0]
            for s in range(sku_count):
                sku_id += 1
                spec = random.choice(SPECS)
                skus.append(
                    (
                        i,
                        f"SKU{i:06d}-{s+1}",
                        spec,
                        round(price * random.uniform(0.95, 1.08), 2),
                        random.randint(100, 5000),
                    )
                )
        write_insert(
            f,
            "tb_product",
            [
                "product_code",
                "product_name",
                "category_id",
                "brand_id",
                "supplier_id",
                "list_price",
                "cost_price",
                "status",
            ],
            products,
        )
        write_insert(
            f,
            "tb_product_sku",
            ["product_id", "sku_code", "spec_info", "sale_price", "weight_g"],
            skus,
        )
        total_skus = len(skus)
        print(f"  products: {num_products}, skus: {total_skus}")

        # ---- inventory ----
        inv_rows = []
        for sid in range(1, total_skus + 1):
            for wid in random.sample(warehouse_ids, k=random.randint(1, 3)):
                inv_rows.append((wid, sid, random.randint(0, 800)))
        write_insert(f, "tb_inventory", ["warehouse_id", "sku_id", "quantity"], inv_rows)
        print(f"  inventory: {len(inv_rows)}")

        # ---- price log ----
        pl_rows = []
        for _ in range(min(num_products * 2, 6000)):
            pid = random.randint(1, num_products)
            old = product_prices[pid]
            new = round(old * random.uniform(0.8, 1.2), 2)
            pl_rows.append(
                (
                    pid,
                    old,
                    new,
                    rand_dt(start, now).strftime("%Y-%m-%d %H:%M:%S"),
                    random.choice(["system", "admin", "promo"]),
                )
            )
        write_insert(
            f, "tb_price_log", ["product_id", "old_price", "new_price", "changed_at", "changed_by"], pl_rows
        )

        # ---- stock movement ----
        sm_rows = []
        for _ in range(min(total_skus * 3, 15000)):
            sm_rows.append(
                (
                    random.choice(warehouse_ids),
                    random.randint(1, total_skus),
                    random.choice([1, 1, 1, 2, 2, 3, 4]),
                    random.randint(1, 200),
                    f"REF{random.randint(100000,999999)}",
                    rand_dt(start, now).strftime("%Y-%m-%d %H:%M:%S"),
                )
            )
        write_insert(
            f,
            "tb_stock_movement",
            ["warehouse_id", "sku_id", "movement_type", "quantity", "ref_no", "created_at"],
            sm_rows,
        )

        # ---- orders ----
        orders = []
        order_meta: list[dict] = []
        for i in range(1, num_orders + 1):
            cid = random.randint(1, num_customers)
            status = random.choices([1, 2, 3, 4, 5], weights=[5, 15, 20, 55, 5])[0]
            create = rand_dt(start, now)
            voucher = random.choice([None, None, None] + voucher_ids) if random.random() < 0.25 else None
            pay_val = round(random.uniform(50, 8000), 2)
            disc = round(pay_val * random.uniform(0, 0.15), 2) if voucher else 0
            city = random.choice(city_ids)
            pay_time = ship_time = complete_time = None
            if status >= 2:
                pay_time = create + timedelta(minutes=random.randint(1, 120))
            if status >= 3:
                ship_time = pay_time + timedelta(hours=random.randint(4, 72)) if pay_time else None
            if status == 4:
                complete_time = ship_time + timedelta(days=random.randint(1, 7)) if ship_time else None
            orders.append(
                (
                    f"ORD{i:08d}",
                    cid,
                    voucher,
                    status,
                    pay_val,
                    disc,
                    round(random.uniform(0, 20), 2),
                    city,
                    create.strftime("%Y-%m-%d %H:%M:%S"),
                    pay_time.strftime("%Y-%m-%d %H:%M:%S") if pay_time else None,
                    ship_time.strftime("%Y-%m-%d %H:%M:%S") if ship_time else None,
                    complete_time.strftime("%Y-%m-%d %H:%M:%S") if complete_time else None,
                )
            )
            order_meta.append({"status": status, "pay_val": pay_val, "create": create, "cid": cid})
        write_insert(
            f,
            "tb_voucher_order",
            [
                "order_no",
                "customer_id",
                "voucher_id",
                "order_status",
                "pay_value",
                "discount_value",
                "shipping_fee",
                "city_id",
                "create_time",
                "pay_time",
                "ship_time",
                "complete_time",
            ],
            orders,
        )
        print(f"  orders: {num_orders}")

        # ---- order items ----
        oi_rows = []
        sku_prices = {idx + 1: skus[idx][3] for idx in range(total_skus)}
        sku_products = {idx + 1: skus[idx][0] for idx in range(total_skus)}
        for oid in range(1, num_orders + 1):
            for _ in range(random.choices([1, 2, 3, 4], weights=[40, 35, 20, 5])[0]):
                sid = random.randint(1, total_skus)
                qty = random.randint(1, 5)
                up = float(sku_prices[sid])
                oi_rows.append((oid, sid, sku_products[sid], qty, up, round(up * qty, 2)))
        write_insert(
            f,
            "tb_order_item",
            ["order_id", "sku_id", "product_id", "quantity", "unit_price", "line_amount"],
            oi_rows,
        )
        print(f"  order_items: {len(oi_rows)}")

        # ---- payments ----
        pay_rows = []
        pay_id = 0
        for oid, meta in enumerate(order_meta, start=1):
            if meta["status"] >= 2:
                pay_id += 1
                pay_rows.append(
                    (
                        oid,
                        f"PAY{pay_id:010d}",
                        random.choice(PAY_CHANNELS),
                        meta["pay_val"],
                        1,
                        (meta["create"] + timedelta(minutes=random.randint(1, 60))).strftime(
                            "%Y-%m-%d %H:%M:%S"
                        ),
                    )
                )
        write_insert(
            f,
            "tb_payment",
            ["order_id", "payment_no", "pay_channel", "pay_amount", "pay_status", "pay_time"],
            pay_rows,
        )
        print(f"  payments: {len(pay_rows)}")

        # ---- refunds ----
        refund_rows = []
        paid_orders = [i + 1 for i, m in enumerate(order_meta) if m["status"] >= 2]
        for rid, oid in enumerate(random.sample(paid_orders, k=min(len(paid_orders) // 20, 3000)), start=1):
            meta = order_meta[oid - 1]
            refund_rows.append(
                (
                    oid,
                    f"RF{rid:08d}",
                    round(meta["pay_val"] * random.uniform(0.3, 1.0), 2),
                    random.choice(REFUND_REASONS),
                    1,
                )
            )
        write_insert(
            f,
            "tb_refund",
            ["order_id", "refund_no", "refund_amount", "reason", "refund_status"],
            refund_rows,
        )

        # ---- reviews ----
        rv_rows = []
        for _ in range(min(num_orders // 3, 12000)):
            oid = random.randint(1, num_orders)
            cid = order_meta[oid - 1]["cid"]
            pid = random.randint(1, num_products)
            rv_rows.append(
                (
                    pid,
                    cid,
                    oid if random.random() < 0.7 else None,
                    random.choices([5, 4, 3, 2, 1], weights=[45, 30, 15, 7, 3])[0],
                    random.choice(["很好", "不错", "一般", "满意", "性价比高", "物流快", ""]),
                    rand_dt(start, now).strftime("%Y-%m-%d %H:%M:%S"),
                )
            )
        write_insert(
            f,
            "tb_product_review",
            ["product_id", "customer_id", "order_id", "rating", "content", "created_at"],
            rv_rows,
        )
        print(f"  reviews: {len(rv_rows)}")

        # ---- campaign attribution ----
        ca_rows = []
        for oid in random.sample(range(1, num_orders + 1), k=min(num_orders // 4, 20000)):
            ca_rows.append(
                (
                    oid,
                    random.choice(campaign_ids),
                    random.choice(channel_ids),
                    rand_dt(start, now).strftime("%Y-%m-%d %H:%M:%S"),
                )
            )
        write_insert(
            f,
            "tb_campaign_attribution",
            ["order_id", "campaign_id", "channel_id", "touch_time"],
            ca_rows,
        )

        # ---- page views ----
        pv_rows = []
        for _ in range(num_page_views):
            cid = random.randint(1, num_customers) if random.random() < 0.7 else None
            ptype = random.choice(PAGE_TYPES)
            pid = random.randint(1, num_products) if ptype == "product" else None
            pv_rows.append(
                (
                    cid,
                    ptype,
                    pid,
                    random.choice(channel_ids) if random.random() < 0.6 else None,
                    f"sess{random.randint(1, num_customers * 2)}",
                    rand_dt(start, now).strftime("%Y-%m-%d %H:%M:%S"),
                )
            )
        write_insert(
            f,
            "tb_page_view",
            ["customer_id", "page_type", "product_id", "channel_id", "session_id", "view_time"],
            pv_rows,
            batch=500,
        )
        print(f"  page_views: {num_page_views}")

        # ---- shopping carts ----
        cart_rows = []
        cart_items = []
        cart_count = min(num_customers // 2, 8000)
        active_customers = random.sample(range(1, num_customers + 1), cart_count)
        for idx, cid in enumerate(active_customers, start=1):
            cart_rows.append((cid, 1 if random.random() < 0.3 else 0))
        write_insert(f, "tb_shopping_cart", ["customer_id", "status"], cart_rows)

        for cart_id in range(1, len(cart_rows) + 1):
            for _ in range(random.randint(1, 5)):
                cart_items.append((cart_id, random.randint(1, total_skus), random.randint(1, 3)))
        write_insert(f, "tb_cart_item", ["cart_id", "sku_id", "quantity"], cart_items)
        print(f"  carts: {len(cart_rows)}, cart_items: {len(cart_items)}")

        f.write("\nSET FOREIGN_KEY_CHECKS = 1;\n")
        f.write("-- 数据生成完成\n")

    size_mb = args.output.stat().st_size / 1024 / 1024
    print(f"Done: {args.output} ({size_mb:.1f} MB)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
