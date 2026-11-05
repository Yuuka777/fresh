# 数据库脚本 · 鲜禾（xianhe_fresh）

| 文件 | 内容 |
|---|---|
| `01-schema.sql` | 建库 + 11 张表（含索引与注释）。**可重复执行**（`DROP TABLE IF EXISTS` 后重建） |
| `02-data.sql` | 种子数据。**可重复执行**（先按依赖倒序 `DELETE` 并重置自增）。时间列用相对表达式，重新导入即为“新鲜”数据 |

## 导入

```bash
# 必须带 --default-character-set=utf8mb4
# 否则 Windows 下客户端默认 gbk，会把脚本里的中文按 gbk 解码后入库，造成双重编码乱码
mysql -uroot -p --default-character-set=utf8mb4 < 01-schema.sql
mysql -uroot -p --default-character-set=utf8mb4 < 02-data.sql
```

本机实际路径：`mysql` 客户端在 `D:\tool\mysql\bin\mysql.exe`，MySQL 8.4.11，`root` / `123456`。

> ⚠️ **编码坑（已实测）**：不加 `--default-character-set=utf8mb4` 时，`SHOW VARIABLES` 显示 `character_set_client=gbk`，中文注释与商品名会入库成乱码（`商品分类` 变成 `鍟嗗搧鍒嗙被`）。核验方式是对比 `HEX()` 与预期 UTF-8 字节。

## 与苍穹外卖的表对应

| 原表 | 新表 | 含义 |
|---|---|---|
| `employee` | `staff` | 店员 |
| `category` | `goods_category` | 商品分类（含组合分类） |
| `dish` | `goods` | 商品 |
| `dish_flavor` | `goods_spec` | 商品规格 |
| `setmeal` | `bundle` | 组合箱 |
| `setmeal_dish` | `bundle_goods` | 组合箱内含商品 |
| `user` | `member` | 会员 |
| `address_book` | `address` | 收货地址 |
| `shopping_cart` | `cart` | 购物车 |
| `orders` | `order_main` | 主订单 |
| `order_detail` | `order_item` | 订单明细 |

相对原项目的 **4 增 4 删**：

- 增：`goods.origin`、`goods.shelf_life`、`goods.storage_zone`、`bundle.storage_zone`
- 删：`order_main.tableware_number`、`order_main.tableware_status`、`member.id_number`
- 字段改名：`user_id`→`member_id`、`dish_id`→`goods_id`、`setmeal_id`→`bundle_id`、`dish_flavor`→`spec_text`、`pack_amount`→`delivery_fee`（`int`→`decimal(10,2)`）

**不设库存字段，不加库存表**（见《开发计划.md》§3）。

## 建表时的两处加严（相对原项目）

| 位置 | 原项目 | 本项目 | 原因 |
|---|---|---|---|
| `goods_category.sort` | `int NOT NULL DEFAULT 0` | `int NOT NULL`（无默认值） | 所有者裁定「非空必填」；去掉默认值可让“忘记传 sort”在数据库层直接报错，而不是悄悄排到最前 |
| `order_main.number` | 可空、无唯一约束 | `NOT NULL` + 唯一索引 | 订单号必须唯一且非空 |

## 种子数据概览

| 表 | 行数 | 说明 |
|---|---|---|
| `staff` | 4 | 密码统一 `123456`（MD5）；含 1 个禁用账号用于演示 |
| `goods_category` | 11 | 8 个商品分类（`type=1`）+ 3 个组合分类（`type=2`），`sort` 连续 |
| `goods` | 49 | 生鲜商品，温区分布 常温 19 / 冷藏 24 / 冷冻 6 |
| `goods_spec` | 38 | 生鲜称重类带「净重」等规格；粮油调味与酒水饮料按整包/整瓶售卖，**不设规格** |
| `bundle` | 5 | 整箱温区由内含商品最高温区推导 |
| `bundle_goods` | 22 | `name`/`price` 为冗余快照，已核验与 `goods` 一致 |
| `member` | 8 | `openid` 为 28 位、以 `o` 开头；不含身份证号 |
| `address` | 8 | 每个下单会员一个默认地址，区划编码取真实值 |
| `cart` | 3 | 1 号会员预置，便于演示确认订单 |
| `order_main` / `order_item` | 26 / 54 | 覆盖状态 1~6 全部取值；跨近 14 天，供营业额与销量统计 |

**订单状态分布**：待付款 2 / 待接单 3 / 已接单 2 / 派送中 2 / 已完成 13 / 已取消 4（超时未支付 1、拒单退款 1、会员取消 2）。

> 待付款订单的 `order_time` 是「刚刚」（2 分钟 / 9 分钟前）。**后端超时任务运行满 15 分钟后会把它们自动取消**——这是既定业务规则，不是数据错误。需要重新演示时重跑 `02-data.sql` 即可。

## 账号与演示注意

- 管理端登录：`admin` / `123456`（其余 3 个店员同密码，`zhouyan` 为禁用账号）
- **会员无法直接登录**：`member.openid` 是种子值，微信 `code2session` 换回的真实 openid 与它不同，因此首次真实登录会**新建一个会员**，看不到种子里那 26 笔订单。若要用种子会员的数据演示，需要把该 openid 改成真实值，或临时在库里按 openid 造会员。

## 修改表的注意事项

- 改 `02-data.sql` 后请重跑导入，勿手工改库，避免脚本与库不一致。
- 时间列统一用 `NOW() - INTERVAL n MINUTE`（分钟级）与 `TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL n DAY), 'HH:MM:SS')`（按天），**不要写死日期**，否则脚本两天后就过期。
- `order_main.amount` 必须等于 `Σ order_item.amount + delivery_fee`（`02-data.sql` 末尾自带自检语句，正常应返回 0 条不一致）。
