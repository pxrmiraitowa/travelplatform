# 旅游平台项目说明

## 1. 项目简介

这是一个课程演示用的综合旅游平台，采用前后端分离架构，覆盖用户端和后台管理端的完整业务流程。当前项目重点是业务链路完整、演示稳定、模块清晰，不接入真实支付，也不依赖第三方实时业务接口。

核心业务包括：

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

项目根目录提供了通用启动脚本：

- `start-dev.ps1`
- `start-dev.cmd`

通用脚本用于普通联调，不携带本地第三方 AI 密钥。  
如果你需要在自己电脑上带环境变量启动第三方 AI，可以基于通用脚本复制一份本地专用脚本，例如 `start-dev-local.ps1`、`start-dev-local.cmd`，并把它们加入 `.gitignore`，避免真实密钥进入仓库。

脚本会自动：

- 检查 `mvn` 和 `npm` 是否可用
- 分别打开前后端终端窗口
- 启动后端 `mvn spring-boot:run`
- 启动前端 `npm run dev`

PowerShell 启动通用版本：

```powershell
.\start-dev.ps1
```

Windows 命令行启动通用版本：

```text
start-dev.cmd
```

PowerShell 启动本地 AI 本地专用版本：

```powershell
.\start-dev-local.ps1
```

Windows 命令行启动本地 AI 本地专用版本：

```text
start-dev-local.cmd
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

- 项目启动时会自动执行 `schema.sql` 和 `ai_trip_plan.sql`
- 会自动建表并补充默认演示数据
- 景点初始化采用幂等方式，不会因重启自动清空 `attraction` 表
- 本地上传图片默认保存在后端目录下的 `uploads/`

如果你本地使用高版本 JDK，也可以先打包后再启动：

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

## 6. AI 行程规划说明

### 6.1 功能位置

前台 `行程规划` 模块新增了 `AI 生成行程` 入口。

### 6.2 当前能力边界

当前版本支持：

- 固定为结构化输入
  - `目的地`
  - `停留天数`
  - `出发日期`
  - `旅游偏好`
- 生成按天拆分的景点推荐结果
- 前端预览每日景点与推荐理由
- 一键保存到现有 `行程规划`
- 在预览中区分 `AI增强` 和 `本地生成`

当前实现方式：

- 使用本地景点库作为推荐基础数据
- 使用后端规则完成候选景点筛选、偏好匹配和按天分配
- 接入 OpenAI 兼容的第三方大模型调用链，用于景点排序优化和推荐理由补充
- 当未配置 `AI_API_KEY` 或第三方调用失败时，会自动回退为本地生成，保证演示稳定

相关接口：

- AI 行程预览：`POST /api/trip-plans/ai-preview`
- AI 行程保存：`POST /api/trip-plans/ai-save`

### 6.3 第三方 AI 配置

后端通过环境变量读取第三方 AI 配置，不在仓库中保存真实密钥。

支持的配置项：

- `AI_API_KEY`：必填，第三方模型密钥
- `AI_BASE_URL`：可选，默认 `https://dashscope.aliyuncs.com/compatible-mode/v1`
- `AI_MODEL`：可选，默认 `qwen-plus`
- `AI_CHAT_COMPLETIONS_PATH`：可选，默认 `/chat/completions`
- `AI_TIMEOUT_SECONDS`：可选，默认 `20`
- `AI_USE_JSON_SCHEMA_RESPONSE_FORMAT`：可选，默认 `false`

示例：

```powershell
$env:AI_API_KEY="你的APIKey"
$env:AI_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1"
$env:AI_MODEL="qwen-plus"
mvn spring-boot:run
```

如果你只是本地调试，也可以基于 `start-dev.ps1` 复制一份本地专用脚本，填入你自己的环境变量后再运行本地版本。该脚本建议仅保留在本机，并加入 `.gitignore`。

## 7. 已有功能概览

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
- 通过 AI 规划入口生成并保存行程

### 7.5 旅行分享

访问路径：

- 列表：`/shares`
- 发布：`/shares/create`
- 详情：`/shares/{id}`

支持：

- 浏览公开分享
- 登录后发布分享
- 上传分享图片

### 7.6 酒店与旅游产品图片展示

当前支持：

- 维护封面图
- 维护详情图集
- 前台详情页展示主图和图集
- 老数据在没有详情图集时自动回退到封面图

### 7.7 后台图片上传

后台酒店管理和旅游产品管理支持直接从本地上传图片，不需要手动输入图片 URL。

支持：

- 封面图上传
- 详情图集多图上传
- 上传数量到上限后自动隐藏上传入口
- 上传成功后自动回填表单

### 7.8 酒店到房型管理快捷入口

后台酒店管理页新增了 `管理房型` 按钮：

- 点击后直接跳转到房型管理页
- 自动带上当前酒店 `hotelId`
- 房型管理页会自动按该 `hotelId` 筛选
- 新增房型时也会自动带入该 `hotelId`

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

## 10. 演示建议

建议按以下顺序演示项目：

1. 启动 MySQL、后端、前端
2. 使用普通用户账号登录前台
3. 在个人中心维护常用出行人
4. 体验机票、酒店或旅游产品下单
5. 查看订单中心
6. 进入行程规划，演示 AI 生成行程、预览与一键保存
7. 使用管理员账号登录后台
8. 在后台维护商品、订单或内容
9. 返回前台查看订单状态变化和评价结果

如果要演示图片上传能力，建议补充以下流程：

1. 管理员进入酒店管理或旅游产品管理
2. 上传封面图和详情图集
3. 返回前台打开对应详情页查看展示效果
4. 在酒店管理页点击 `管理房型`，继续维护该酒店的房型数据

## 11. 注意事项

- 本项目用于本地和课程演示，不接入真实支付
- 价格提醒和价格对比是站内演示能力，不依赖第三方实时业务接口
- 本地上传图片保存到后端 `uploads/` 目录
- `travel-platform-server/uploads/` 已加入 `.gitignore`
- 如需启用第三方 AI，务必通过环境变量或本地忽略脚本提供真实密钥，不要把密钥写回仓库
- 如修改数据库连接信息，需要同步更新 `application.yml`

## 12. 微服务版本快速验收

### 12.1 版本与环境

- 改造前版本：Git 标签 `monolith-start`
- 微服务中期版本：Git 标签 `midterm-2026-08-29-microservices`
- 当前交付分支：`codex/microservices-ci-integration`
- CI 构建环境：JDK 17、Node.js 22、MySQL 8.4
- 成员 E 本地实验环境：Windows 11、PowerShell 7.2+、Docker Desktop（Docker Engine 29.3.1）、kubectl 1.34.1、Kind 节点 Kubernetes 1.34.3

版本和提交关系详见 [`docs/版本与提交记录.md`](docs/版本与提交记录.md)。

### 12.2 Docker Compose 启动微服务

在项目根目录执行：

```powershell
docker compose up --build -d
docker compose ps
```

首次启动会创建 `travel_user`、`travel_product`、`travel_order` 和 `travel_content_trip` 数据库并导入演示数据。`docker compose down` 只停止并删除容器，命名卷中的 MySQL 数据和上传文件仍会保留。

| 组件 | 本机端口 | 健康检查或入口 |
| --- | ---: | --- |
| 前端 | 8088 | `http://127.0.0.1:8088/` |
| API 网关 | 8000 | `http://127.0.0.1:8000/api/public/health` |
| 用户服务 | 8101 | `http://127.0.0.1:8101/api/public/health` |
| 商品服务 | 8102 | `http://127.0.0.1:8102/api/public/health` |
| 订单服务 | 8103 | `http://127.0.0.1:8103/api/public/health` |
| 内容与行程服务 | 8104 | `http://127.0.0.1:8104/api/public/health` |
| MySQL | 3307 | 容器内端口 3306 |

测试账号仍使用第 5 节中的 `demo_user / 123456` 和 `admin / Admin123456`。以上仅为课程演示数据，不应作为生产凭据。

### 12.3 Kubernetes 与云原生实验

- Kubernetes 部署和回滚入口：[`deploy/k8s`](deploy/k8s)、[`scripts/deploy-kind.sh`](scripts/deploy-kind.sh)、[`scripts/rollback-kind.sh`](scripts/rollback-kind.sh)
- 成员 E 独立实验配置：[`deploy/member-e`](deploy/member-e)
- HPA、故障和性能复现入口：[`docs/成员E-第一二阶段完成报告-20260831.md`](docs/成员E-第一二阶段完成报告-20260831.md)
- 原始证据及校验说明：[`experiments/results/成员E-原始证据说明.md`](experiments/results/成员E-原始证据说明.md)
- 最终交付材料总索引：[`docs/最终提交材料索引.md`](docs/最终提交材料索引.md)
