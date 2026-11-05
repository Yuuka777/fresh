-- ============================================================
--  鲜禾（xianhe-fresh）· 建库建表
--  库：xianhe_fresh        字符集：utf8mb4 / utf8mb4_0900_ai_ci
--  共 11 张表，命名与字段依《开发计划.md》§2.3 / §2.4 / §3
--    苍穹外卖 → 鲜禾：employee→staff、category→goods_category、dish→goods、
--    dish_flavor→goods_spec、setmeal→bundle、setmeal_dish→bundle_goods、
--    user→member、address_book→address、shopping_cart→cart、
--    orders→order_main、order_detail→order_item
--  相对原项目的 4 增 4 删：
--    + goods.origin / goods.shelf_life / goods.storage_zone / bundle.storage_zone
--    - order_main.tableware_number / tableware_status、member.id_number
--  明确不做：库存字段与库存表（见《开发计划.md》§3）
-- ============================================================

CREATE DATABASE IF NOT EXISTS `xianhe_fresh`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE `xianhe_fresh`;

-- ------------------------------------------------------------
-- 1. 店员（原 employee）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `staff`;
CREATE TABLE `staff` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        varchar(32)  NOT NULL                COMMENT '姓名',
  `username`    varchar(32)  NOT NULL                COMMENT '登录用户名',
  `password`    varchar(255) NOT NULL                COMMENT '密码（PBKDF2，兼容旧 MD5）',
  `phone`       varchar(11)  NOT NULL                COMMENT '手机号',
  `sex`         varchar(2)   NOT NULL                COMMENT '性别',
  `id_number`   varchar(18)  NOT NULL                COMMENT '身份证号（店员入职实名，会员不实名）',
  `status`      int          NOT NULL DEFAULT 1      COMMENT '状态 0 禁用 / 1 启用',
  `create_time` datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_time` datetime              DEFAULT NULL   COMMENT '更新时间',
  `create_user` bigint                DEFAULT NULL   COMMENT '创建人',
  `update_user` bigint                DEFAULT NULL   COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_staff_username` (`username`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='店员';

-- ------------------------------------------------------------
-- 2. 商品分类（原 category）—— 商品分类与组合分类同表，靠 type 区分
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `goods_category`;
CREATE TABLE `goods_category` (
  `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type`        int         NOT NULL                COMMENT '分类类型 1 商品分类 / 2 组合分类',
  `name`        varchar(32) NOT NULL                COMMENT '分类名称',
  `sort`        int         NOT NULL                COMMENT '排序值，越小越靠前（必填，无默认值）',
  `status`      int         NOT NULL DEFAULT 1      COMMENT '状态 0 禁用 / 1 启用',
  `create_time` datetime             DEFAULT NULL   COMMENT '创建时间',
  `update_time` datetime             DEFAULT NULL   COMMENT '更新时间',
  `create_user` bigint               DEFAULT NULL   COMMENT '创建人',
  `update_user` bigint               DEFAULT NULL   COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_goods_category_name` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='商品分类（含组合分类）';

-- ------------------------------------------------------------
-- 3. 商品（原 dish）—— 新增 origin / shelf_life / storage_zone
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `goods`;
CREATE TABLE `goods` (
  `id`           bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`         varchar(32)   NOT NULL                COMMENT '商品名称',
  `category_id`  bigint        NOT NULL                COMMENT '所属商品分类 id（type = 1）',
  `price`        decimal(10,2) NOT NULL                COMMENT '售价',
  `image`        varchar(255)           DEFAULT NULL   COMMENT '图片 URL，可为空（无图时前端用首字色块占位）',
  `description`  varchar(255)           DEFAULT NULL   COMMENT '描述',
  `status`       int           NOT NULL DEFAULT 1      COMMENT '状态 0 下架 / 1 上架',
  `origin`       varchar(64)            DEFAULT NULL   COMMENT '产地',
  `shelf_life`   varchar(32)            DEFAULT NULL   COMMENT '保质期文案，如「冷藏 3 天」',
  `storage_zone` tinyint       NOT NULL                COMMENT '温区 1 常温 / 2 冷藏 / 3 冷冻',
  `create_time`  datetime               DEFAULT NULL   COMMENT '创建时间',
  `update_time`  datetime               DEFAULT NULL   COMMENT '更新时间',
  `create_user`  bigint                 DEFAULT NULL   COMMENT '创建人',
  `update_user`  bigint                 DEFAULT NULL   COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_goods_name` (`name`) USING BTREE,
  KEY `idx_goods_category` (`category_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='商品';

-- ------------------------------------------------------------
-- 4. 商品规格（原 dish_flavor）—— value 存候选值数组的 JSON 文本
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `goods_spec`;
CREATE TABLE `goods_spec` (
  `id`       bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `goods_id` bigint       NOT NULL                COMMENT '商品 id',
  `name`     varchar(32)           DEFAULT NULL   COMMENT '规格名称，如「净重」',
  `value`    varchar(255)          DEFAULT NULL   COMMENT '候选值数组的 JSON 文本，如 ["约500g","约1kg"]',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_spec_goods` (`goods_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='商品规格';

-- ------------------------------------------------------------
-- 5. 组合箱（原 setmeal）—— 新增 storage_zone（按内含商品最高温区推导）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `bundle`;
CREATE TABLE `bundle` (
  `id`           bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id`  bigint        NOT NULL                COMMENT '所属组合分类 id（type = 2）',
  `name`         varchar(32)   NOT NULL                COMMENT '组合箱名称',
  `price`        decimal(10,2) NOT NULL                COMMENT '售价',
  `status`       int           NOT NULL DEFAULT 1      COMMENT '状态 0 停售 / 1 起售',
  `description`  varchar(255)           DEFAULT NULL   COMMENT '描述',
  `image`        varchar(255)           DEFAULT NULL   COMMENT '图片 URL，可为空',
  `storage_zone` tinyint       NOT NULL                COMMENT '整箱温区 1 常温 / 2 冷藏 / 3 冷冻（由内含商品最高温区推导）',
  `create_time`  datetime               DEFAULT NULL   COMMENT '创建时间',
  `update_time`  datetime               DEFAULT NULL   COMMENT '更新时间',
  `create_user`  bigint                 DEFAULT NULL   COMMENT '创建人',
  `update_user`  bigint                 DEFAULT NULL   COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_bundle_name` (`name`) USING BTREE,
  KEY `idx_bundle_category` (`category_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='组合箱';

-- ------------------------------------------------------------
-- 6. 组合箱内含商品（原 setmeal_dish）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `bundle_goods`;
CREATE TABLE `bundle_goods` (
  `id`        bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `bundle_id` bigint        NOT NULL                COMMENT '组合箱 id',
  `goods_id`  bigint        NOT NULL                COMMENT '商品 id',
  `name`      varchar(32)            DEFAULT NULL   COMMENT '商品名称（冗余）',
  `price`     decimal(10,2)          DEFAULT NULL   COMMENT '商品单价（冗余）',
  `copies`    int                    DEFAULT 1      COMMENT '份数',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_bg_bundle` (`bundle_id`) USING BTREE,
  KEY `idx_bg_goods` (`goods_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='组合箱内含商品';

-- ------------------------------------------------------------
-- 7. 会员（原 user）—— 删除 id_number（生鲜消费者不实名）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `member`;
CREATE TABLE `member` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `openid`      varchar(45)  NOT NULL                COMMENT '微信用户唯一标识（登录凭证）',
  `name`        varchar(32)           DEFAULT NULL   COMMENT '昵称',
  `phone`       varchar(11)           DEFAULT NULL   COMMENT '手机号',
  `sex`         varchar(2)            DEFAULT NULL   COMMENT '性别',
  `avatar`      varchar(500)          DEFAULT NULL   COMMENT '头像 URL',
  `create_time` datetime              DEFAULT NULL   COMMENT '注册时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_member_openid` (`openid`) USING BTREE,
  -- 会员报表按注册时间统计（累计人数走 range，按天分组仍需临时表）
  KEY `idx_member_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='会员';

-- ------------------------------------------------------------
-- 8. 收货地址（原 address_book）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `address`;
CREATE TABLE `address` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `member_id`     bigint       NOT NULL                COMMENT '会员 id',
  `consignee`     varchar(50)           DEFAULT NULL   COMMENT '收货人',
  `sex`           varchar(2)            DEFAULT NULL   COMMENT '性别',
  `phone`         varchar(11)  NOT NULL                COMMENT '手机号',
  `province_code` varchar(12)           DEFAULT NULL   COMMENT '省级区划编号',
  `province_name` varchar(32)           DEFAULT NULL   COMMENT '省级名称',
  `city_code`     varchar(12)           DEFAULT NULL   COMMENT '市级区划编号',
  `city_name`     varchar(32)           DEFAULT NULL   COMMENT '市级名称',
  `district_code` varchar(12)           DEFAULT NULL   COMMENT '区级区划编号',
  `district_name` varchar(32)           DEFAULT NULL   COMMENT '区级名称',
  `detail`        varchar(200)          DEFAULT NULL   COMMENT '详细地址',
  `label`         varchar(100)          DEFAULT NULL   COMMENT '标签，如「家」「公司」',
  `is_default`    tinyint      NOT NULL DEFAULT 0      COMMENT '是否默认 0 否 / 1 是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_address_member` (`member_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='收货地址';

-- ------------------------------------------------------------
-- 9. 购物车（原 shopping_cart）
--    同款判定：member_id + goods_id/bundle_id + spec_text
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
  `id`          bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        varchar(32)            DEFAULT NULL   COMMENT '商品 / 组合箱名称（快照）',
  `image`       varchar(255)           DEFAULT NULL   COMMENT '图片（快照）',
  `member_id`   bigint        NOT NULL                COMMENT '会员 id',
  `goods_id`    bigint                 DEFAULT NULL   COMMENT '商品 id',
  `bundle_id`   bigint                 DEFAULT NULL   COMMENT '组合箱 id',
  `spec_text`   varchar(100)           DEFAULT NULL   COMMENT '规格文本快照，如「净重:约500g」',
  `number`      int           NOT NULL DEFAULT 1      COMMENT '数量（由服务端 +1 / -1）',
  `amount`      decimal(10,2) NOT NULL                COMMENT '小计金额',
  `create_time` datetime               DEFAULT NULL   COMMENT '加入时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_cart_member` (`member_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='购物车';

-- ------------------------------------------------------------
-- 10. 主订单（原 orders）
--     删除 tableware_number / tableware_status；pack_amount → delivery_fee（int → decimal）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `order_main`;
CREATE TABLE `order_main` (
  `id`                      bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `number`                  varchar(50)   NOT NULL                COMMENT '订单号',
  `status`                  int           NOT NULL DEFAULT 1      COMMENT '订单状态 1 待付款 / 2 待接单 / 3 已接单 / 4 派送中 / 5 已完成 / 6 已取消',
  `member_id`               bigint        NOT NULL                COMMENT '下单会员 id',
  `address_id`              bigint        NOT NULL                COMMENT '收货地址 id',
  `order_time`              datetime      NOT NULL                COMMENT '下单时间',
  `checkout_time`           datetime               DEFAULT NULL   COMMENT '支付时间',
  `pay_method`              int           NOT NULL DEFAULT 1      COMMENT '支付方式 1 微信',
  `pay_status`              tinyint       NOT NULL DEFAULT 0      COMMENT '支付状态 0 未支付 / 1 已支付 / 2 已退款',
  `amount`                  decimal(10,2) NOT NULL                COMMENT '实收金额（商品金额 + 运送费）',
  `remark`                  varchar(100)           DEFAULT NULL   COMMENT '备注',
  `phone`                   varchar(11)            DEFAULT NULL   COMMENT '收货手机号（快照）',
  `address`                 varchar(255)           DEFAULT NULL   COMMENT '完整地址（快照）',
  `user_name`               varchar(32)            DEFAULT NULL   COMMENT '会员昵称（快照）',
  `consignee`               varchar(32)            DEFAULT NULL   COMMENT '收货人（快照）',
  `cancel_reason`           varchar(255)           DEFAULT NULL   COMMENT '取消原因',
  `rejection_reason`        varchar(255)           DEFAULT NULL   COMMENT '拒单原因',
  `cancel_time`             datetime               DEFAULT NULL   COMMENT '取消时间',
  `estimated_delivery_time` datetime               DEFAULT NULL   COMMENT '预计送达时间',
  `delivery_status`         tinyint       NOT NULL DEFAULT 1      COMMENT '配送时效 1 尽快送达 / 0 预约时段',
  `delivery_time`           datetime               DEFAULT NULL   COMMENT '送达时间',
  `delivery_fee`            decimal(10,2) NOT NULL DEFAULT 0.00   COMMENT '运送费（原 pack_amount 打包费）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_order_number` (`number`) USING BTREE,
  KEY `idx_order_member` (`member_id`) USING BTREE,
  KEY `idx_order_status` (`status`) USING BTREE,
  KEY `idx_order_time` (`order_time`) USING BTREE,
  KEY `idx_status_time` (`status`, `order_time`, `id`) USING BTREE,
  KEY `idx_member_time` (`member_id`, `order_time`, `id`) USING BTREE,
  -- 报表聚合专用覆盖索引：状态 + 时间区间过滤后直接取 amount 求和，不回表
  KEY `idx_report` (`status`, `order_time`, `amount`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='主订单';

-- ------------------------------------------------------------
-- 11. 订单明细（原 order_detail）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
  `id`        bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`      varchar(32)            DEFAULT NULL   COMMENT '名称（快照）',
  `image`     varchar(255)           DEFAULT NULL   COMMENT '图片（快照）',
  `order_id`  bigint        NOT NULL                COMMENT '所属订单 id',
  `goods_id`  bigint                 DEFAULT NULL   COMMENT '商品 id',
  `bundle_id` bigint                 DEFAULT NULL   COMMENT '组合箱 id',
  `spec_text` varchar(100)           DEFAULT NULL   COMMENT '规格文本快照',
  `number`    int           NOT NULL DEFAULT 1      COMMENT '数量',
  `amount`    decimal(10,2) NOT NULL                COMMENT '小计金额',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_item_order` (`order_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='订单明细';
