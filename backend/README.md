# 鲜禾 xianhe-fresh · 后端

生鲜前置仓「30 分钟达」业务的服务端。与《接口文档》逐条对应：管理端 55 个接口、用户端 26 个接口。

## 技术栈

| 项 | 选型 | 说明 |
|---|---|---|
| JDK | **17** | 与 Spring Boot 2.7.3 的官方支持范围一致 |
| 框架 | Spring Boot 2.7.3 | |
| 持久层 | **纯 MyBatis 2.2.0 + PageHelper 1.4.7** | 不用 MyBatis-Plus，SQL 全部手写在 XML 里 |
| 连接池 | Druid 1.2.1 | |
| 缓存 / 分布式锁 | **Redisson 3.17.7（核心包）** + StringRedisTemplate | 手写 `RedissonClient` Bean，不用 starter，避免替换 Spring Data Redis 的自动配置 |
| 对象存储 | aliyun-sdk-oss 3.10.2 | 图片上传 |
| 接口文档 | knife4j 3.0.2 | 启动后访问 `/doc.html` |

**刻意不使用**：MyBatis-Plus、fastjson、POI、微信支付 SDK。

### 两处与参照项目的版本差异（都是为了让它在 Spring Boot 2.7 上真的能起来）

- **PageHelper 用 1.4.7 而不是 1.3.0**：1.3.0 的 `PageHelperAutoConfiguration` 直接注入 `List<SqlSessionFactory>`，与 `SqlSessionFactory` 需要的 `PageInterceptor` 形成 Bean 循环，而 Spring Boot 2.6+ 默认禁止循环引用，启动即失败。1.4.3 起改用 `ObjectProvider` 打破循环，因此本项目**不需要**打开 `spring.main.allow-circular-references` 这个全局放宽开关。
- **Redisson 用核心包而不是 starter**：starter 会替换 Spring Data Redis 的连接工厂，使 `StringRedisTemplate` 的序列化与连接行为变得不直观。本项目只需要一个 `RedissonClient`。

## 目录结构

```
xianhe-fresh/
├─ xianhe-common/          通用模块：常量、上下文、异常、统一返回、工具类、配置属性
│   └─ com/xianhe/{constant,context,enumeration,exception,json,properties,result,utils}
├─ xianhe-pojo/            数据模型
│   └─ com/xianhe/{entity,dto,vo}
├─ xianhe-server/          服务模块
│   └─ com/xianhe/{annotation,aspect,config,controller/{admin,user},handler,interceptor,mapper,service,service/impl,task,websocket}
│       └─ resources/{application.yml, application-dev.yml, mapper/*.xml}
└─ api-test/               接口测试（Node，零依赖）
```

`xianhe-common`、`xianhe-pojo` 的包名与参照工程保持同构，便于对照阅读；`com.xianhe` 是统一的根包。

## 运行前置

1. **MySQL**：建库并导入 `简历项目/sql/01-schema.sql` 与 `02-data.sql`（命令见该目录 README）。
2. **Redis**：本机 `D:\tool\Redis\Redis-x64-3.2.100`，带密码，使用 **11 号库**（与其它项目隔离）。
3. **配置文件**：把 `xianhe-server/src/main/resources/application-dev.yml.example` 复制为
   `application-dev.yml` 并填入数据库口令、Redis 口令、对象存储与微信小程序凭证。
   该文件已被 `.gitignore` 忽略，**不会进入版本库**。

## 运行

```bash
# 编译打包（必须显式指定 JDK 17：本机 JAVA_HOME 默认指向 JDK 21）
JAVA_HOME="D:/tool/jdk/jdk-17.0.18" mvn package -DskipTests

# 启动
java -Dfile.encoding=UTF-8 -jar xianhe-server/target/xianhe-server-1.0.0.jar --server.port=8080
```

> `--server.port=8080` 建议始终显式传：若运行环境中存在 `SERVER__PORT` 之类的环境变量，
> Spring Boot 的宽松绑定会把它当作 `server.port`，优先级高于 `application.yml`。

启动后：

| 地址 | 用途 |
|---|---|
| `http://127.0.0.1:8080/doc.html` | 接口文档（knife4j） |
| `http://127.0.0.1:9000` | 管理端（走 nginx 便携包时） |

## SQL 性能采集（p6spy）

依赖与 `spy.properties` 已内置，但**默认不启用**，正常启动不产生任何采集开销。要采集时只加两个启动参数，把驱动换成 P6Spy 并把 URL 加前缀：

```bash
java -jar xianhe-server/target/xianhe-server-1.0.0.jar \
  --spring.datasource.druid.driver-class-name=com.p6spy.engine.spy.P6SpyDriver \
  --spring.datasource.druid.url='jdbc:p6spy:mysql://localhost:3306/xianhe_fresh?...'
```

输出在 `spy.properties` 里配的 `logfile`（默认 `D:/project/.workbuddy/tmp/p6spy/p6spy.log`），每行格式为 `时间戳|耗时ms|类别|SQL`，SQL 可能跨行。工作区里的 `tmp/sql-top.mjs` 会把 SQL 归一化成模板、按「总耗时 = 次数 × 单次」排序，用来找整轮测试里最贵的语句。

## 接口测试

`api-test/` 下是零依赖的 Node 测试脚本（只用内置 `fetch` 与 `child_process`）。

```bash
node api-test/run.mjs              # 跑全部模块
node api-test/run.mjs staff        # 只跑店员模块
```

设计要点：**同时断言 HTTP 响应与底层存储的真实状态**。只断言响应体的话，
「服务端返回成功但没写库」这类问题会被漏掉，因此 `lib.mjs` 里除了请求封装，
还提供了直连 MySQL 的 `sql()`（用于前置清理、后置清理与库内断言），
`shop.mjs` 还会直接读 redis-cli 校验营业状态。

脚本可重复执行：开始与结束都会清掉自己造的数据。

| 模块 | 文件 | 接口数 | 断言数 | 状态 |
|---|---|---|---|---|
| 店员 | `staff.mjs` | 9 | 67 | ✅ |
| 商品分类 | `category.mjs` | 6 | 43 | ✅ |
| 通用（文件上传） | `common.mjs` | 1 | 18 | ✅ |
| 店铺 | `shop.mjs` | 2 | 12 | ✅ |
| 商品 | `goods.mjs` | 7 | 93 | ✅ |
| 组合箱 | `bundle.mjs` | 6 | 88 | ✅ |
| 用户端 · 登录与鉴权 | `user-login.mjs` | 2 | 23 | ✅ |
| 用户端 · 商品浏览（含缓存） | `user-goods.mjs` | 4 | 56 | ✅ |
| 用户端 · 购物车 | `user-cart.mjs` | 5 | 63 | ✅ |
| 用户端 · 收货地址 | `user-address.mjs` | 7 | 58 | ✅ |
| 用户端 · 订单（提交 / 支付 / 查询 / 详情 / 取消 / 复购 / 催单） | `user-order.mjs` | 7 | 115 | ✅ |
| 管理端 · 订单（搜索 / 角标 / 详情 / 状态流转 / 超时取消） | `admin-order.mjs` | 8 | 64 | ✅ |
| 管理端 · 工作台（今日数据 / 订单·商品·组合箱总览） | `workspace.mjs` | 4 | 29 | ✅ |
| 管理端 · 报表（营业额 / 会员 / 订单 / 销量 TOP10） | `report.mjs` | 4 | 54 | ✅ |

### 演示数据：工作台与报表要有数据才看得出东西

统计口径都依赖「近 30 天的订单与会员」，而最初的种子数据只有 29 单、**当天一单都没有**，
所以另配了一个造数脚本（不在交付工程内）：

```
D:\project\.workbuddy\tmp\assets-tools\seed-report-data.mjs     # 造数（幂等，可 --clean 只清理）
```

它造 12 个会员（含今天注册 2 个）与近 30 天约 200 单：明细取库里真实商品/组合箱做快照，
`amount = Σ明细 + 5.00 运费` 自洽，每天 4~10 单并留出状态分布（今天含待付款 / 待接单 / 已接单 / 派送中 / 已完成 / 已取消）。

> 注意：脚本给「今天」留的 3 单**待付款**，下单时间在 15 分钟内 —— 过 15 分钟仍会被超时任务自动取消（这是
> 契约 2.8.9 的既定行为）。演示前若想让「待付款」页签有单，重跑一次脚本即可。

商品与组合箱两个模块的用例覆盖了全部失败分支（分类类型不符、上架被分类禁用拦下、
下架被在售组合箱拦下、删除被上架状态或组合箱引用拦下、内含商品重复 / 份数非正 / 商品不存在），
并核对删除时的连带清理（`goods_spec` / `bundle_goods` / `cart`）。

除接口测试外，商品与组合箱还做过一轮**浏览器级联调**（真实浏览器 → 接口 → 数据库）：
在界面上新增商品、上架、组箱、起售、下架、删除，每一步都回库核对。
脚本在工作区外：`D:\project\.workbuddy\tmp\assets-tools\admin-e2e-goods-bundle.mjs`（需 Playwright）。

工作台与报表同理，脚本为同目录的 `admin-e2e-workspace-report.mjs`：把页面上显示的数字与接口返回的数字逐个比对
（5 个关键指标、订单总览 6 行、四张图渲染、点总览行跳对应页签、快捷「近 7 天」后汇总随之变化）。

## 实现约定

| 项 | 约定 |
|---|---|
| 统一返回 | `{code, msg, data}`，`code === 1` 为成功；错误文案集中在 `MessageConstant` |
| 鉴权 | 管理端请求头 `token`，用户端 `authentication`；拦截器未通过时返回 **HTTP 200 + `{code:0, msg:"登录状态已失效"}`**（前端 axios 只处理 2xx 响应体，返回 401 会让前端走不到「清 token 跳登录」的分支） |
| 当前登录人 | 存 `BaseContext`（ThreadLocal），业务层不接收前端传入的操作人 id |
| 公共字段 | `create_time` / `create_user` / `update_time` / `update_user` 由 `@AutoFill` + AOP 填充，前端不传 |
| 时间格式 | 全局 `JacksonObjectMapper` 统一为 `yyyy-MM-dd HH:mm:ss` |
| 金额 | 库中 `decimal(10,2)`，Java 用 `BigDecimal` |
| 更新语句 | 一律**动态更新**：只写提交过来的字段，未提交的保持原值。既避免把 NOT NULL 列刷成 null，也避免漏传某个字段就把数据抹掉。可空字段用**空串表示清空**——前端清空输入框、点「移除图片」提交的都是空串，`null` 才表示「这一项不改」 |
| 分类查询 | 管理端**不过滤 `status`**（否则禁用即不可恢复）；用户端只返回 `status = 1` 并按 `sort` 升序 |
| 用户端鉴权 | 请求头 `authentication`，与员工令牌**密钥不同、互不通用**；白名单只有 `/user/member/login` 与 `/user/shop/status` |
| 地址归属 | 用户端地址的查 / 改 / 删 / 设默认都按 `member_id` 校验归属（参照工程这三个接口都不校验，知道 id 就能改别人的地址）；**查不到与不属于自己共用「地址不存在」一句文案**，不给外人用 id 探测。另有「默认地址互斥」与「第一条自动默认」两条规则 |
| 用户端登录 | 只收 `code`：服务端调微信 `jscode2session` 换 openid，member 表没有就自动注册，再签发 JWT。openid 的换取完全在服务端，小程序拿不到也不需要 |
| 购物车 | 同款 = `member_id + goodsId/bundleId + specText`，且 `spec_text` 为 NULL 与有值**不互相合并**（参照工程会串行）；请求体不带数量，服务端 +1/-1、减到 0 删行；`amount` 存**小计**（单价 × 数量），不是单价 |
| 购物车按现值返回 | `/user/cart/list` 的名称 / 图片 / 小计**按现值回查覆盖**（库里仍留加购时的快照），另返回 `status`（可结算状态）与 `priceChanged`。已下架 / 已停售的项不能结算：前端标注并拦「去结算」，服务端下单时再拦一道。合计只算可结算的项 |
| 管理端订单流转 | 接单 `2 → 3`、派送 `3 → 4`、完成 `4 → 5`（写 `delivery_time`）、拒单 `2 → 6`（写拒单原因）、取消 `1/2/3/4 → 6`（写取消原因）；**每一步都校验前置状态**，不满足返回「订单状态错误」且数据不变；拒单与取消时若已支付，同时置 `pay_status = 2` |
| 超时未支付自动取消 | Spring Task `@Scheduled(cron = "0 * * * * ?")`：每分钟扫 `status = 1` 且 `order_time` 早于 15 分钟前的订单，置 `6` 并写 `cancel_reason = "订单超时未支付，系统自动取消"`（契约 2.8.9）；不设库存，取消不涉及回滚 |
| WebSocket 提醒 | `@ServerEndpoint("/ws/{sid}")` + `ServerEndpointExporter`；握手须带 `?token=<管理端令牌>`（浏览器不能加自定义请求头），验签失败以 `1008` 关闭；**只在支付成功推 `type = 1`（来单）**、催单推 `type = 2`，消息体 `{type, orderId, content}`，推送挂在事务提交后；管理端顶栏连接后响铃、弹通知、点击通知跳该单详情；提示音用 `src/assets/` 下的两个音频素材（取自参照工程） |
| 并发与口令 | 订单状态迁移一律「条件更新」（`WHERE id = ? AND status = ?`），影响行数为 0 即返回「订单状态错误」；超时收口是一条批量 UPDATE。口令 PBKDF2 + 每账号随机盐（`PasswordUtil`），存量 MD5 登录后自动升级 |
| 用户端订单 | 查询一律按 `member_id` 校验归属（**别人的订单按「订单不存在」**）；列表 `order_time DESC, id DESC`、明细一次批量取回；**取消**仅限未进入履约（`1` / `2`），已支付的取消即置 `pay_status = 2`；**复购**同款累加并按现值刷新快照，商品已删除则保留订单快照（购物车标成不可结算）；**催单** `2` / `3` / `4` 可催（催接单 / 催出餐 / 催配送） |
| 金额 | 库中 `decimal(10,2)`、Java 用 `BigDecimal`，**计算全在服务端**（下单金额尤其不采信请求体）。JSON 里是**数字**（保留两位靠 `BigDecimal` 的标度）；前端只做展示 —— 管理端走 `money()`、小程序走 `xh-price` / `money()`，内部都是 `Number(v).toFixed(2)`，两种类型都吃。**前端不参与算钱**，与参照工程「数字传输 + 前端 `toFixed`」是同一路子 |
| 下单不采信请求体 | `POST /user/order/submit` 只认 `addressBookId` / `payMethod` / `remark` / `estimatedDeliveryTime` / `deliveryStatus`；金额、运费、收货信息、昵称、明细全由服务端取（地址按 `addressBookId` 回查并校验归属、明细读购物车、运费取常量 `5.00`）。前端多带的字段被忽略 —— 参照工程是 `BeanUtils.copyProperties` 照收，改请求体就能篡改金额 |
| 热点缓存 | 用户端商品/组合箱列表按分类缓存：`xh:goods:category:{分类id}` / `xh:bundle:category:{分类id}`。**防穿透**空值哨兵（空结果也写 `[]`，TTL 1 分钟）、**防击穿** Redisson `RLock` 只放一个线程回源、**防雪崩** TTL 30 分钟 + 0~5 分钟抖动。管理端的上下架与「上架中商品的修改」精准清该分类的键 |
| Bean 命名 | 管理端与用户端存在同名控制器（`GoodsController`、`BundleController`、`ShopController`、`GoodsCategoryController`），**必须显式指定 Bean 名**（如 `@RestController("userGoodsController")`），否则启动即 `ConflictingBeanDefinitionException` |
| 状态规则 | 「新增一律为 0」（店员 / 分类禁用、商品下架、组合箱停售）与「在售即完好」（商品上架要求分类启用；组合箱起售要求内含商品全部上架且分类启用；商品下架要求不被在售组合箱引用）。两条规则写在《接口文档》1.5，商品、组合箱模块的**新增 / 修改 / 状态**三个入口都按它校验 |
| 批量删除 | `ids` 为空时直接返回成功、不执行 SQL；空集合判定放在 Service 层，**不写在 Mapper XML 里**（原因见下） |
| 工作台口径 | 今日运营数据固定统计「今天」；订单总览里**待接单 / 待派送不限时间**（它们是「手上还有多少活」，否则昨天接单还没派的单会从工作台消失、与订单页页签对不上），已完成 / 已取消 / 全部限今天；商品与组合箱总览按 `status` 计数 |
| 报表口径 | 时间区间**缺省最近 30 天**（只传一端时另一端按 30 天跨度补齐，`begin` 晚于 `end` 返回空序列）——与报表页「默认展示近 30 天」的说明一致；查询一律**一次按天聚合再补 0**，不逐天查库（参照工程是逐天 for 循环，30 天要发 30 条 SQL）；完成率保留 4 位、金额保留 2 位；销量 TOP10 只聚合 `status = 5` 的订单明细，返回**数组**（参照工程返回两个逗号分隔的字符串） |
| **报表索引提示** | 报表四条聚合只要带 `status` 就 `FORCE INDEX(idx_report)`（`status, order_time, amount`）。原因：优化器会挑单列的 `idx_order_status`，把该状态**全部订单**扫完再过滤时间，24 万行、30 天区间实测 408ms；走覆盖索引只扫区间内的行，61ms。不带 `status` 的「全部订单按天」不提示，交给优化器走 `idx_order_time`。索引提示写成 `<if test="status != null">`，不是无条件 `FORCE` |

## 代码风格

与参照工程（苍穹外卖）保持一致，注释只写「为什么」，不写「做了什么」：

```java
/**
 * 商品业务实现
 */
public class GoodsServiceImpl implements GoodsService {

    //主键
    private Long id;

    /**
     * 上架 / 下架
     * @param status
     * @param id
     */
    public void setStatus(Integer status, Long id) {
        Goods goods = requireGoods(id);

        // 上架：所属分类必须启用，否则用户在分类页看不到它、只有搜索能搜到
        if (StatusConstant.ENABLE.equals(status)) {
            assertCategoryEnabled(goods.getCategoryId());
        }
        // 下架：不能被起售中的组合箱引用，否则在售的箱子会缺件
        else if (...) { ... }
    }
}
```

- **类**：一行说明。
- **方法**：一行说明 + `@param 名`（不写说明）+ `@return`（非 void 才有）；取值不直观的参数才补一句，如 `@param status 0 下架 / 1 上架`。
- **字段 / 常量**：`//一行说明`，不用 javadoc。
- **业务逻辑的解释**：写成方法体里的 `//`，逐行说明判断依据与后果。
- 不用 `<p>` / `<ul>` / `{@code}` 这类富文本，不写跨多段的长篇 javadoc。

### 一个必须知道的坑：Mapper XML 里不要调用集合方法

`<if test="ids != null and ids.size() > 0">` 这种写法在 JDK 17 上会炸。

MyBatis 3.5.7 用 OGNL 求值表达式，OGNL 会反射调用 `size()`；而
`List.of(...)` / `Stream.toList()` 产生的是 `java.util.ImmutableCollections` 里的
**包私有类**，JPMS 拒绝为它开反射权限，于是抛 `InaccessibleObjectException`，
最终被兜底成一句「未知错误」，排查成本很高。

因此本项目的批量 Mapper 方法一律**不做空集合防护**，只声明「入参不可为空」这个前置条件，
由 Service 层在调用前判定。（`foreach` 本身不会调用集合方法，不受影响。）

## 用户端（小程序）相关的两处测试说明

1. **登录接口的成功路径无法纯自动化**：`/user/member/login` 要小程序 `wx.login` 换来的 code，而微信只认真实小程序发起的 code。
   因此 `api-test/user-login.mjs` 覆盖失败路径与令牌行为；成功路径（换 openid → 自动注册 → 签发令牌 → 二次登录不重复注册）
   用本地桩跑通，脚本在 `D:\project\.workbuddy\tmp\`：`wx-stub.mjs`（桩）与 `verify-wx-login.mjs`（验证），
   后端加参数启动即可：`--xianhe.wechat.jscode2session-url=http://127.0.0.1:9099/sns/jscode2session`。
2. **其余用户端接口的令牌由测试自签**：`api-test/lib.mjs` 的 `mintUserToken(memberId)` 读本地 `application-dev.yml` 里的
   `user-secret-key`，用与服务端相同的 HS256 算法签一个令牌 —— 在服务端看来与登录签发的完全等价。
