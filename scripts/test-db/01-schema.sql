-- chatbi_bench 表结构：电商 + CRM + 营销分析
USE chatbi_bench;

SET FOREIGN_KEY_CHECKS = 0;

-- ========== 地理与客户 ==========

DROP TABLE IF EXISTS tb_region;
CREATE TABLE tb_region (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    region_code VARCHAR(16) NOT NULL UNIQUE COMMENT '区域编码',
    region_name VARCHAR(64) NOT NULL COMMENT '区域名称',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '销售区域';

DROP TABLE IF EXISTS tb_city;
CREATE TABLE tb_city (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    region_id   INT NOT NULL COMMENT '所属区域',
    city_name   VARCHAR(64) NOT NULL COMMENT '城市名称',
    tier        TINYINT DEFAULT 2 COMMENT '城市等级 1一线 2二线 3三线',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_region (region_id),
    CONSTRAINT fk_city_region FOREIGN KEY (region_id) REFERENCES tb_region(id)
) COMMENT '城市';

DROP TABLE IF EXISTS tb_customer_level;
CREATE TABLE tb_customer_level (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    level_code  VARCHAR(16) NOT NULL UNIQUE COMMENT '等级编码',
    level_name  VARCHAR(32) NOT NULL COMMENT '等级名称',
    min_points  INT DEFAULT 0 COMMENT '最低积分',
    discount_rate DECIMAL(4,2) DEFAULT 1.00 COMMENT '折扣系数'
) COMMENT '会员等级';

DROP TABLE IF EXISTS tb_customer;
CREATE TABLE tb_customer (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_no   VARCHAR(32) NOT NULL UNIQUE COMMENT '客户编号',
    customer_name VARCHAR(64) NOT NULL COMMENT '客户姓名',
    gender        TINYINT COMMENT '0未知 1男 2女',
    phone         VARCHAR(20) COMMENT '手机号',
    email         VARCHAR(128) COMMENT '邮箱',
    city_id       INT COMMENT '所在城市',
    level_id      INT COMMENT '会员等级',
    points        INT DEFAULT 0 COMMENT '积分',
    register_time DATETIME NOT NULL COMMENT '注册时间',
    last_login    DATETIME COMMENT '最后登录',
    status        TINYINT DEFAULT 1 COMMENT '1正常 0禁用',
    INDEX idx_city (city_id),
    INDEX idx_level (level_id),
    INDEX idx_register (register_time),
    CONSTRAINT fk_customer_city FOREIGN KEY (city_id) REFERENCES tb_city(id),
    CONSTRAINT fk_customer_level FOREIGN KEY (level_id) REFERENCES tb_customer_level(id)
) COMMENT '客户';

DROP TABLE IF EXISTS tb_tag;
CREATE TABLE tb_tag (
    id        INT PRIMARY KEY AUTO_INCREMENT,
    tag_name  VARCHAR(32) NOT NULL UNIQUE COMMENT '标签名',
    tag_group VARCHAR(32) COMMENT '标签分组'
) COMMENT '客户标签';

DROP TABLE IF EXISTS tb_customer_tag;
CREATE TABLE tb_customer_tag (
    customer_id BIGINT NOT NULL,
    tag_id      INT NOT NULL,
    assigned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (customer_id, tag_id),
    INDEX idx_tag (tag_id),
    CONSTRAINT fk_ct_customer FOREIGN KEY (customer_id) REFERENCES tb_customer(id),
    CONSTRAINT fk_ct_tag FOREIGN KEY (tag_id) REFERENCES tb_tag(id)
) COMMENT '客户-标签（多对多）';

-- ========== 商品与供应链 ==========

DROP TABLE IF EXISTS tb_category;
CREATE TABLE tb_category (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(64) NOT NULL COMMENT '分类名称',
    parent_id     INT COMMENT '父分类（自关联）',
    level         TINYINT DEFAULT 1 COMMENT '层级',
    sort_order    INT DEFAULT 0,
    INDEX idx_parent (parent_id),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES tb_category(id)
) COMMENT '商品分类（树形）';

DROP TABLE IF EXISTS tb_brand;
CREATE TABLE tb_brand (
    id         INT PRIMARY KEY AUTO_INCREMENT,
    brand_name VARCHAR(64) NOT NULL UNIQUE COMMENT '品牌名称',
    country    VARCHAR(32) COMMENT '国家/地区'
) COMMENT '品牌';

DROP TABLE IF EXISTS tb_supplier;
CREATE TABLE tb_supplier (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    supplier_code VARCHAR(32) NOT NULL UNIQUE COMMENT '供应商编码',
    supplier_name VARCHAR(128) NOT NULL COMMENT '供应商名称',
    contact_phone VARCHAR(20),
    city_id       INT COMMENT '所在城市',
    credit_rating TINYINT DEFAULT 3 COMMENT '信用 1-5',
    INDEX idx_supplier_city (city_id),
    CONSTRAINT fk_supplier_city FOREIGN KEY (city_id) REFERENCES tb_city(id)
) COMMENT '供应商';

DROP TABLE IF EXISTS tb_product;
CREATE TABLE tb_product (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_code  VARCHAR(32) NOT NULL UNIQUE COMMENT '商品编码',
    product_name  VARCHAR(200) NOT NULL COMMENT '商品名称',
    category_id   INT NOT NULL COMMENT '分类',
    brand_id      INT COMMENT '品牌',
    supplier_id   INT COMMENT '供应商',
    list_price    DECIMAL(12,2) NOT NULL COMMENT '标价',
    cost_price    DECIMAL(12,2) COMMENT '成本价',
    status        TINYINT DEFAULT 1 COMMENT '1上架 0下架',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_product_cat (category_id),
    INDEX idx_product_brand (brand_id),
    INDEX idx_product_supplier (supplier_id),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES tb_category(id),
    CONSTRAINT fk_product_brand FOREIGN KEY (brand_id) REFERENCES tb_brand(id),
    CONSTRAINT fk_product_supplier FOREIGN KEY (supplier_id) REFERENCES tb_supplier(id)
) COMMENT '商品SPU';

DROP TABLE IF EXISTS tb_product_sku;
CREATE TABLE tb_product_sku (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id  BIGINT NOT NULL COMMENT '所属商品',
    sku_code    VARCHAR(48) NOT NULL UNIQUE COMMENT 'SKU编码',
    spec_info   VARCHAR(128) COMMENT '规格描述',
    sale_price  DECIMAL(12,2) NOT NULL COMMENT '售价',
    weight_g    INT COMMENT '重量克',
    INDEX idx_sku_product (product_id),
    CONSTRAINT fk_sku_product FOREIGN KEY (product_id) REFERENCES tb_product(id)
) COMMENT '商品SKU';

DROP TABLE IF EXISTS tb_warehouse;
CREATE TABLE tb_warehouse (
    id             INT PRIMARY KEY AUTO_INCREMENT,
    warehouse_code VARCHAR(16) NOT NULL UNIQUE COMMENT '仓库编码',
    warehouse_name VARCHAR(64) NOT NULL COMMENT '仓库名称',
    city_id        INT COMMENT '所在城市',
    capacity       INT COMMENT '容量',
    INDEX idx_wh_city (city_id),
    CONSTRAINT fk_wh_city FOREIGN KEY (city_id) REFERENCES tb_city(id)
) COMMENT '仓库';

DROP TABLE IF EXISTS tb_inventory;
CREATE TABLE tb_inventory (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    warehouse_id INT NOT NULL,
    sku_id       BIGINT NOT NULL,
    quantity     INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_wh_sku (warehouse_id, sku_id),
    INDEX idx_inv_sku (sku_id),
    CONSTRAINT fk_inv_wh FOREIGN KEY (warehouse_id) REFERENCES tb_warehouse(id),
    CONSTRAINT fk_inv_sku FOREIGN KEY (sku_id) REFERENCES tb_product_sku(id)
) COMMENT '库存';

DROP TABLE IF EXISTS tb_stock_movement;
CREATE TABLE tb_stock_movement (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    warehouse_id  INT NOT NULL,
    sku_id        BIGINT NOT NULL,
    movement_type TINYINT NOT NULL COMMENT '1入库 2出库 3调拨 4盘点',
    quantity      INT NOT NULL COMMENT '变动数量',
    ref_no        VARCHAR(32) COMMENT '关联单号',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sm_wh (warehouse_id),
    INDEX idx_sm_sku (sku_id),
    INDEX idx_sm_time (created_at),
    CONSTRAINT fk_sm_wh FOREIGN KEY (warehouse_id) REFERENCES tb_warehouse(id),
    CONSTRAINT fk_sm_sku FOREIGN KEY (sku_id) REFERENCES tb_product_sku(id)
) COMMENT '库存流水';

DROP TABLE IF EXISTS tb_price_log;
CREATE TABLE tb_price_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id  BIGINT NOT NULL,
    old_price   DECIMAL(12,2),
    new_price   DECIMAL(12,2) NOT NULL,
    changed_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    changed_by  VARCHAR(32) COMMENT '操作人',
    INDEX idx_pl_product (product_id),
    INDEX idx_pl_time (changed_at),
    CONSTRAINT fk_pl_product FOREIGN KEY (product_id) REFERENCES tb_product(id)
) COMMENT '价格变更历史';

-- ========== 优惠券与订单 ==========

DROP TABLE IF EXISTS tb_voucher;
CREATE TABLE tb_voucher (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    voucher_code  VARCHAR(32) NOT NULL UNIQUE COMMENT '优惠券码',
    voucher_name  VARCHAR(64) NOT NULL COMMENT '优惠券名称',
    voucher_type  TINYINT DEFAULT 1 COMMENT '1满减 2折扣 3无门槛',
    face_value    DECIMAL(10,2) COMMENT '面额/折扣',
    min_amount    DECIMAL(10,2) DEFAULT 0 COMMENT '最低消费',
    start_time    DATETIME NOT NULL,
    end_time      DATETIME NOT NULL,
    total_count   INT DEFAULT 0 COMMENT '发放总量',
    used_count    INT DEFAULT 0 COMMENT '已使用',
    status        TINYINT DEFAULT 1 COMMENT '1有效 0失效'
) COMMENT '优惠券';

DROP TABLE IF EXISTS tb_voucher_order;
CREATE TABLE tb_voucher_order (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no      VARCHAR(32) NOT NULL UNIQUE COMMENT '订单号',
    customer_id   BIGINT NOT NULL COMMENT '客户',
    voucher_id    INT COMMENT '使用的优惠券',
    order_status  TINYINT NOT NULL DEFAULT 1 COMMENT '1待支付 2已支付 3已发货 4已完成 5已取消',
    pay_value     DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '实付金额',
    discount_value DECIMAL(12,2) DEFAULT 0 COMMENT '优惠金额',
    shipping_fee  DECIMAL(10,2) DEFAULT 0 COMMENT '运费',
    city_id       INT COMMENT '收货城市',
    create_time   DATETIME NOT NULL,
    pay_time      DATETIME COMMENT '支付时间',
    ship_time     DATETIME COMMENT '发货时间',
    complete_time DATETIME COMMENT '完成时间',
    INDEX idx_order_customer (customer_id),
    INDEX idx_order_voucher (voucher_id),
    INDEX idx_order_status (order_status),
    INDEX idx_order_create (create_time),
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES tb_customer(id),
    CONSTRAINT fk_order_voucher FOREIGN KEY (voucher_id) REFERENCES tb_voucher(id),
    CONSTRAINT fk_order_city FOREIGN KEY (city_id) REFERENCES tb_city(id)
) COMMENT '订单';

DROP TABLE IF EXISTS tb_order_item;
CREATE TABLE tb_order_item (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id    BIGINT NOT NULL COMMENT '订单',
    sku_id      BIGINT NOT NULL COMMENT 'SKU',
    product_id  BIGINT NOT NULL COMMENT '商品',
    quantity    INT NOT NULL COMMENT '数量',
    unit_price  DECIMAL(12,2) NOT NULL COMMENT '成交单价',
    line_amount DECIMAL(12,2) NOT NULL COMMENT '行金额',
    INDEX idx_oi_order (order_id),
    INDEX idx_oi_sku (sku_id),
    INDEX idx_oi_product (product_id),
    CONSTRAINT fk_oi_order FOREIGN KEY (order_id) REFERENCES tb_voucher_order(id),
    CONSTRAINT fk_oi_sku FOREIGN KEY (sku_id) REFERENCES tb_product_sku(id),
    CONSTRAINT fk_oi_product FOREIGN KEY (product_id) REFERENCES tb_product(id)
) COMMENT '订单明细';

DROP TABLE IF EXISTS tb_payment;
CREATE TABLE tb_payment (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id      BIGINT NOT NULL COMMENT '订单',
    payment_no    VARCHAR(48) NOT NULL UNIQUE COMMENT '支付流水号',
    pay_channel   VARCHAR(16) NOT NULL COMMENT 'alipay/wechat/card',
    pay_amount    DECIMAL(12,2) NOT NULL COMMENT '支付金额',
    pay_status    TINYINT DEFAULT 1 COMMENT '1成功 0失败',
    pay_time      DATETIME NOT NULL,
    INDEX idx_pay_order (order_id),
    INDEX idx_pay_time (pay_time),
    CONSTRAINT fk_pay_order FOREIGN KEY (order_id) REFERENCES tb_voucher_order(id)
) COMMENT '支付记录';

DROP TABLE IF EXISTS tb_refund;
CREATE TABLE tb_refund (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id      BIGINT NOT NULL,
    refund_no     VARCHAR(48) NOT NULL UNIQUE COMMENT '退款单号',
    refund_amount DECIMAL(12,2) NOT NULL,
    reason        VARCHAR(200) COMMENT '退款原因',
    refund_status TINYINT DEFAULT 1 COMMENT '1完成 0处理中',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_refund_order (order_id),
    CONSTRAINT fk_refund_order FOREIGN KEY (order_id) REFERENCES tb_voucher_order(id)
) COMMENT '退款';

DROP TABLE IF EXISTS tb_product_review;
CREATE TABLE tb_product_review (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id  BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    order_id    BIGINT COMMENT '关联订单',
    rating      TINYINT NOT NULL COMMENT '评分 1-5',
    content     VARCHAR(500) COMMENT '评价内容',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_review_product (product_id),
    INDEX idx_review_customer (customer_id),
    CONSTRAINT fk_review_product FOREIGN KEY (product_id) REFERENCES tb_product(id),
    CONSTRAINT fk_review_customer FOREIGN KEY (customer_id) REFERENCES tb_customer(id)
) COMMENT '商品评价';

-- ========== 组织与营销 ==========

DROP TABLE IF EXISTS tb_department;
CREATE TABLE tb_department (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    dept_name       VARCHAR(64) NOT NULL COMMENT '部门名称',
    parent_id       INT COMMENT '上级部门（自关联）',
    manager_name    VARCHAR(32) COMMENT '负责人',
    INDEX idx_dept_parent (parent_id),
    CONSTRAINT fk_dept_parent FOREIGN KEY (parent_id) REFERENCES tb_department(id)
) COMMENT '部门（树形）';

DROP TABLE IF EXISTS tb_employee;
CREATE TABLE tb_employee (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    emp_no        VARCHAR(16) NOT NULL UNIQUE COMMENT '工号',
    emp_name      VARCHAR(32) NOT NULL COMMENT '姓名',
    department_id INT COMMENT '部门',
    title         VARCHAR(32) COMMENT '职位',
    hire_date     DATE COMMENT '入职日期',
    status        TINYINT DEFAULT 1 COMMENT '1在职 0离职',
    INDEX idx_emp_dept (department_id),
    CONSTRAINT fk_emp_dept FOREIGN KEY (department_id) REFERENCES tb_department(id)
) COMMENT '员工';

DROP TABLE IF EXISTS tb_employee_region;
CREATE TABLE tb_employee_region (
    employee_id INT NOT NULL,
    region_id   INT NOT NULL,
    is_primary  TINYINT DEFAULT 0 COMMENT '是否主负责区域',
    PRIMARY KEY (employee_id, region_id),
    INDEX idx_er_region (region_id),
    CONSTRAINT fk_er_emp FOREIGN KEY (employee_id) REFERENCES tb_employee(id),
    CONSTRAINT fk_er_region FOREIGN KEY (region_id) REFERENCES tb_region(id)
) COMMENT '员工-区域（多对多）';

DROP TABLE IF EXISTS tb_marketing_channel;
CREATE TABLE tb_marketing_channel (
    id           INT PRIMARY KEY AUTO_INCREMENT,
    channel_code VARCHAR(16) NOT NULL UNIQUE COMMENT '渠道编码',
    channel_name VARCHAR(64) NOT NULL COMMENT '渠道名称',
    channel_type VARCHAR(16) COMMENT 'paid/organic/social'
) COMMENT '营销渠道';

DROP TABLE IF EXISTS tb_campaign;
CREATE TABLE tb_campaign (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    campaign_code VARCHAR(32) NOT NULL UNIQUE COMMENT '活动编码',
    campaign_name VARCHAR(128) NOT NULL COMMENT '活动名称',
    start_date    DATE NOT NULL,
    end_date      DATE NOT NULL,
    budget        DECIMAL(14,2) COMMENT '预算',
    status        TINYINT DEFAULT 1 COMMENT '1进行中 0结束'
) COMMENT '营销活动';

DROP TABLE IF EXISTS tb_campaign_channel;
CREATE TABLE tb_campaign_channel (
    campaign_id INT NOT NULL,
    channel_id  INT NOT NULL,
    spend       DECIMAL(12,2) DEFAULT 0 COMMENT '渠道花费',
    PRIMARY KEY (campaign_id, channel_id),
    INDEX idx_cc_channel (channel_id),
    CONSTRAINT fk_cc_campaign FOREIGN KEY (campaign_id) REFERENCES tb_campaign(id),
    CONSTRAINT fk_cc_channel FOREIGN KEY (channel_id) REFERENCES tb_marketing_channel(id)
) COMMENT '活动-渠道（多对多）';

DROP TABLE IF EXISTS tb_campaign_attribution;
CREATE TABLE tb_campaign_attribution (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id    BIGINT NOT NULL COMMENT '订单',
    campaign_id INT COMMENT '归因活动',
    channel_id  INT COMMENT '归因渠道',
    touch_time  DATETIME COMMENT '触达时间',
    INDEX idx_ca_order (order_id),
    INDEX idx_ca_campaign (campaign_id),
    CONSTRAINT fk_ca_order FOREIGN KEY (order_id) REFERENCES tb_voucher_order(id),
    CONSTRAINT fk_ca_campaign FOREIGN KEY (campaign_id) REFERENCES tb_campaign(id),
    CONSTRAINT fk_ca_channel FOREIGN KEY (channel_id) REFERENCES tb_marketing_channel(id)
) COMMENT '营销归因';

DROP TABLE IF EXISTS tb_page_view;
CREATE TABLE tb_page_view (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT COMMENT '客户（可空，未登录）',
    page_type   VARCHAR(32) NOT NULL COMMENT 'home/product/cart/checkout',
    product_id  BIGINT COMMENT '商品页',
    channel_id  INT COMMENT '来源渠道',
    session_id  VARCHAR(64) COMMENT '浏览会话',
    view_time   DATETIME NOT NULL,
    INDEX idx_pv_customer (customer_id),
    INDEX idx_pv_product (product_id),
    INDEX idx_pv_time (view_time),
    CONSTRAINT fk_pv_customer FOREIGN KEY (customer_id) REFERENCES tb_customer(id),
    CONSTRAINT fk_pv_product FOREIGN KEY (product_id) REFERENCES tb_product(id),
    CONSTRAINT fk_pv_channel FOREIGN KEY (channel_id) REFERENCES tb_marketing_channel(id)
) COMMENT '页面浏览（大数据分析）';

DROP TABLE IF EXISTS tb_shopping_cart;
CREATE TABLE tb_shopping_cart (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    status      TINYINT DEFAULT 1 COMMENT '1活跃 0已转化/废弃',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cart_customer (customer_id),
    CONSTRAINT fk_cart_customer FOREIGN KEY (customer_id) REFERENCES tb_customer(id)
) COMMENT '购物车';

DROP TABLE IF EXISTS tb_cart_item;
CREATE TABLE tb_cart_item (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    cart_id     BIGINT NOT NULL,
    sku_id      BIGINT NOT NULL,
    quantity    INT NOT NULL DEFAULT 1,
    added_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ci_cart (cart_id),
    INDEX idx_ci_sku (sku_id),
    CONSTRAINT fk_ci_cart FOREIGN KEY (cart_id) REFERENCES tb_shopping_cart(id),
    CONSTRAINT fk_ci_sku FOREIGN KEY (sku_id) REFERENCES tb_product_sku(id)
) COMMENT '购物车明细';

SET FOREIGN_KEY_CHECKS = 1;
