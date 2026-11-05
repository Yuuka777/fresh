# 鲜禾 · 生鲜前置仓新零售系统

生鲜前置仓的线上门店系统。顾客在小程序下单，门店在管理端接单、派送、完成，覆盖商品与组合箱、购物车、订单履约、实时提醒与经营报表。

- 后端：Java 17 · Spring Boot 2.7 · MyBatis · MySQL 8 · Redis · WebSocket · Spring Task
- 管理端：Vue 3 · Vite · Element Plus · ECharts · Pinia
- 小程序：uni-app（同时支持编译到微信小程序与 H5）

后端共 **81 个接口**（管理端 55 + 用户端 26），接口契约见 `admin-web/接口文档.md`。

## 目录结构

```
.
├── backend/          后端（Maven 多模块：common / pojo / server）
│   └── api-test/     接口测试脚本（Node，零依赖）
├── admin-web/        管理端（Vue 3 + Vite）
├── mini-program/     小程序源码（uni-app）
└── sql/              建表与演示数据
```

## 运行

### 1. 数据库

MySQL 8，先建库再按顺序导入：

```sql
CREATE DATABASE xianhe_fresh DEFAULT CHARACTER SET utf8mb4;
```

```bash
mysql -uroot -p --default-character-set=utf8mb4 xianhe_fresh < sql/01-schema.sql
mysql -uroot -p --default-character-set=utf8mb4 xianhe_fresh < sql/02-data.sql
```

### 2. 后端

准备 JDK 17、Maven、Redis。复制配置模板并填自己的环境：

```bash
cp backend/xianhe-server/src/main/resources/application-dev.yml.example \
   backend/xianhe-server/src/main/resources/application-dev.yml
```

需要填写：数据库账号口令、Redis 口令、对象存储（阿里云 OSS）的 endpoint / bucket / AK / SK、小程序的 appid 与 secret。

```bash
cd backend
mvn package -DskipTests
java -jar xianhe-server/target/xianhe-server-1.0.0.jar --server.port=8080
```

启动后接口文档在 `http://127.0.0.1:8080/doc.html`。

### 3. 管理端

```bash
cd admin-web
npm install
npm run dev        # 开发态，默认 9100，代理到后端 8080
npm run build      # 产出 dist/
```

生产部署可把 `dist/` 放到任意静态服务器，并把 `/api` 反代到后端、`/ws` 升级为 WebSocket。

### 4. 小程序

```bash
cd mini-program
npm install
npm run dev:mp-weixin     # 产出到 dist/dev/mp-weixin
```

用微信开发者工具导入 `dist/dev/mp-weixin`。真机预览需在微信后台配置服务器域名（接口域名 + 图片域名）。

> `src/manifest.json` 里的 `mp-weixin.appid` 已清空——用你自己的小程序 appid 填入（或导入时在开发者工具里填），仓库里不放任何真实 appid 与密钥。

> 小程序登录走微信 `code2session`，需要 `application-dev.yml` 里填自己的 appid / secret；本机调试可在开发者工具里关闭域名校验。

## 演示账号

管理端：`admin` / `123456`（首次登录后建议修改）。

## 说明

- 仓库**不含**构建产物与依赖（`node_modules/`、`target/`、`dist/`），也不含任何真实密钥，配置一律走 `application-dev.yml`（已在 `.gitignore` 中排除）。
- 报表与工作台要有数据才看得出效果：`sql/02-data.sql` 已带演示数据；也可以直接在小程序下单。
- 已知边界见 `backend/README.md` 与 `admin-web/接口文档.md` 的变更记录。
