-- 苗丰施肥管控平台 数据库初始化脚本
-- Database: fertilizer
-- Engine: MySQL 8.0+
-- Charset: utf8mb4

CREATE DATABASE IF NOT EXISTS fertilizer
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE fertilizer;

-- =====================================================
-- 系统用户表
-- =====================================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    username        VARCHAR(50)     NOT NULL                COMMENT '用户名',
    password        VARCHAR(255)    NOT NULL                COMMENT '密码(BCrypt)',
    real_name       VARCHAR(50)     DEFAULT NULL            COMMENT '真实姓名',
    role            VARCHAR(20)     NOT NULL DEFAULT 'operator' COMMENT '角色: admin/operator',
    status          TINYINT         NOT NULL DEFAULT 1      COMMENT '状态: 1启用/0禁用',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除: 1已删/0未删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_role (role),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- =====================================================
-- 树木表
-- =====================================================
DROP TABLE IF EXISTS tree;
CREATE TABLE tree (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    species         VARCHAR(100)    NOT NULL                COMMENT '树种',
    variety         VARCHAR(100)    DEFAULT NULL            COMMENT '品种',
    age             INT             NOT NULL DEFAULT 0      COMMENT '树龄(年)',
    plant_date      DATE            DEFAULT NULL            COMMENT '种植日期',
    location        VARCHAR(200)    DEFAULT NULL            COMMENT '种植位置',
    area            DECIMAL(10,2)   DEFAULT NULL            COMMENT '种植面积(平方米)',
    quantity         INT             NOT NULL DEFAULT 1      COMMENT '数量',
    status          VARCHAR(20)     NOT NULL DEFAULT 'healthy' COMMENT '健康状态: healthy/sick/dead',
    remark          VARCHAR(500)    DEFAULT NULL            COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_species (species),
    KEY idx_age (age),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='树木表';

-- =====================================================
-- 肥料表
-- =====================================================
DROP TABLE IF EXISTS fertilizer;
CREATE TABLE fertilizer (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name            VARCHAR(100)    NOT NULL                COMMENT '肥料名称',
    type            VARCHAR(50)     NOT NULL                COMMENT '肥料类型: organic/compound/potash/nitrogen/phosphate',
    brand           VARCHAR(100)    DEFAULT NULL            COMMENT '品牌',
    nutrient_content VARCHAR(200)  DEFAULT NULL            COMMENT '养分含量说明',
    unit            VARCHAR(20)     NOT NULL DEFAULT 'kg'   COMMENT '计量单位',
    stock           DECIMAL(12,2)   NOT NULL DEFAULT 0      COMMENT '库存量',
    unit_price      DECIMAL(10,2)   DEFAULT NULL            COMMENT '单价(元)',
    remark          VARCHAR(500)    DEFAULT NULL            COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_type (type),
    KEY idx_name (name),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='肥料表';

-- =====================================================
-- 施肥记录表
-- =====================================================
DROP TABLE IF EXISTS fertilization_record;
CREATE TABLE fertilization_record (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    tree_id         BIGINT          NOT NULL                COMMENT '树木ID',
    fertilizer_id   BIGINT          NOT NULL                COMMENT '肥料ID',
    amount          DECIMAL(12,2)   NOT NULL                COMMENT '施肥用量',
    fertilize_date  DATE            NOT NULL                COMMENT '施肥日期',
    operator_id     BIGINT          NOT NULL                COMMENT '操作人ID',
    method          VARCHAR(50)     DEFAULT NULL            COMMENT '施肥方式: broadcast/furrow/foliar/drip',
    remark          VARCHAR(500)    DEFAULT NULL            COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_tree_id (tree_id),
    KEY idx_fertilizer_id (fertilizer_id),
    KEY idx_fertilize_date (fertilize_date),
    KEY idx_operator_id (operator_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='施肥记录表';

-- =====================================================
-- 施肥推荐规则表
-- =====================================================
DROP TABLE IF EXISTS fertilization_rule;
CREATE TABLE fertilization_rule (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    species         VARCHAR(100)    NOT NULL                COMMENT '适用树种',
    age_min         INT             NOT NULL DEFAULT 0      COMMENT '最小树龄(年)',
    age_max         INT             NOT NULL DEFAULT 999    COMMENT '最大树龄(年)',
    season          VARCHAR(20)     NOT NULL DEFAULT 'all'  COMMENT '适用季节: spring/summer/autumn/winter/all',
    fertilizer_id   BIGINT          NOT NULL                COMMENT '推荐肥料ID',
    recommend_amount DECIMAL(12,2)  NOT NULL                COMMENT '推荐用量(每棵)',
    method          VARCHAR(50)     NOT NULL DEFAULT 'broadcast' COMMENT '推荐施肥方式',
    priority        INT             NOT NULL DEFAULT 0      COMMENT '优先级(越大越优先)',
    remark          VARCHAR(500)    DEFAULT NULL            COMMENT '规则说明',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_species (species),
    KEY idx_season (season),
    KEY idx_fertilizer_id (fertilizer_id),
    KEY idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='施肥推荐规则表';

-- =====================================================
-- 初始化数据
-- =====================================================

-- 管理员账号: admin / admin123 (BCrypt)
INSERT INTO sys_user (id, username, password, real_name, role) VALUES
(1, 'admin', '$2a$10$ZxP9AXM9Qq6ifRDwSYOBAufycB6A/PyRwnA75LQCQCzwbR9HajvfS', '系统管理员', 'admin'),
(2, 'operator01', '$2a$10$ZxP9AXM9Qq6ifRDwSYOBAufycB6A/PyRwnA75LQCQCzwbR9HajvfS', '操作员张三', 'operator');

-- =====================================================
-- === 以下为平台扩展表 (用户客户端 + 管理员后台) ===
-- =====================================================

-- =====================================================
-- 消费者用户表
-- =====================================================
DROP TABLE IF EXISTS user;
CREATE TABLE user (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    username            VARCHAR(50)     NOT NULL                COMMENT '用户名',
    password            VARCHAR(200)    NOT NULL                COMMENT '密码(BCrypt)',
    phone               VARCHAR(20)     DEFAULT NULL            COMMENT '手机号',
    email               VARCHAR(100)    DEFAULT NULL            COMMENT '邮箱',
    nickname            VARCHAR(50)     DEFAULT NULL            COMMENT '昵称',
    avatar              VARCHAR(500)    DEFAULT NULL            COMMENT '头像URL',
    gender              TINYINT         DEFAULT 0               COMMENT '性别: 0未知/1男/2女',
    birthday            DATE            DEFAULT NULL            COMMENT '生日',
    points              INT             DEFAULT 0               COMMENT '积分',
    membership_level_id BIGINT          DEFAULT NULL            COMMENT '会员等级ID',
    status              TINYINT         DEFAULT 1               COMMENT '状态: 1正常/0禁用',
    last_login_time     DATETIME        DEFAULT NULL            COMMENT '最后登录时间',
    last_login_ip       VARCHAR(50)     DEFAULT NULL            COMMENT '最后登录IP',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消费者用户表';

-- =====================================================
-- 收货地址表
-- =====================================================
DROP TABLE IF EXISTS user_address;
CREATE TABLE user_address (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT          NOT NULL                COMMENT '用户ID',
    receiver_name   VARCHAR(50)     NOT NULL                COMMENT '收件人',
    receiver_phone  VARCHAR(20)     NOT NULL                COMMENT '收件人电话',
    province        VARCHAR(50)     NOT NULL                COMMENT '省',
    city            VARCHAR(50)     NOT NULL                COMMENT '市',
    district        VARCHAR(50)     NOT NULL                COMMENT '区',
    detail          VARCHAR(200)    NOT NULL                COMMENT '详细地址',
    postal_code     VARCHAR(10)     DEFAULT NULL            COMMENT '邮编',
    is_default      TINYINT         DEFAULT 0               COMMENT '是否默认: 1是/0否',
    tag             VARCHAR(20)     DEFAULT NULL            COMMENT '标签: home/office',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收货地址表';

-- =====================================================
-- 验证码表
-- =====================================================
DROP TABLE IF EXISTS verification_code;
CREATE TABLE verification_code (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    target          VARCHAR(100)    NOT NULL                COMMENT '手机号或邮箱',
    code            VARCHAR(10)     NOT NULL                COMMENT '验证码',
    type            VARCHAR(20)     NOT NULL                COMMENT '类型: register/login/reset_password',
    used            TINYINT         DEFAULT 0               COMMENT '是否已用: 1是/0否',
    expire_time     DATETIME        NOT NULL                COMMENT '过期时间',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_target_type (target, type),
    KEY idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='验证码表';

-- =====================================================
-- 管理员角色表
-- =====================================================
DROP TABLE IF EXISTS admin_role;
CREATE TABLE admin_role (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name            VARCHAR(50)     NOT NULL                COMMENT '角色名称',
    description     VARCHAR(200)    DEFAULT NULL            COMMENT '角色描述',
    permissions     TEXT            NOT NULL                COMMENT '权限JSON数组',
    status          TINYINT         DEFAULT 1               COMMENT '状态: 1启用/0禁用',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员角色表';

-- =====================================================
-- 平台参数配置表 (KV结构)
-- =====================================================
DROP TABLE IF EXISTS platform_config;
CREATE TABLE platform_config (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    config_key      VARCHAR(100)    NOT NULL                COMMENT '配置键',
    config_value    TEXT            DEFAULT NULL            COMMENT '配置值',
    config_group    VARCHAR(50)     DEFAULT NULL            COMMENT '配置分组',
    description     VARCHAR(200)    DEFAULT NULL            COMMENT '配置说明',
    value_type      VARCHAR(20)     DEFAULT 'string'        COMMENT '值类型: string/int/json/image',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key),
    KEY idx_config_group (config_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台参数配置表';

-- =====================================================
-- 系统操作日志表
-- =====================================================
DROP TABLE IF EXISTS system_log;
CREATE TABLE system_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    operator_id     BIGINT          DEFAULT NULL            COMMENT '操作人ID',
    operator_name   VARCHAR(50)     DEFAULT NULL            COMMENT '操作人用户名',
    module          VARCHAR(50)     DEFAULT NULL            COMMENT '操作模块',
    action          VARCHAR(50)     DEFAULT NULL            COMMENT '操作动作',
    target          VARCHAR(200)    DEFAULT NULL            COMMENT '操作对象',
    request_params  TEXT            DEFAULT NULL            COMMENT '请求参数JSON',
    ip              VARCHAR(50)     DEFAULT NULL            COMMENT '操作IP',
    user_agent      VARCHAR(500)    DEFAULT NULL            COMMENT '浏览器UA',
    cost_time       BIGINT          DEFAULT NULL            COMMENT '耗时(ms)',
    result          VARCHAR(20)     DEFAULT NULL            COMMENT '结果: success/failure',
    error_msg       TEXT            DEFAULT NULL            COMMENT '错误信息',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_operator_id (operator_id),
    KEY idx_module_action (module, action),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表';

-- =====================================================
-- 商品分类表
-- =====================================================
DROP TABLE IF EXISTS product_category;
CREATE TABLE product_category (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name            VARCHAR(50)     NOT NULL                COMMENT '分类名称',
    parent_id       BIGINT          DEFAULT 0               COMMENT '父级ID: 0=根分类',
    type            VARCHAR(20)     NOT NULL                COMMENT '分类类型: tree/fertilizer/encyclopedia',
    sort_order      INT             DEFAULT 0               COMMENT '排序',
    icon            VARCHAR(500)    DEFAULT NULL            COMMENT '图标URL',
    description     VARCHAR(200)    DEFAULT NULL            COMMENT '描述',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_type (type),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- =====================================================
-- 商品SPU表
-- =====================================================
DROP TABLE IF EXISTS product;
CREATE TABLE product (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name            VARCHAR(200)    NOT NULL                COMMENT '商品名称',
    product_type    VARCHAR(20)     NOT NULL                COMMENT '商品类型: tree/fertilizer',
    category_id     BIGINT          DEFAULT NULL            COMMENT '分类ID',
    brand           VARCHAR(100)    DEFAULT NULL            COMMENT '品牌',
    cover_image     VARCHAR(500)    DEFAULT NULL            COMMENT '封面图URL',
    images          TEXT            DEFAULT NULL            COMMENT '图片JSON数组',
    video_url       VARCHAR(500)    DEFAULT NULL            COMMENT '视频URL',
    price           DECIMAL(10,2)   NOT NULL                COMMENT '售价',
    original_price  DECIMAL(10,2)   DEFAULT NULL            COMMENT '原价',
    stock           INT             DEFAULT 0               COMMENT '库存',
    unit            VARCHAR(20)     DEFAULT 'piece'         COMMENT '销售单位',
    sales_count     INT             DEFAULT 0               COMMENT '销量',
    status          TINYINT         DEFAULT 1               COMMENT '状态: 0下架/1上架',
    is_recommend    TINYINT         DEFAULT 0               COMMENT '是否推荐',
    is_new          TINYINT         DEFAULT 0               COMMENT '是否新品',
    sort_order      INT             DEFAULT 0               COMMENT '排序',
    description     TEXT            DEFAULT NULL            COMMENT '商品描述(富文本)',
    min_purchase    INT             DEFAULT 1               COMMENT '最低购买量',
    freight         DECIMAL(10,2)   DEFAULT 0               COMMENT '运费',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_product_type (product_type),
    KEY idx_category_id (category_id),
    KEY idx_status (status),
    KEY idx_is_recommend (is_recommend),
    KEY idx_sales_count (sales_count),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品SPU表';

-- =====================================================
-- 商品扩展属性表
-- =====================================================
DROP TABLE IF EXISTS product_detail;
CREATE TABLE product_detail (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    product_id      BIGINT          NOT NULL                COMMENT '商品ID',
    detail_type     VARCHAR(20)     NOT NULL                COMMENT '类型: tree/fertilizer',
    attrs_json      JSON            DEFAULT NULL            COMMENT '扩展属性JSON',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品扩展属性表';

-- =====================================================
-- 购物车表
-- =====================================================
DROP TABLE IF EXISTS shopping_cart_item;
CREATE TABLE shopping_cart_item (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT          NOT NULL                COMMENT '用户ID',
    product_id      BIGINT          NOT NULL                COMMENT '商品ID',
    quantity        INT             NOT NULL DEFAULT 1      COMMENT '数量',
    selected        TINYINT         DEFAULT 1               COMMENT '是否选中: 1是/0否',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_product (user_id, product_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

-- =====================================================
-- 订单主表
-- =====================================================
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no        VARCHAR(32)     NOT NULL                COMMENT '订单编号',
    user_id         BIGINT          NOT NULL                COMMENT '用户ID',
    address_snapshot JSON          NOT NULL                COMMENT '地址快照JSON',
    total_amount    DECIMAL(12,2)   NOT NULL                COMMENT '商品总额',
    freight_amount  DECIMAL(10,2)   DEFAULT 0               COMMENT '运费',
    discount_amount DECIMAL(10,2)   DEFAULT 0               COMMENT '优惠金额',
    pay_amount      DECIMAL(12,2)   NOT NULL                COMMENT '实付金额',
    status          VARCHAR(20)     DEFAULT 'pending_pay'   COMMENT '状态: pending_pay/pending_ship/shipped/completed/cancelled/refunding/refunded',
    payment_method  VARCHAR(20)     DEFAULT NULL            COMMENT '支付方式: wechat/alipay',
    pay_time        DATETIME        DEFAULT NULL            COMMENT '支付时间',
    ship_time       DATETIME        DEFAULT NULL            COMMENT '发货时间',
    complete_time   DATETIME        DEFAULT NULL            COMMENT '完成时间',
    cancel_time     DATETIME        DEFAULT NULL            COMMENT '取消时间',
    cancel_reason   VARCHAR(500)    DEFAULT NULL            COMMENT '取消原因',
    user_remark     VARCHAR(500)    DEFAULT NULL            COMMENT '用户备注',
    admin_remark    VARCHAR(500)    DEFAULT NULL            COMMENT '管理员备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time),
    KEY idx_pay_time (pay_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单主表';

-- =====================================================
-- 订单明细表
-- =====================================================
DROP TABLE IF EXISTS order_item;
CREATE TABLE order_item (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id        BIGINT          NOT NULL                COMMENT '订单ID',
    order_no        VARCHAR(32)     NOT NULL                COMMENT '订单编号(冗余)',
    product_id      BIGINT          NOT NULL                COMMENT '商品ID',
    product_name    VARCHAR(200)    NOT NULL                COMMENT '商品名称快照',
    product_image   VARCHAR(500)    DEFAULT NULL            COMMENT '商品图片快照',
    product_attrs   JSON            DEFAULT NULL            COMMENT '商品属性快照JSON',
    price           DECIMAL(10,2)   NOT NULL                COMMENT '单价快照',
    quantity        INT             NOT NULL                COMMENT '数量',
    total_price     DECIMAL(12,2)   NOT NULL                COMMENT '小计',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_order_no (order_no),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';

-- =====================================================
-- 支付记录表
-- =====================================================
DROP TABLE IF EXISTS payment;
CREATE TABLE payment (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id        BIGINT          NOT NULL                COMMENT '订单ID',
    order_no        VARCHAR(32)     NOT NULL                COMMENT '订单编号',
    user_id         BIGINT          NOT NULL                COMMENT '用户ID',
    pay_method      VARCHAR(20)     NOT NULL                COMMENT '支付方式: wechat/alipay',
    amount          DECIMAL(12,2)   NOT NULL                COMMENT '支付金额',
    trade_no        VARCHAR(64)     DEFAULT NULL            COMMENT '第三方交易号',
    status          VARCHAR(20)     DEFAULT 'pending'       COMMENT '状态: pending/success/failed/refunded',
    pay_time        DATETIME        DEFAULT NULL            COMMENT '实际支付时间',
    refund_amount   DECIMAL(12,2)   DEFAULT NULL            COMMENT '退款金额',
    refund_time     DATETIME        DEFAULT NULL            COMMENT '退款时间',
    raw_response    TEXT            DEFAULT NULL            COMMENT '第三方回调原始数据',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_order_no (order_no),
    KEY idx_trade_no (trade_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

-- =====================================================
-- 树木百科词条表
-- =====================================================
DROP TABLE IF EXISTS encyclopedia_entry;
CREATE TABLE encyclopedia_entry (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name            VARCHAR(200)    NOT NULL                COMMENT '树木名称',
    scientific_name VARCHAR(200)    DEFAULT NULL            COMMENT '拉丁学名',
    alias           VARCHAR(200)    DEFAULT NULL            COMMENT '别名',
    pinyin          VARCHAR(200)    DEFAULT NULL            COMMENT '拼音',
    family          VARCHAR(100)    DEFAULT NULL            COMMENT '科',
    genus           VARCHAR(100)    DEFAULT NULL            COMMENT '属',
    category_id     BIGINT          DEFAULT NULL            COMMENT '分类ID',
    cover_image     VARCHAR(500)    DEFAULT NULL            COMMENT '封面图',
    images          TEXT            DEFAULT NULL            COMMENT '图片JSON数组',
    description     TEXT            DEFAULT NULL            COMMENT '简介',
    morphology      TEXT            DEFAULT NULL            COMMENT '形态特征',
    distribution    TEXT            DEFAULT NULL            COMMENT '分布地区',
    habitat         TEXT            DEFAULT NULL            COMMENT '生长习性',
    care_guide      TEXT            DEFAULT NULL            COMMENT '养护要点',
    value_description TEXT          DEFAULT NULL            COMMENT '价值描述',
    is_published    TINYINT         DEFAULT 1               COMMENT '发布状态: 0草稿/1已发布',
    view_count      INT             DEFAULT 0               COMMENT '浏览量',
    like_count      INT             DEFAULT 0               COMMENT '点赞数',
    comment_count   INT             DEFAULT 0               COMMENT '评论数',
    tags            VARCHAR(500)    DEFAULT NULL            COMMENT '标签(逗号分隔)',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_name (name),
    KEY idx_scientific_name (scientific_name),
    KEY idx_category_id (category_id),
    KEY idx_is_published (is_published),
    FULLTEXT KEY ft_name_alias (name, alias, pinyin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='树木百科词条表';

-- =====================================================
-- 用户上传表 (待审核)
-- =====================================================
DROP TABLE IF EXISTS user_upload;
CREATE TABLE user_upload (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT          NOT NULL                COMMENT '用户ID',
    name            VARCHAR(200)    NOT NULL                COMMENT '树木名称',
    location        VARCHAR(200)    DEFAULT NULL            COMMENT '发现地点',
    description     TEXT            DEFAULT NULL            COMMENT '描述',
    features        TEXT            DEFAULT NULL            COMMENT '形态特征',
    images          TEXT            NOT NULL                COMMENT '图片JSON数组',
    tags            VARCHAR(500)    DEFAULT NULL            COMMENT '标签',
    status          VARCHAR(20)     DEFAULT 'pending'       COMMENT '状态: pending/approved/rejected',
    review_comment  VARCHAR(500)    DEFAULT NULL            COMMENT '审核意见',
    reviewer_id     BIGINT          DEFAULT NULL            COMMENT '审核人ID',
    review_time     DATETIME        DEFAULT NULL            COMMENT '审核时间',
    encyclopedia_id BIGINT          DEFAULT NULL            COMMENT '关联百科词条ID(审核通过后)',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_status (status),
    KEY idx_reviewer_id (reviewer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户上传表';

-- =====================================================
-- 科普文章表
-- =====================================================
DROP TABLE IF EXISTS encyclopedia_article;
CREATE TABLE encyclopedia_article (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    title           VARCHAR(200)    NOT NULL                COMMENT '文章标题',
    summary         VARCHAR(500)    DEFAULT NULL            COMMENT '摘要',
    cover_image     VARCHAR(500)    DEFAULT NULL            COMMENT '封面图',
    images          TEXT            DEFAULT NULL            COMMENT '图片JSON数组',
    content         LONGTEXT        NOT NULL                COMMENT '文章内容(富文本)',
    author_id       BIGINT          DEFAULT NULL            COMMENT '作者ID(管理员)',
    category_id     BIGINT          DEFAULT NULL            COMMENT '分类ID',
    tags            VARCHAR(500)    DEFAULT NULL            COMMENT '标签',
    is_published    TINYINT         DEFAULT 1               COMMENT '发布状态',
    is_top          TINYINT         DEFAULT 0               COMMENT '是否置顶',
    is_recommend    TINYINT         DEFAULT 0               COMMENT '是否推荐',
    view_count      INT             DEFAULT 0               COMMENT '浏览量',
    like_count      INT             DEFAULT 0               COMMENT '点赞数',
    comment_count   INT             DEFAULT 0               COMMENT '评论数',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_category_id (category_id),
    KEY idx_is_published (is_published),
    KEY idx_is_top (is_top),
    KEY idx_view_count (view_count),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科普文章表';

-- =====================================================
-- 社区评论表
-- =====================================================
DROP TABLE IF EXISTS community_comment;
CREATE TABLE community_comment (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT          NOT NULL                COMMENT '评论用户ID',
    target_type     VARCHAR(30)     NOT NULL                COMMENT '目标类型: encyclopedia/article/upload/product',
    target_id       BIGINT          NOT NULL                COMMENT '目标ID',
    parent_id       BIGINT          DEFAULT 0               COMMENT '父评论ID: 0=一级评论',
    reply_to_user_id BIGINT         DEFAULT NULL            COMMENT '回复的目标用户ID',
    content         TEXT            NOT NULL                COMMENT '评论内容',
    is_deleted_by_admin TINYINT     DEFAULT 0               COMMENT '管理员删除标记',
    ip              VARCHAR(50)     DEFAULT NULL            COMMENT '评论IP',
    like_count      INT             DEFAULT 0               COMMENT '点赞数',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_target (target_type, target_id),
    KEY idx_user_id (user_id),
    KEY idx_parent_id (parent_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区评论表';

-- =====================================================
-- 社区点赞表
-- =====================================================
DROP TABLE IF EXISTS community_like;
CREATE TABLE community_like (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT          NOT NULL                COMMENT '用户ID',
    target_type     VARCHAR(30)     NOT NULL                COMMENT '目标类型: comment/encyclopedia/article/upload/product',
    target_id       BIGINT          NOT NULL                COMMENT '目标ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    KEY idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区点赞表';

-- =====================================================
-- 收藏表
-- =====================================================
DROP TABLE IF EXISTS favorite;
CREATE TABLE favorite (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT          NOT NULL                COMMENT '用户ID',
    target_type     VARCHAR(30)     NOT NULL                COMMENT '目标类型: product/encyclopedia/article',
    target_id       BIGINT          NOT NULL                COMMENT '目标ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- =====================================================
-- 浏览历史表
-- =====================================================
DROP TABLE IF EXISTS browsing_history;
CREATE TABLE browsing_history (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT          NOT NULL                COMMENT '用户ID',
    target_type     VARCHAR(30)     NOT NULL                COMMENT '目标类型: product/encyclopedia/article',
    target_id       BIGINT          NOT NULL                COMMENT '目标ID',
    target_name     VARCHAR(200)    DEFAULT NULL            COMMENT '目标名称快照',
    target_image    VARCHAR(500)    DEFAULT NULL            COMMENT '目标图片快照',
    stay_duration   INT             DEFAULT 0               COMMENT '停留时长(秒)',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浏览历史表';

-- =====================================================
-- 会员等级表
-- =====================================================
DROP TABLE IF EXISTS membership_level;
CREATE TABLE membership_level (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name            VARCHAR(50)     NOT NULL                COMMENT '等级名称',
    level           INT             NOT NULL                COMMENT '等级数值(越大越高)',
    min_points      INT             NOT NULL                COMMENT '最低积分门槛',
    discount_rate   DECIMAL(3,2)    DEFAULT 1.00            COMMENT '折扣率: 0.95=95折',
    icon            VARCHAR(500)    DEFAULT NULL            COMMENT '等级图标URL',
    description     VARCHAR(200)    DEFAULT NULL            COMMENT '等级说明',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_level (level),
    KEY idx_min_points (min_points)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员等级表';

-- =====================================================
-- 积分流水表
-- =====================================================
DROP TABLE IF EXISTS points_record;
CREATE TABLE points_record (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT          NOT NULL                COMMENT '用户ID',
    points          INT             NOT NULL                COMMENT '积分变动(正=获得/负=消费)',
    type            VARCHAR(30)     NOT NULL                COMMENT '类型: register/purchase/review/upload/sign_in/activity/exchange/expire',
    description     VARCHAR(200)    DEFAULT NULL            COMMENT '描述',
    ref_id          BIGINT          DEFAULT NULL            COMMENT '关联实体ID',
    balance_after   INT             NOT NULL                COMMENT '变动后余额',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_user_time (user_id, create_time),
    KEY idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分流水表';

-- =====================================================
-- FAQ表
-- =====================================================
DROP TABLE IF EXISTS faq;
CREATE TABLE faq (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    question        VARCHAR(500)    NOT NULL                COMMENT '问题',
    answer          TEXT            NOT NULL                COMMENT '答案(富文本)',
    category        VARCHAR(50)     NOT NULL                COMMENT '分类: order/product/account/care',
    sort_order      INT             DEFAULT 0               COMMENT '排序',
    is_published    TINYINT         DEFAULT 1               COMMENT '发布状态',
    view_count      INT             DEFAULT 0               COMMENT '查看次数',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_category (category),
    KEY idx_is_published (is_published)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='FAQ表';

-- =====================================================
-- 意见反馈表
-- =====================================================
DROP TABLE IF EXISTS feedback;
CREATE TABLE feedback (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT          DEFAULT NULL            COMMENT '用户ID(可匿名)',
    contact         VARCHAR(100)    DEFAULT NULL            COMMENT '联系方式',
    content         TEXT            NOT NULL                COMMENT '反馈内容',
    images          TEXT            DEFAULT NULL            COMMENT '图片JSON数组',
    type            VARCHAR(20)     NOT NULL                COMMENT '类型: suggestion/bug/complaint/other',
    status          VARCHAR(20)     DEFAULT 'pending'       COMMENT '状态: pending/processing/completed/closed',
    handler_id      BIGINT          DEFAULT NULL            COMMENT '处理人ID',
    handler_reply   TEXT            DEFAULT NULL            COMMENT '处理回复',
    handle_time     DATETIME        DEFAULT NULL            COMMENT '处理时间',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_status (status),
    KEY idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='意见反馈表';

-- =====================================================
-- 活动促销表
-- =====================================================
DROP TABLE IF EXISTS activity;
CREATE TABLE activity (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    title           VARCHAR(200)    NOT NULL                COMMENT '活动标题',
    description     TEXT            DEFAULT NULL            COMMENT '活动描述',
    cover_image     VARCHAR(500)    DEFAULT NULL            COMMENT '封面图',
    type            VARCHAR(30)     NOT NULL                COMMENT '类型: flash_sale/discount/new_arrival/holiday/campaign',
    rule_json       JSON            DEFAULT NULL            COMMENT '活动规则JSON',
    start_time      DATETIME        NOT NULL                COMMENT '开始时间',
    end_time        DATETIME        NOT NULL                COMMENT '结束时间',
    status          VARCHAR(20)     DEFAULT 'draft'         COMMENT '状态: draft/active/ended',
    is_banner       TINYINT         DEFAULT 0               COMMENT '是否首页banner',
    sort_order      INT             DEFAULT 0               COMMENT '排序',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_status (status),
    KEY idx_start_end (start_time, end_time),
    KEY idx_is_banner (is_banner)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动促销表';

-- =====================================================
-- 消息通知表
-- =====================================================
DROP TABLE IF EXISTS message;
CREATE TABLE message (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT          DEFAULT NULL            COMMENT '用户ID(NULL=全员广播)',
    title           VARCHAR(200)    NOT NULL                COMMENT '消息标题',
    content         TEXT            NOT NULL                COMMENT '消息内容',
    type            VARCHAR(30)     NOT NULL                COMMENT '类型: order/activity/system/interaction',
    target_type     VARCHAR(30)     DEFAULT NULL            COMMENT '跳转目标类型',
    target_id       BIGINT          DEFAULT NULL            COMMENT '跳转目标ID',
    is_read         TINYINT         DEFAULT 0               COMMENT '是否已读',
    read_time       DATETIME        DEFAULT NULL            COMMENT '阅读时间',
    push_channel    VARCHAR(20)     DEFAULT 'in_app'        COMMENT '推送渠道: in_app/sms/email',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_user_read (user_id, is_read),
    KEY idx_type (type),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息通知表';

-- =====================================================
-- 文件上传记录表
-- =====================================================
DROP TABLE IF EXISTS file_upload;
CREATE TABLE file_upload (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    original_name   VARCHAR(500)    NOT NULL                COMMENT '原始文件名',
    stored_name     VARCHAR(500)    NOT NULL                COMMENT '存储文件名(UUID)',
    file_path       VARCHAR(500)    NOT NULL                COMMENT '存储路径',
    file_url        VARCHAR(500)    NOT NULL                COMMENT '访问URL',
    file_size       BIGINT          DEFAULT NULL            COMMENT '文件大小(字节)',
    mime_type       VARCHAR(100)    DEFAULT NULL            COMMENT 'MIME类型',
    file_ext        VARCHAR(20)     DEFAULT NULL            COMMENT '文件扩展名',
    uploader_id     BIGINT          DEFAULT NULL            COMMENT '上传者ID',
    uploader_type   VARCHAR(20)     DEFAULT NULL            COMMENT '上传者类型: admin/user',
    purpose         VARCHAR(30)     DEFAULT NULL            COMMENT '用途: product_image/avatar/article_image/encyclopedia_image',
    width           INT             DEFAULT NULL            COMMENT '图片宽度',
    height          INT             DEFAULT NULL            COMMENT '图片高度',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_uploader (uploader_id, uploader_type),
    KEY idx_purpose (purpose)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件上传记录表';

-- 示例树木数据
INSERT INTO tree (id, species, variety, age, plant_date, location, area, quantity, status) VALUES
(1, '苹果', '红富士', 5, '2021-03-15', 'A区-01号地', 1200.00, 100, 'healthy'),
(2, '柑橘', '砂糖橘', 3, '2023-04-20', 'B区-03号地', 800.00, 80, 'healthy'),
(3, '桃树', '水蜜桃', 7, '2019-02-10', 'C区-05号地', 1500.00, 150, 'sick'),
(4, '杨树', '毛白杨', 10, '2016-11-05', 'D区-02号地', 3000.00, 200, 'healthy');

-- 示例肥料数据
INSERT INTO fertilizer (id, name, type, brand, nutrient_content, unit, stock, unit_price) VALUES
(1, '有机复合肥', 'organic', '绿源生物', '有机质≥45%, N+P2O5+K2O≥5%', 'kg', 5000.00, 3.50),
(2, '尿素', 'nitrogen', '中化化肥', 'N≥46%', 'kg', 3000.00, 2.80),
(3, '磷酸二铵', 'compound', '云天化', 'N≥18%, P2O5≥46%', 'kg', 2000.00, 4.20),
(4, '硫酸钾', 'potash', '盐湖股份', 'K2O≥50%', 'kg', 1500.00, 5.00),
(5, '过磷酸钙', 'phosphate', '开磷', 'P2O5≥12%', 'kg', 2500.00, 1.80);

-- 推荐规则数据
INSERT INTO fertilization_rule (id, species, age_min, age_max, season, fertilizer_id, recommend_amount, method, priority, remark) VALUES
(1, '苹果', 2, 5, 'spring', 1, 2.50, 'furrow', 10, '幼树春季基肥'),
(2, '苹果', 5, 15, 'summer', 2, 0.50, 'broadcast', 5, '成年苹果追氮肥'),
(3, '柑橘', 1, 4, 'spring', 3, 1.00, 'furrow', 10, '幼龄柑橘磷肥'),
(4, '柑橘', 4, 20, 'summer', 4, 0.80, 'broadcast', 8, '柑橘膨果钾肥'),
(5, '桃树', 0, 999, 'autumn', 1, 3.00, 'furrow', 8, '桃树秋施基肥'),
(6, '杨树', 0, 999, 'spring', 2, 1.00, 'broadcast', 3, '杨树春季追肥'),
(7, '苹果', 0, 999, 'all', 5, 1.50, 'broadcast', 2, '苹果通用磷肥'),
(8, '柑橘', 0, 999, 'all', 1, 2.00, 'broadcast', 2, '柑橘通用有机肥');
