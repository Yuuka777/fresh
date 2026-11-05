-- ============================================================
--  鲜禾（xianhe-fresh）· 种子数据
--  由 gen-seed.mjs 生成；时间列用相对表达式，脚本可反复执行
--  金额已保证：order_main.amount = Σ order_item.amount + delivery_fee
--  注意：待付款订单（status = 1）的 order_time 为「刚刚」，
--        后端超时任务运行 15 分钟后会将其自动取消，属预期行为。
-- ============================================================

USE `xianhe_fresh`;

SET NAMES utf8mb4;

-- 清空（按依赖倒序，便于反复执行）
DELETE FROM `order_item`;
ALTER TABLE `order_item` AUTO_INCREMENT = 1;
DELETE FROM `order_main`;
ALTER TABLE `order_main` AUTO_INCREMENT = 1;
DELETE FROM `cart`;
ALTER TABLE `cart` AUTO_INCREMENT = 1;
DELETE FROM `address`;
ALTER TABLE `address` AUTO_INCREMENT = 1;
DELETE FROM `member`;
ALTER TABLE `member` AUTO_INCREMENT = 1;
DELETE FROM `bundle_goods`;
ALTER TABLE `bundle_goods` AUTO_INCREMENT = 1;
DELETE FROM `bundle`;
ALTER TABLE `bundle` AUTO_INCREMENT = 1;
DELETE FROM `goods_spec`;
ALTER TABLE `goods_spec` AUTO_INCREMENT = 1;
DELETE FROM `goods`;
ALTER TABLE `goods` AUTO_INCREMENT = 1;
DELETE FROM `goods_category`;
ALTER TABLE `goods_category` AUTO_INCREMENT = 1;
DELETE FROM `staff`;
ALTER TABLE `staff` AUTO_INCREMENT = 1;

-- ---------- 店员（密码统一为 123456 的 MD5） ----------
INSERT INTO `staff` (`name`, `username`, `password`, `phone`, `sex`, `id_number`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES
('沈青禾', 'admin', 'e10adc3949ba59abbe56e057f20f883e', '13800000001', '女', '110101199001011234', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 120 DAY), '09:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 120 DAY), '09:00:00'), 1, 1),
('陈续', 'chenxu', 'e10adc3949ba59abbe56e057f20f883e', '13800000002', '男', '110101199203056789', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 96 DAY), '09:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 96 DAY), '09:00:00'), 1, 1),
('林晚', 'linwan', 'e10adc3949ba59abbe56e057f20f883e', '13800000003', '女', '310101199507114321', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 74 DAY), '09:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 74 DAY), '09:00:00'), 1, 1),
('周砚', 'zhouyan', 'e10adc3949ba59abbe56e057f20f883e', '13800000004', '男', '440101199811200987', 0, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 40 DAY), '09:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 40 DAY), '09:00:00'), 1, 1);

-- ---------- 商品分类（type 1）与组合分类（type 2） ----------
INSERT INTO `goods_category` (`type`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES
(1, '时令蔬菜', 1, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 30 DAY), '08:30:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '08:30:00'), 1, 1),
(1, '当季水果', 2, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 30 DAY), '08:30:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '08:30:00'), 1, 1),
(1, '肉禽蛋品', 3, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 30 DAY), '08:30:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '08:30:00'), 1, 1),
(1, '鲜活水产', 4, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 30 DAY), '08:30:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '08:30:00'), 1, 1),
(1, '乳品烘焙', 5, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 30 DAY), '08:30:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '08:30:00'), 1, 1),
(1, '粮油调味', 6, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 30 DAY), '08:30:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '08:30:00'), 1, 1),
(1, '冷冻速食', 7, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 30 DAY), '08:30:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '08:30:00'), 1, 1),
(1, '酒水饮料', 8, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 30 DAY), '08:30:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '08:30:00'), 1, 1),
(2, '家庭组合', 1, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 30 DAY), '08:30:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '08:30:00'), 1, 1),
(2, '聚餐组合', 2, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 30 DAY), '08:30:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '08:30:00'), 1, 1),
(2, '轻食组合', 3, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 30 DAY), '08:30:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '08:30:00'), 1, 1);

-- ---------- 商品 ----------
INSERT INTO `goods` (`name`, `category_id`, `price`, `image`, `description`, `status`, `origin`, `shelf_life`, `storage_zone`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES
('有机小油菜', 1, 4.90, NULL, '山东寿光直采 · 全程冷藏', 1, '山东寿光', '冷藏 3 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('板栗南瓜', 1, 6.80, NULL, '海南三亚直采 · 产地直发', 1, '海南三亚', '常温 15 天', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('荷兰豆', 1, 9.90, NULL, '云南昆明直采 · 全程冷藏', 1, '云南昆明', '冷藏 4 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('紫皮洋葱', 1, 3.20, NULL, '甘肃张掖直采 · 产地直发', 1, '甘肃张掖', '常温 20 天', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('罗马生菜', 1, 5.60, NULL, '北京昌平直采 · 全程冷藏', 1, '北京昌平', '冷藏 3 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('水果黄瓜', 1, 7.50, NULL, '山东潍坊直采 · 全程冷藏', 1, '山东潍坊', '冷藏 5 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('香椿芽', 1, 12.80, NULL, '河南信阳直采 · 全程冷藏', 1, '河南信阳', '冷藏 2 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('贝贝南瓜', 1, 8.90, NULL, '广西南宁直采 · 产地直发', 1, '广西南宁', '常温 20 天', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('海南贵妃芒', 2, 9.90, NULL, '海南陵水直采 · 产地直发', 1, '海南陵水', '常温 5 天', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('阳光玫瑰葡萄', 2, 19.80, NULL, '陕西渭南直采 · 全程冷藏', 1, '陕西渭南', '冷藏 7 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('赣南脐橙', 2, 8.60, NULL, '江西赣州直采 · 产地直发', 1, '江西赣州', '常温 10 天', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('云南蓝莓', 2, 25.90, NULL, '云南红河直采 · 全程冷藏', 1, '云南红河', '冷藏 5 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('烟台红富士', 2, 6.90, NULL, '山东烟台直采 · 全程冷藏', 1, '山东烟台', '冷藏 15 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('广西沃柑', 2, 7.80, NULL, '广西桂林直采 · 产地直发', 1, '广西桂林', '常温 8 天', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('新疆西梅', 2, 22.80, NULL, '新疆喀什直采 · 全程冷藏', 1, '新疆喀什', '冷藏 6 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('现杀土鸡腿', 3, 19.90, NULL, '安徽宣城直采 · 全程冷藏', 1, '安徽宣城', '冷藏 2 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('黑猪五花肉', 3, 32.80, NULL, '江西南昌直采 · 全程冷藏', 1, '江西南昌', '冷藏 3 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('冷鲜牛里脊', 3, 68.00, NULL, '内蒙锡林郭勒直采 · 全程冷藏', 1, '内蒙锡林郭勒', '冷藏 3 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('散养土鸡蛋', 3, 29.90, NULL, '湖北神农架直采 · 产地直发', 1, '湖北神农架', '常温 15 天', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('冷鲜鸭胸', 3, 16.50, NULL, '江苏高邮直采 · 全程冷藏', 1, '江苏高邮', '冷藏 3 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('黑猪排骨段', 3, 45.80, NULL, '江西南昌直采 · 全程冷藏', 1, '江西南昌', '冷藏 2 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('活鲜基围虾', 4, 68.00, NULL, '广东湛江直采 · 全程冷藏', 1, '广东湛江', '冷藏 1 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('三文鱼刺身块', 4, 78.00, NULL, '挪威进口直采 · 全程冷藏', 1, '挪威进口', '冷藏 2 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('舟山小黄鱼', 4, 32.50, NULL, '浙江舟山直采 · 全程冷藏', 1, '浙江舟山', '冷藏 1 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('大连扇贝', 4, 25.00, NULL, '辽宁大连直采 · 全程冷藏', 1, '辽宁大连', '冷藏 2 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('冰鲜海鲈鱼', 4, 28.80, NULL, '福建宁德直采 · 全程冷藏', 1, '福建宁德', '冷藏 1 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('巴氏鲜牛奶', 5, 16.80, NULL, '河北石家庄直采 · 全程冷藏', 1, '河北石家庄', '冷藏 7 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('现烤北海道吐司', 5, 12.90, NULL, '门店现烤直采 · 产地直发', 1, '门店现烤', '常温 3 天', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('手工原味酸奶', 5, 9.90, NULL, '北京顺义直采 · 全程冷藏', 1, '北京顺义', '冷藏 5 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('法式可颂', 5, 15.80, NULL, '门店现烤直采 · 产地直发', 1, '门店现烤', '常温 2 天', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('舒芙蕾乳酪', 5, 28.00, NULL, '门店现烤直采 · 全程冷藏', 1, '门店现烤', '冷藏 3 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('淡奶油蛋糕卷', 5, 22.00, NULL, '门店现烤直采 · 全程冷藏', 1, '门店现烤', '冷藏 2 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('五常稻花香米', 6, 59.90, NULL, '黑龙江五常直采 · 产地直发', 1, '黑龙江五常', '常温 12 个月', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('特级初榨橄榄油', 6, 68.00, NULL, '西班牙进口直采 · 产地直发', 1, '西班牙进口', '常温 18 个月', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('现磨黑胡椒粒', 6, 12.90, NULL, '广西玉林直采 · 产地直发', 1, '广西玉林', '常温 12 个月', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('古法酿造生抽', 6, 15.80, NULL, '广东佛山直采 · 产地直发', 1, '广东佛山', '常温 18 个月', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('高筋面粉', 6, 9.90, NULL, '河南新乡直采 · 产地直发', 1, '河南新乡', '常温 12 个月', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('冷榨亚麻籽油', 6, 45.00, NULL, '宁夏固原直采 · 产地直发', 1, '宁夏固原', '常温 12 个月', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('手工三鲜水饺', 7, 22.80, NULL, '山东青岛直采 · 全程冷冻', 1, '山东青岛', '冷冻 90 天', 3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('潮汕手打牛肉丸', 7, 32.00, NULL, '广东潮州直采 · 全程冷冻', 1, '广东潮州', '冷冻 180 天', 3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('冷冻甜玉米粒', 7, 8.90, NULL, '吉林长春直采 · 全程冷冻', 1, '吉林长春', '冷冻 365 天', 3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('速冻小笼包', 7, 18.80, NULL, '上海南翔直采 · 全程冷冻', 1, '上海南翔', '冷冻 180 天', 3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('冷冻虾滑', 7, 25.90, NULL, '广东湛江直采 · 全程冷冻', 1, '广东湛江', '冷冻 180 天', 3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('冷冻豌豆荚', 7, 7.90, NULL, '云南保山直采 · 全程冷冻', 1, '云南保山', '冷冻 365 天', 3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('无糖气泡水', 8, 5.50, NULL, '江苏无锡直采 · 产地直发', 1, '江苏无锡', '常温 12 个月', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('冷萃咖啡液', 8, 19.90, NULL, '云南普洱直采 · 全程冷藏', 1, '云南普洱', '冷藏 15 天', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('精酿小麦啤酒', 8, 12.80, NULL, '山东青岛直采 · 产地直发', 1, '山东青岛', '常温 6 个月', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('泰国椰子水', 8, 15.90, NULL, '泰国进口直采 · 产地直发', 1, '泰国进口', '常温 12 个月', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1),
('薏米红豆水', 8, 6.50, NULL, '广东肇庆直采 · 产地直发', 1, '广东肇庆', '常温 9 个月', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), 1, 1);

-- ---------- 商品规格（value 为候选值数组的 JSON 文本） ----------
INSERT INTO `goods_spec` (`goods_id`, `name`, `value`) VALUES
(1, '净重', '["约300g","约500g"]'),
(2, '净重', '["约1kg","约2kg"]'),
(3, '净重', '["约300g"]'),
(4, '净重', '["约500g","约1kg"]'),
(5, '净重', '["约300g"]'),
(6, '净重', '["约500g"]'),
(7, '净重', '["约100g"]'),
(8, '净重', '["约1kg"]'),
(9, '净重', '["约1kg","约2kg"]'),
(10, '净重', '["约500g","约1kg"]'),
(11, '净重', '["约2kg"]'),
(12, '净重', '["125g×2盒"]'),
(13, '净重', '["约1kg","约2kg"]'),
(14, '净重', '["约2kg"]'),
(15, '净重', '["约500g"]'),
(16, '加工方式', '["约500g"]'),
(17, '加工方式', '["约500g","约1kg"]'),
(18, '加工方式', '["约400g"]'),
(19, '加工方式', '["30枚"]'),
(20, '加工方式', '["约500g"]'),
(21, '加工方式', '["约500g"]'),
(22, '加工方式', '["约500g"]'),
(23, '加工方式', '["约300g"]'),
(24, '加工方式', '["约500g","约1kg"]'),
(25, '加工方式', '["约500g"]'),
(26, '加工方式', '["约600g"]'),
(27, '规格', '["950ml"]'),
(28, '规格', '["450g"]'),
(29, '规格', '["200g×4"]'),
(30, '规格', '["4只装"]'),
(31, '规格', '["1个"]'),
(32, '规格', '["1条"]'),
(39, '规格', '["500g","1kg"]'),
(40, '规格', '["250g"]'),
(41, '规格', '["500g"]'),
(42, '规格', '["8只装"]'),
(43, '规格', '["150g×2"]'),
(44, '规格', '["500g"]');

-- ---------- 组合箱（storage_zone 由内含商品最高温区推导） ----------
INSERT INTO `bundle` (`category_id`, `name`, `price`, `status`, `description`, `image`, `storage_zone`, `create_time`, `update_time`, `create_user`, `update_user`) VALUES
(9, '一周蔬菜箱', 49.90, 1, '5 种商品按温区配齐', NULL, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 20 DAY), '14:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '14:00:00'), 1, 1),
(10, '火锅食材组合', 128.00, 1, '5 种商品按温区配齐', NULL, 3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 20 DAY), '14:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '14:00:00'), 1, 1),
(11, '轻食沙拉包', 39.90, 1, '4 种商品按温区配齐', NULL, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 20 DAY), '14:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '14:00:00'), 1, 1),
(9, '家庭早餐组合', 68.00, 1, '4 种商品按温区配齐', NULL, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 20 DAY), '14:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '14:00:00'), 1, 1),
(10, '海鲜小聚拼盘', 158.00, 1, '4 种商品按温区配齐', NULL, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 20 DAY), '14:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '14:00:00'), 1, 1);

-- ---------- 组合箱内含商品（name / price 为冗余快照） ----------
INSERT INTO `bundle_goods` (`bundle_id`, `goods_id`, `name`, `price`, `copies`) VALUES
(1, 1, '有机小油菜', 4.90, 1),
(1, 3, '荷兰豆', 9.90, 1),
(1, 5, '罗马生菜', 5.60, 1),
(1, 6, '水果黄瓜', 7.50, 1),
(1, 4, '紫皮洋葱', 3.20, 1),
(2, 17, '黑猪五花肉', 32.80, 1),
(2, 18, '冷鲜牛里脊', 68.00, 1),
(2, 40, '潮汕手打牛肉丸', 32.00, 1),
(2, 41, '冷冻甜玉米粒', 8.90, 1),
(2, 43, '冷冻虾滑', 25.90, 1),
(3, 5, '罗马生菜', 5.60, 1),
(3, 6, '水果黄瓜', 7.50, 1),
(3, 26, '冰鲜海鲈鱼', 28.80, 1),
(3, 7, '香椿芽', 12.80, 1),
(4, 27, '巴氏鲜牛奶', 16.80, 1),
(4, 30, '法式可颂', 15.80, 1),
(4, 33, '五常稻花香米', 59.90, 1),
(4, 19, '散养土鸡蛋', 29.90, 1),
(5, 22, '活鲜基围虾', 68.00, 1),
(5, 23, '三文鱼刺身块', 78.00, 1),
(5, 25, '大连扇贝', 25.00, 1),
(5, 24, '舟山小黄鱼', 32.50, 1);

-- ---------- 会员 ----------
INSERT INTO `member` (`openid`, `name`, `phone`, `sex`, `avatar`, `create_time`) VALUES
('oXh7Kd9mQp2RsT4vW6yZ1aB3cD5e', '李沐', '13910001001', '女', NULL, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 88 DAY), '11:00:00')),
('oXh7Kd9mQp2RsT4vW6yZ1aB3cD5f', '陈嘉树', '13910001002', '男', NULL, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 71 DAY), '11:00:00')),
('oXh7Kd9mQp2RsT4vW6yZ1aB3cD5g', '苏晚晴', '13910001003', '女', NULL, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 55 DAY), '11:00:00')),
('oXh7Kd9mQp2RsT4vW6yZ1aB3cD5h', '郑淮', '13910001004', '男', NULL, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 39 DAY), '11:00:00')),
('oXh7Kd9mQp2RsT4vW6yZ1aB3cD5i', '何一诺', '13910001005', '女', NULL, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 26 DAY), '11:00:00')),
('oXh7Kd9mQp2RsT4vW6yZ1aB3cD5j', '徐砚青', '13910001006', '男', NULL, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 17 DAY), '11:00:00')),
('oXh7Kd9mQp2RsT4vW6yZ1aB3cD5k', '秦朗', '13910001007', '男', NULL, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 9 DAY), '11:00:00')),
('oXh7Kd9mQp2RsT4vW6yZ1aB3cD5m', '温言', '13910001008', '女', NULL, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '11:00:00'));

-- ---------- 收货地址 ----------
INSERT INTO `address` (`member_id`, `consignee`, `sex`, `phone`, `province_code`, `province_name`, `city_code`, `city_name`, `district_code`, `district_name`, `detail`, `label`, `is_default`) VALUES
(1, '李沐', '女', '13910001001', '110000', '北京市', '110100', '市辖区', '110105', '朝阳区', '望京西园四区 402 号楼 2 单元 1203', '家', 1),
(2, '陈嘉树', '男', '13910001002', '110000', '北京市', '110100', '市辖区', '110108', '海淀区', '中关村南大街 12 号院 3 号楼 501', '家', 1),
(3, '苏晚晴', '女', '13910001003', '110000', '北京市', '110100', '市辖区', '110105', '朝阳区', '建国路 88 号 SOHO 现代城 B 座 1806', '公司', 1),
(4, '郑淮', '男', '13910001004', '310000', '上海市', '310100', '市辖区', '310115', '浦东新区', '张杨路 500 号 8 号楼 902', '家', 1),
(5, '何一诺', '女', '13910001005', '110000', '北京市', '110100', '市辖区', '110102', '西城区', '金融街 33 号通泰大厦 A 座 1508', '公司', 1),
(6, '徐砚青', '男', '13910001006', '110000', '北京市', '110100', '市辖区', '110105', '朝阳区', '东三环中路 39 号建外 SOHO 西区 16 号楼 2103', '家', 1),
(7, '秦朗', '男', '13910001007', '110000', '北京市', '110100', '市辖区', '110106', '丰台区', '方庄芳城园一区 6 号楼 703', '家', 1),
(8, '温言', '女', '13910001008', '110000', '北京市', '110100', '市辖区', '110108', '海淀区', '学院路 30 号科大天工大厦 B 座 1105', '公司', 1);

-- ---------- 购物车（1 号会员预置，便于演示确认订单） ----------
INSERT INTO `cart` (`name`, `image`, `member_id`, `goods_id`, `bundle_id`, `spec_text`, `number`, `amount`, `create_time`) VALUES
('有机小油菜', NULL, 1, 1, NULL, '净重:约500g', 2, 9.80, NOW() - INTERVAL 20 MINUTE),
('冰鲜海鲈鱼', NULL, 1, 26, NULL, NULL, 1, 28.80, NOW() - INTERVAL 20 MINUTE),
('一周蔬菜箱', NULL, 1, NULL, 1, NULL, 1, 49.90, NOW() - INTERVAL 20 MINUTE);

-- ---------- 主订单（覆盖 1~6 全部状态） ----------
INSERT INTO `order_main` (`number`, `status`, `member_id`, `address_id`, `order_time`, `checkout_time`, `pay_method`, `pay_status`, `amount`, `remark`, `phone`, `address`, `user_name`, `consignee`, `cancel_reason`, `rejection_reason`, `cancel_time`, `estimated_delivery_time`, `delivery_status`, `delivery_time`, `delivery_fee`) VALUES
(CONCAT('XH', DATE_FORMAT(NOW() - INTERVAL 2 MINUTE, '%Y%m%d%H%i%s'), '1001'), 1, 1, 1, NOW() - INTERVAL 2 MINUTE, NULL, 1, 0, 40.70, '不要辣', '13910001001', '北京市市辖区朝阳区望京西园四区 402 号楼 2 单元 1203', '李沐', '李沐', NULL, NULL, NULL, NULL, 1, NULL, 5.00),
(CONCAT('XH', DATE_FORMAT(NOW() - INTERVAL 9 MINUTE, '%Y%m%d%H%i%s'), '1002'), 1, 3, 3, NOW() - INTERVAL 9 MINUTE, NULL, 1, 0, 41.60, NULL, '13910001003', '北京市市辖区朝阳区建国路 88 号 SOHO 现代城 B 座 1806', '苏晚晴', '苏晚晴', NULL, NULL, NULL, NULL, 1, NULL, 5.00),
(CONCAT('XH', DATE_FORMAT(NOW() - INTERVAL 26 MINUTE, '%Y%m%d%H%i%s'), '1003'), 2, 2, 2, NOW() - INTERVAL 26 MINUTE, (NOW() - INTERVAL 26 MINUTE) + INTERVAL 3 MINUTE, 1, 1, 77.50, '请电话联系', '13910001002', '北京市市辖区海淀区中关村南大街 12 号院 3 号楼 501', '陈嘉树', '陈嘉树', NULL, NULL, NULL, (NOW() - INTERVAL 26 MINUTE) + INTERVAL 35 MINUTE, 1, NULL, 5.00),
(CONCAT('XH', DATE_FORMAT(NOW() - INTERVAL 44 MINUTE, '%Y%m%d%H%i%s'), '1004'), 2, 5, 5, NOW() - INTERVAL 44 MINUTE, (NOW() - INTERVAL 44 MINUTE) + INTERVAL 3 MINUTE, 1, 1, 172.80, NULL, '13910001005', '北京市市辖区西城区金融街 33 号通泰大厦 A 座 1508', '何一诺', '何一诺', NULL, NULL, NULL, (NOW() - INTERVAL 44 MINUTE) + INTERVAL 35 MINUTE, 1, NULL, 5.00),
(CONCAT('XH', DATE_FORMAT(NOW() - INTERVAL 58 MINUTE, '%Y%m%d%H%i%s'), '1005'), 2, 6, 6, NOW() - INTERVAL 58 MINUTE, (NOW() - INTERVAL 58 MINUTE) + INTERVAL 3 MINUTE, 1, 1, 134.30, NULL, '13910001006', '北京市市辖区朝阳区东三环中路 39 号建外 SOHO 西区 16 号楼 2103', '徐砚青', '徐砚青', NULL, NULL, NULL, (NOW() - INTERVAL 58 MINUTE) + INTERVAL 35 MINUTE, 1, NULL, 5.00),
(CONCAT('XH', DATE_FORMAT(NOW() - INTERVAL 3 HOUR, '%Y%m%d%H%i%s'), '1006'), 3, 4, 4, NOW() - INTERVAL 3 HOUR, (NOW() - INTERVAL 3 HOUR) + INTERVAL 3 MINUTE, 1, 1, 192.80, NULL, '13910001004', '上海市市辖区浦东新区张杨路 500 号 8 号楼 902', '郑淮', '郑淮', NULL, NULL, NULL, (NOW() - INTERVAL 3 HOUR) + INTERVAL 35 MINUTE, 1, NULL, 5.00),
(CONCAT('XH', DATE_FORMAT(NOW() - INTERVAL 5 HOUR, '%Y%m%d%H%i%s'), '1007'), 3, 7, 7, NOW() - INTERVAL 5 HOUR, (NOW() - INTERVAL 5 HOUR) + INTERVAL 3 MINUTE, 1, 1, 54.90, NULL, '13910001007', '北京市市辖区丰台区方庄芳城园一区 6 号楼 703', '秦朗', '秦朗', NULL, NULL, NULL, (NOW() - INTERVAL 5 HOUR) + INTERVAL 35 MINUTE, 1, NULL, 5.00),
(CONCAT('XH', DATE_FORMAT(NOW() - INTERVAL 7 HOUR, '%Y%m%d%H%i%s'), '1008'), 4, 1, 1, NOW() - INTERVAL 7 HOUR, (NOW() - INTERVAL 7 HOUR) + INTERVAL 3 MINUTE, 1, 1, 41.60, NULL, '13910001001', '北京市市辖区朝阳区望京西园四区 402 号楼 2 单元 1203', '李沐', '李沐', NULL, NULL, NULL, (NOW() - INTERVAL 7 HOUR) + INTERVAL 35 MINUTE, 1, NULL, 5.00),
(CONCAT('XH', DATE_FORMAT(NOW() - INTERVAL 9 HOUR, '%Y%m%d%H%i%s'), '1009'), 4, 8, 8, NOW() - INTERVAL 9 HOUR, (NOW() - INTERVAL 9 HOUR) + INTERVAL 3 MINUTE, 1, 1, 77.90, NULL, '13910001008', '北京市市辖区海淀区学院路 30 号科大天工大厦 B 座 1105', '温言', '温言', NULL, NULL, NULL, (NOW() - INTERVAL 9 HOUR) + INTERVAL 35 MINUTE, 1, NULL, 5.00),
(CONCAT('XH', DATE_FORMAT(TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '19:30:00'), '%Y%m%d%H%i%s'), '1010'), 5, 2, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '19:30:00'), (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '19:30:00')) + INTERVAL 3 MINUTE, 1, 1, 33.30, NULL, '13910001002', '北京市市辖区海淀区中关村南大街 12 号院 3 号楼 501', '陈嘉树', '陈嘉树', NULL, NULL, NULL, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '19:30:00')) + INTERVAL 35 MINUTE, 1, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '19:30:00')) + INTERVAL 32 MINUTE, 5.00),
(CONCAT('XH', DATE_FORMAT(TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '12:10:00'), '%Y%m%d%H%i%s'), '1011'), 5, 3, 3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '12:10:00'), (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '12:10:00')) + INTERVAL 3 MINUTE, 1, 1, 100.90, '放门口就行', '13910001003', '北京市市辖区朝阳区建国路 88 号 SOHO 现代城 B 座 1806', '苏晚晴', '苏晚晴', NULL, NULL, NULL, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '12:10:00')) + INTERVAL 35 MINUTE, 1, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '12:10:00')) + INTERVAL 32 MINUTE, 5.00),
(CONCAT('XH', DATE_FORMAT(TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '20:05:00'), '%Y%m%d%H%i%s'), '1012'), 5, 4, 4, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '20:05:00'), (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '20:05:00')) + INTERVAL 3 MINUTE, 1, 1, 190.40, NULL, '13910001004', '上海市市辖区浦东新区张杨路 500 号 8 号楼 902', '郑淮', '郑淮', NULL, NULL, NULL, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '20:05:00')) + INTERVAL 35 MINUTE, 1, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '20:05:00')) + INTERVAL 32 MINUTE, 5.00),
(CONCAT('XH', DATE_FORMAT(TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '11:20:00'), '%Y%m%d%H%i%s'), '1013'), 5, 5, 5, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '11:20:00'), (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '11:20:00')) + INTERVAL 3 MINUTE, 1, 1, 51.50, '有老人，轻敲门', '13910001005', '北京市市辖区西城区金融街 33 号通泰大厦 A 座 1508', '何一诺', '何一诺', NULL, NULL, NULL, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '11:20:00')) + INTERVAL 35 MINUTE, 1, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '11:20:00')) + INTERVAL 32 MINUTE, 5.00),
(CONCAT('XH', DATE_FORMAT(TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '18:40:00'), '%Y%m%d%H%i%s'), '1014'), 5, 6, 6, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '18:40:00'), (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '18:40:00')) + INTERVAL 3 MINUTE, 1, 1, 151.00, NULL, '13910001006', '北京市市辖区朝阳区东三环中路 39 号建外 SOHO 西区 16 号楼 2103', '徐砚青', '徐砚青', NULL, NULL, NULL, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '18:40:00')) + INTERVAL 35 MINUTE, 1, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '18:40:00')) + INTERVAL 32 MINUTE, 5.00),
(CONCAT('XH', DATE_FORMAT(TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:15:00'), '%Y%m%d%H%i%s'), '1015'), 5, 1, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:15:00'), (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:15:00')) + INTERVAL 3 MINUTE, 1, 1, 26.10, NULL, '13910001001', '北京市市辖区朝阳区望京西园四区 402 号楼 2 单元 1203', '李沐', '李沐', NULL, NULL, NULL, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:15:00')) + INTERVAL 35 MINUTE, 1, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:15:00')) + INTERVAL 32 MINUTE, 5.00),
(CONCAT('XH', DATE_FORMAT(TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '21:00:00'), '%Y%m%d%H%i%s'), '1016'), 5, 7, 7, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '21:00:00'), (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '21:00:00')) + INTERVAL 3 MINUTE, 1, 1, 231.00, '周末聚餐', '13910001007', '北京市市辖区丰台区方庄芳城园一区 6 号楼 703', '秦朗', '秦朗', NULL, NULL, NULL, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '21:00:00')) + INTERVAL 35 MINUTE, 1, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '21:00:00')) + INTERVAL 32 MINUTE, 5.00),
(CONCAT('XH', DATE_FORMAT(TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:30:00'), '%Y%m%d%H%i%s'), '1017'), 5, 8, 8, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:30:00'), (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:30:00')) + INTERVAL 3 MINUTE, 1, 1, 71.50, NULL, '13910001008', '北京市市辖区海淀区学院路 30 号科大天工大厦 B 座 1105', '温言', '温言', NULL, NULL, NULL, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:30:00')) + INTERVAL 35 MINUTE, 1, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '09:30:00')) + INTERVAL 32 MINUTE, 5.00),
(CONCAT('XH', DATE_FORMAT(TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '17:45:00'), '%Y%m%d%H%i%s'), '1018'), 5, 2, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '17:45:00'), (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '17:45:00')) + INTERVAL 3 MINUTE, 1, 1, 28.80, NULL, '13910001002', '北京市市辖区海淀区中关村南大街 12 号院 3 号楼 501', '陈嘉树', '陈嘉树', NULL, NULL, NULL, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '17:45:00')) + INTERVAL 35 MINUTE, 1, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '17:45:00')) + INTERVAL 32 MINUTE, 5.00),
(CONCAT('XH', DATE_FORMAT(TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 8 DAY), '13:05:00'), '%Y%m%d%H%i%s'), '1019'), 5, 3, 3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 8 DAY), '13:05:00'), (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 8 DAY), '13:05:00')) + INTERVAL 3 MINUTE, 1, 1, 93.60, NULL, '13910001003', '北京市市辖区朝阳区建国路 88 号 SOHO 现代城 B 座 1806', '苏晚晴', '苏晚晴', NULL, NULL, NULL, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 8 DAY), '13:05:00')) + INTERVAL 35 MINUTE, 1, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 8 DAY), '13:05:00')) + INTERVAL 32 MINUTE, 5.00),
(CONCAT('XH', DATE_FORMAT(TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 9 DAY), '19:15:00'), '%Y%m%d%H%i%s'), '1020'), 5, 4, 4, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 9 DAY), '19:15:00'), (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 9 DAY), '19:15:00')) + INTERVAL 3 MINUTE, 1, 1, 81.30, NULL, '13910001004', '上海市市辖区浦东新区张杨路 500 号 8 号楼 902', '郑淮', '郑淮', NULL, NULL, NULL, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 9 DAY), '19:15:00')) + INTERVAL 35 MINUTE, 1, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 9 DAY), '19:15:00')) + INTERVAL 32 MINUTE, 5.00),
(CONCAT('XH', DATE_FORMAT(TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 11 DAY), '12:30:00'), '%Y%m%d%H%i%s'), '1021'), 5, 5, 5, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 11 DAY), '12:30:00'), (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 11 DAY), '12:30:00')) + INTERVAL 3 MINUTE, 1, 1, 31.20, '不要辣', '13910001005', '北京市市辖区西城区金融街 33 号通泰大厦 A 座 1508', '何一诺', '何一诺', NULL, NULL, NULL, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 11 DAY), '12:30:00')) + INTERVAL 35 MINUTE, 1, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 11 DAY), '12:30:00')) + INTERVAL 32 MINUTE, 5.00),
(CONCAT('XH', DATE_FORMAT(TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 13 DAY), '16:20:00'), '%Y%m%d%H%i%s'), '1022'), 5, 6, 6, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 13 DAY), '16:20:00'), (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 13 DAY), '16:20:00')) + INTERVAL 3 MINUTE, 1, 1, 104.80, NULL, '13910001006', '北京市市辖区朝阳区东三环中路 39 号建外 SOHO 西区 16 号楼 2103', '徐砚青', '徐砚青', NULL, NULL, NULL, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 13 DAY), '16:20:00')) + INTERVAL 35 MINUTE, 1, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 13 DAY), '16:20:00')) + INTERVAL 32 MINUTE, 5.00),
(CONCAT('XH', DATE_FORMAT(TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '22:40:00'), '%Y%m%d%H%i%s'), '1023'), 6, 8, 8, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '22:40:00'), NULL, 1, 0, 20.90, NULL, '13910001008', '北京市市辖区海淀区学院路 30 号科大天工大厦 B 座 1105', '温言', '温言', '订单超时未支付，系统自动取消', NULL, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '22:40:00')) + INTERVAL 15 MINUTE, NULL, 1, NULL, 5.00),
(CONCAT('XH', DATE_FORMAT(TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '14:00:00'), '%Y%m%d%H%i%s'), '1024'), 6, 7, 7, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '14:00:00'), (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '14:00:00')) + INTERVAL 3 MINUTE, 1, 2, 83.00, NULL, '13910001007', '北京市市辖区丰台区方庄芳城园一区 6 号楼 703', '秦朗', '秦朗', NULL, '该商品已售完，建议改选同温区替代品', (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '14:00:00')) + INTERVAL 15 MINUTE, NULL, 1, NULL, 5.00),
(CONCAT('XH', DATE_FORMAT(TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '15:25:00'), '%Y%m%d%H%i%s'), '1025'), 6, 3, 3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '15:25:00'), (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '15:25:00')) + INTERVAL 3 MINUTE, 1, 2, 32.70, NULL, '13910001003', '北京市市辖区朝阳区建国路 88 号 SOHO 现代城 B 座 1806', '苏晚晴', '苏晚晴', '用户取消', NULL, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '15:25:00')) + INTERVAL 15 MINUTE, NULL, 1, NULL, 5.00),
(CONCAT('XH', DATE_FORMAT(TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '20:10:00'), '%Y%m%d%H%i%s'), '1026'), 6, 1, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '20:10:00'), (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '20:10:00')) + INTERVAL 3 MINUTE, 1, 2, 14.90, NULL, '13910001001', '北京市市辖区朝阳区望京西园四区 402 号楼 2 单元 1203', '李沐', '李沐', '下错地址', NULL, (TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '20:10:00')) + INTERVAL 15 MINUTE, NULL, 1, NULL, 5.00);

-- ---------- 订单明细 ----------
INSERT INTO `order_item` (`name`, `image`, `order_id`, `goods_id`, `bundle_id`, `spec_text`, `number`, `amount`) VALUES
('有机小油菜', NULL, 1, 1, NULL, '净重:约300g', 2, 9.80),
('云南蓝莓', NULL, 1, 12, NULL, '净重:125g×2盒', 1, 25.90),
('巴氏鲜牛奶', NULL, 2, 27, NULL, '规格:950ml', 1, 16.80),
('手工原味酸奶', NULL, 2, 29, NULL, '规格:200g×4', 2, 19.80),
('黑猪五花肉', NULL, 3, 17, NULL, '加工方式:约500g', 1, 32.80),
('散养土鸡蛋', NULL, 3, 19, NULL, '加工方式:30枚', 1, 29.90),
('有机小油菜', NULL, 3, 1, NULL, '净重:约300g', 2, 9.80),
('火锅食材组合', NULL, 4, NULL, 2, NULL, 1, 128.00),
('冷萃咖啡液', NULL, 4, 46, NULL, NULL, 2, 39.80),
('活鲜基围虾', NULL, 5, 22, NULL, '加工方式:约500g', 1, 68.00),
('舟山小黄鱼', NULL, 5, 24, NULL, '加工方式:约500g', 1, 32.50),
('冰鲜海鲈鱼', NULL, 5, 26, NULL, '加工方式:约600g', 1, 28.80),
('五常稻花香米', NULL, 6, 33, NULL, NULL, 2, 119.80),
('特级初榨橄榄油', NULL, 6, 34, NULL, NULL, 1, 68.00),
('一周蔬菜箱', NULL, 7, NULL, 1, NULL, 1, 49.90),
('海南贵妃芒', NULL, 8, 9, NULL, '净重:约1kg', 1, 9.90),
('阳光玫瑰葡萄', NULL, 8, 10, NULL, '净重:约500g', 1, 19.80),
('烟台红富士', NULL, 8, 13, NULL, '净重:约1kg', 1, 6.90),
('潮汕手打牛肉丸', NULL, 9, 40, NULL, '规格:250g', 2, 64.00),
('冷冻甜玉米粒', NULL, 9, 41, NULL, '规格:500g', 1, 8.90),
('荷兰豆', NULL, 10, 3, NULL, '净重:约300g', 1, 9.90),
('罗马生菜', NULL, 10, 5, NULL, '净重:约300g', 1, 5.60),
('香椿芽', NULL, 10, 7, NULL, '净重:约100g', 1, 12.80),
('轻食沙拉包', NULL, 11, NULL, 3, NULL, 1, 39.90),
('舒芙蕾乳酪', NULL, 11, 31, NULL, '规格:1个', 2, 56.00),
('黑猪五花肉', NULL, 12, 17, NULL, '加工方式:约500g', 2, 65.60),
('冷鲜牛里脊', NULL, 12, 18, NULL, '加工方式:约400g', 1, 68.00),
('冷冻虾滑', NULL, 12, 43, NULL, '规格:150g×2', 2, 51.80),
('巴氏鲜牛奶', NULL, 13, 27, NULL, '规格:950ml', 2, 33.60),
('现烤北海道吐司', NULL, 13, 28, NULL, '规格:450g', 1, 12.90),
('活鲜基围虾', NULL, 14, 22, NULL, '加工方式:约500g', 1, 68.00),
('三文鱼刺身块', NULL, 14, 23, NULL, '加工方式:约300g', 1, 78.00),
('有机小油菜', NULL, 15, 1, NULL, '净重:约300g', 3, 14.70),
('紫皮洋葱', NULL, 15, 4, NULL, '净重:约500g', 2, 6.40),
('家庭早餐组合', NULL, 16, NULL, 4, NULL, 1, 68.00),
('海鲜小聚拼盘', NULL, 16, NULL, 5, NULL, 1, 158.00),
('云南蓝莓', NULL, 17, 12, NULL, '净重:125g×2盒', 2, 51.80),
('烟台红富士', NULL, 17, 13, NULL, '净重:约1kg', 1, 6.90),
('广西沃柑', NULL, 17, 14, NULL, '净重:约2kg', 1, 7.80),
('无糖气泡水', NULL, 18, 45, NULL, NULL, 2, 11.00),
('精酿小麦啤酒', NULL, 18, 47, NULL, NULL, 1, 12.80),
('五常稻花香米', NULL, 19, 33, NULL, NULL, 1, 59.90),
('现磨黑胡椒粒', NULL, 19, 35, NULL, NULL, 1, 12.90),
('古法酿造生抽', NULL, 19, 36, NULL, NULL, 1, 15.80),
('散养土鸡蛋', NULL, 20, 19, NULL, '加工方式:30枚', 2, 59.80),
('冷鲜鸭胸', NULL, 20, 20, NULL, '加工方式:约500g', 1, 16.50),
('罗马生菜', NULL, 21, 5, NULL, '净重:约300g', 2, 11.20),
('水果黄瓜', NULL, 21, 6, NULL, '净重:约500g', 2, 15.00),
('一周蔬菜箱', NULL, 22, NULL, 1, NULL, 2, 99.80),
('泰国椰子水', NULL, 23, 48, NULL, NULL, 1, 15.90),
('三文鱼刺身块', NULL, 24, 23, NULL, '加工方式:约300g', 1, 78.00),
('冷冻甜玉米粒', NULL, 25, 41, NULL, '规格:500g', 1, 8.90),
('速冻小笼包', NULL, 25, 42, NULL, '规格:8只装', 1, 18.80),
('海南贵妃芒', NULL, 26, 9, NULL, '净重:约1kg', 1, 9.90);

-- ---------- 导入后自检 ----------
SELECT o.status AS 状态, COUNT(*) AS 单数, SUM(o.amount) AS 金额合计
  FROM order_main o GROUP BY o.status ORDER BY o.status;
SELECT COUNT(*) AS 金额不一致订单数 FROM (
  SELECT o.id FROM order_main o JOIN order_item i ON i.order_id = o.id
  GROUP BY o.id, o.amount, o.delivery_fee
  HAVING o.amount <> SUM(i.amount) + o.delivery_fee
) t;
