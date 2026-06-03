# 旅游平台项目说明

## 1. 项目简介

这是一个课程演示用的综合旅游平台，包含前台用户端和后台管理端，覆盖以下核心业务：

- 用户注册、登录、退出
- 机票查询、详情查看、下单
- 火车票查询、详情查看、下单
- 酒店查询、详情查看、预订
- 旅游产品浏览、详情查看、下单
- 统一订单中心
- 行程规划
- 旅行分享发布与浏览
- 订单评价
- 价格对比与价格提醒
- 后台用户、商品、订单、内容管理

技术栈：

- 前端：Vue 3、Vite、Vue Router、Pinia、Element Plus、Axios
- 后端：Spring Boot 3、Spring Security、MyBatis-Plus、MySQL、JWT

## 2. 角色说明

- 普通用户：登录后使用前台业务功能
- 管理员：登录后台后管理用户、商品、订单、分享和评价内容

## 3. 运行环境

- JDK 17
- Maven 3.9+
- MySQL 8.x
- Node.js 18+

## 4. 快速启动

### 4.1 推荐：一键启动前后端

项目根目录提供了两个启动脚本：

- `start-dev.ps1`
- `start-dev.cmd`

脚本会自动：

- 检查 `mvn` 和 `npm` 是否可用
- 分别打开两个终端窗口
- 启动后端 `mvn spring-boot:run`
- 启动前端 `npm run dev`

PowerShell 方式：

```powershell
.\start-dev.ps1
```

双击方式：

```text
start-dev.cmd
```

启动成功后访问：

- 前端首页：[http://localhost:5173](http://localhost:5173)
- 后端接口：[http://localhost:8080](http://localhost:8080)
- Swagger：[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### 4.2 单独启动后端

后端目录：

```text
travel-platform-server
```

启动方式：

```bash
cd travel-platform-server
mvn spring-boot:run
```

默认配置来自 `travel-platform-server/src/main/resources/application.yml`：

- 端口：`8080`
- 数据库：`travel_platform`
- 用户名：`root`
- 密码：`123456`

说明：

- 项目启动时会自动执行 `schema.sql`
- 会自动建表并插入一批演示数据
- 本地上传图片默认保存在后端目录下的 `uploads/`

如果你本地使用 JDK 24，也可以改用：

```powershell
cd travel-platform-server
mvn package "-DskipTests"
java -jar target\travel-platform-server-0.0.1-SNAPSHOT.jar
```

### 4.3 单独启动前端

前端目录：

```text
travel-platform-web
```

启动方式：

```bash
cd travel-platform-web
npm install
npm run dev
```

默认地址：

- 前端首页：[http://localhost:5173](http://localhost:5173)

前端已配置代理：

- `/api` 会代理到 `http://localhost:8080`

## 5. 默认演示账号

普通用户：

- 用户名：`demo_user`
- 密码：`123456`

管理员：

- 用户名：`admin`
- 密码：`Admin123456`

## 6. 新增功能说明

### 6.1 酒店和旅游产品图片展示升级

酒店和旅游产品现在支持：

- 维护封面图
- 维护详情图集
- 前台详情页展示主图和图集
- 旧数据在没有详情图集时自动回退到封面图显示

涉及页面：

- 酒店详情页：`/hotel/{id}`
- 旅游产品详情页：`/tour/{id}`

### 6.2 后台支持本地上传商品图片

后台酒店管理和旅游产品管理现在支持直接从本地上传图片，不需要手动输入图片 URL。

支持的上传能力：

- 封面图上传 1 张
- 详情图集上传多张
- 达到上传上限后自动隐藏上传框
- 上传成功后自动回填到表单

后台入口：

- 酒店管理：`/admin/products/hotels`
- 旅游产品管理：`/admin/products/tours`

### 6.3 酒店到房型管理的快捷入口

为了方便维护房型，后台酒店管理页增加了 `管理房型` 按钮：

- 点击后会直接跳转到房型管理页
- 自动带上当前酒店 `hotelId`
- 房型管理页会自动用该 `hotelId` 作为筛选条件
- 新增房型时也会自动带入这个 `hotelId`

后台入口：

- 酒店管理：`/admin/products/hotels`
- 房型管理：`/admin/products/rooms`

## 7. 前台功能概览

### 7.1 首页

首页提供以下业务入口：

- 机票
- 火车票
- 酒店
- 旅游产品
- 旅行分享
- 行程规划
- 我的订单

### 7.2 个人中心

访问路径：

- `/profile`

支持：

- 查看和修改个人资料
- 管理常用出行人
- 查看和删除价格提醒

### 7.3 订单中心

访问路径：

- 订单列表：`/orders`
- 订单详情：`/orders/{id}`

支持：

- 统一查看机票、火车票、酒店、旅游产品订单
- 按业务类型和状态筛选订单
- 取消订单
- 对已完成订单发起评价

### 7.4 行程规划

访问路径：

- 列表页：`/trip-plans`
- 详情页：`/trip-plans/{id}`

支持：

- 创建、编辑、删除行程计划
- 维护每日行程安排

### 7.5 旅行分享

访问路径：

- 列表：`/shares`
- 发布：`/shares/create`
- 详情：`/shares/{id}`

支持：

- 浏览公开分享
- 登录后发布分享
- 上传分享图片

## 8. 后台功能概览

### 8.1 后台登录

- 登录页：`/admin/login`
- 控制台：`/admin/dashboard`

### 8.2 商品管理

后台商品管理覆盖：

- 航班：`/admin/products/flights`
- 车次：`/admin/products/trains`
- 酒店：`/admin/products/hotels`
- 房型：`/admin/products/rooms`
- 旅游产品：`/admin/products/tours`

支持常见 CRUD 操作。

### 8.3 订单管理

访问路径：

- 列表：`/admin/orders`
- 详情：`/admin/orders/{id}`

支持：

- 查询订单
- 查看订单详情
- 修改订单状态
- 后台取消订单

### 8.4 内容管理

包括：

- 分享管理：`/admin/content/shares`
- 评价管理：`/admin/content/reviews`

## 9. 接口与调试

Swagger：

- [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

主要接口分类：

- 公开接口：`/api/public/*`
- 登录后接口：`/api/auth/*`、`/api/users/*`、`/api/orders/*` 等
- 后台接口：`/api/admin/*`

新增图片上传接口：

- 商品图片上传：`POST /api/admin/media/upload`

## 10. 使用建议

建议按以下顺序演示项目：

1. 启动 MySQL、后端、前端
2. 使用普通用户登录前台
3. 先在个人中心维护常用出行人
4. 体验机票、酒店或旅游产品下单
5. 查看订单中心
6. 使用管理员账号登录后台
7. 在后台维护商品、订单或内容
8. 返回前台查看订单状态变化和评价结果

如果要演示新增图片能力，建议补充以下流程：

1. 管理员进入酒店管理或旅游产品管理
2. 上传封面图和详情图集
3. 返回前台打开对应详情页查看展示效果
4. 在酒店管理页点击 `管理房型`，继续维护该酒店的房型数据

## 11. 注意事项

- 本项目用于本地演示，不接入真实支付
- 价格提醒和价格对比是站内演示能力，不依赖第三方实时接口
- 本地上传图片保存到后端 `uploads/` 目录
- `travel-platform-server/uploads/` 已加入 `.gitignore`，本地测试图片不会进入仓库
- 如果修改数据库连接信息，需要同步更新 `application.yml`
