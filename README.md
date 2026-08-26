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

- 推荐容器方式：Docker Desktop，或 Docker Engine + Docker Compose v2
- 本地源码方式：JDK 17、Maven 3.9+、MySQL 8.x、Node.js 22

## 4. 快速启动

### 4.1 推荐：Docker Compose 启动完整系统

换一台机器后，只需要安装 Docker 并取得本仓库代码，不需要单独安装 Java、Maven、Node.js 或 MySQL。

首次启动时，在项目根目录复制环境变量示例文件：

```powershell
Copy-Item .env.example .env
```

Linux 或 macOS：

```bash
cp .env.example .env
```

`.env` 中的值只用于本地演示。共享部署前必须修改 `MYSQL_ROOT_PASSWORD` 和 `JWT_SECRET`，不要提交 `.env`。

构建并启动 MySQL、后端和前端：

```powershell
docker compose up -d --build
```

首次构建需要联网下载基础镜像和依赖。启动完成后检查状态：

```powershell
docker compose ps
```

三个服务都应显示 `healthy`。继续验证前端和完整 API 代理链路：

```powershell
curl.exe --fail http://localhost:8088/healthz
curl.exe --fail http://localhost:8088/api/public/health
```

Linux 或 macOS 将 `curl.exe` 改为 `curl`。预期分别返回 `ok`，以及包含 `"code":200`、`"status":"UP"` 的 JSON。

启动成功后访问：

- 前端首页：[http://localhost:8088](http://localhost:8088)
- 后端健康接口：[http://localhost:8088/api/public/health](http://localhost:8088/api/public/health)
- Swagger：[http://localhost:8088/swagger-ui.html](http://localhost:8088/swagger-ui.html)

Compose 使用三个独立容器：

- `mysql`：官方 `mysql:8.4` 镜像
- `backend`：项目后端 Dockerfile 构建的版本化镜像
- `frontend`：项目前端 Dockerfile 构建的版本化镜像，并由 Nginx 提供静态文件和 `/api` 代理

应用镜像版本由 `.env` 中的 `IMAGE_TAG` 控制，默认是 `0.1.0`，不会只使用 `latest`。

数据库在空数据卷中按以下顺序自动初始化：

1. `schema.sql`：建表
2. `data-demo.sql`：演示数据
3. `demo-data-patch-20260601.sql`：数据迁移补丁
4. `demo-data-charset-repair-20260826.sql`：修复旧数据卷中的中文编码并保持幂等

测试数据脚本不会在普通演示环境自动执行。脚本路径、手动导入方法和迁移规则见 [`deploy/db/README.md`](deploy/db/README.md)。

查看日志：

```powershell
docker compose logs --tail 200 mysql
docker compose logs --tail 200 backend
docker compose logs --tail 200 frontend
```

停止系统但保留数据库和上传文件：

```powershell
docker compose down
```

再次启动：

```powershell
docker compose up -d
```

如需从空数据库重新验证初始化，可执行下面的命令。注意，该命令会永久删除当前 Compose 数据库数据和上传文件：

```powershell
docker compose down -v
docker compose up -d --build
```

### 4.2 GitHub Actions + GHCR + Kind 自动流水线

仓库提供 [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml)。向 GitHub 仓库的 `dev` 分支 push 后，流水线会在同一个串行作业中依次执行：

1. 取代码
2. 安装 Maven 和 npm 依赖
3. 编译后端和前端
4. 执行后端、前端单元测试
5. 使用 MySQL 8.4 执行后端集成测试
6. 制作前后端镜像并推送到 GHCR
7. 创建 Kind 集群并部署 MySQL、后端和前端
8. 检查 Kubernetes 工作负载、前端 `/healthz` 和后端 `/api/public/health`

这些步骤没有设置忽略失败。任意一步返回非零状态后，后面的构建或部署步骤都会被跳过。诊断和上传步骤使用 `if: always()`，因此无论成功还是失败，GitHub Actions 页面都有完整日志，并会保留 30 天的 `ci-cd-evidence-运行ID` 构件。

前后端镜像会同时生成两种版本标签：

- `sha-提交哈希前12位`：Kubernetes 实际部署的不可变提交版本
- `0.1.流水线序号`：便于课程演示和人工识别的发布版本

流水线不会制作或部署 `latest` 标签。镜像名称为：

```text
ghcr.io/<GitHub用户名或组织名>/travel-platform-server:<版本>
ghcr.io/<GitHub用户名或组织名>/travel-platform-web:<版本>
```

当前 `origin` 是 CodeArts 地址，必须先在 GitHub 创建一个空仓库，并把本项目的 `dev` 分支推送过去。保留 CodeArts 作为 `origin` 时，可以增加第二个远程地址：

```powershell
git remote add github https://github.com/<你的GitHub用户名>/travelplatform.git
git push -u github dev
```

第二条命令就是首次触发流水线的 push。以后使用 `git push github dev` 即可再次触发。如果 GitHub 仓库或组织限制了 `GITHUB_TOKEN`，需要在仓库 `Settings -> Actions -> General -> Workflow permissions` 中允许读写；正常情况下不需要另外创建 GHCR 密码，工作流使用当前运行的 `GITHUB_TOKEN` 发布镜像。

流水线相关文件：

- `.github/workflows/ci-cd.yml`：流水线配置
- `deploy/kind/cluster.yaml`：Kind 集群与宿主机端口映射
- `deploy/k8s/*.yaml`：MySQL、后端和前端 Kubernetes 清单
- `scripts/deploy-kind.sh`：创建 Secret、挂载数据库脚本并部署工作负载
- `scripts/health-check.sh`：部署后健康检查
- `scripts/collect-k8s-logs.sh`：成功或失败时收集集群诊断记录

Kubernetes 中的 `mysql:8.4-kind-amd64` 是流水线对官方 `mysql:8.4` amd64 精确摘要添加的 Kind 本地标签，镜像内容没有修改，也没有数据库 Dockerfile。这样可以避免多架构清单和 Kind 节点代理差异导致重复拉取。

本机具备 Docker、Kind、kubectl 和 Git Bash 时，也可以先用本地镜像验证 Kubernetes 部署：

```powershell
$tag = "0.1.0-kind"
docker build -t "travel-platform-server:$tag" .\travel-platform-server
docker build -t "travel-platform-web:$tag" .\travel-platform-web
kind create cluster --name travel-platform-ci --config .\deploy\kind\cluster.yaml --wait 180s
$mysqlSource = "mysql:8.4@sha256:1d6b6a8fcee8ff758ff151d017f5203cd06792a0e698f0a593c9dfcb14609cf0"
docker pull $mysqlSource
docker tag $mysqlSource mysql:8.4-kind-amd64
kind load docker-image mysql:8.4-kind-amd64 --name travel-platform-ci
kind load docker-image "travel-platform-server:$tag" --name travel-platform-ci
kind load docker-image "travel-platform-web:$tag" --name travel-platform-ci
$env:BACKEND_IMAGE = "travel-platform-server"
$env:FRONTEND_IMAGE = "travel-platform-web"
$env:IMAGE_TAG = $tag
$env:MYSQL_ROOT_PASSWORD = "只用于本次验证的数据库密码"
$env:JWT_SECRET = "只用于本次验证且长度足够的JWT密钥-2026"
bash .\scripts\deploy-kind.sh
bash .\scripts\health-check.sh
```

健康检查通过后，在浏览器访问 [http://localhost:18080](http://localhost:18080)。验证结束可删除临时集群：

```powershell
kind delete cluster --name travel-platform-ci
```

### 4.3 本地源码方式：一键启动前后端

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

### 4.4 单独启动后端

后端目录：

```text
travel-platform-server
```

启动方式：

```bash
cd travel-platform-server
mvn spring-boot:run
```

如果你的路径里有中文，Spring Boot 3.3.0～3.3.5 会错误处理该路径，导致启动类明明已编译，却报 ClassNotFoundException。官方确认该问题在 3.3.6 修复。你需要：
把 pom.xml 中：
    <version>3.3.5</version>
改成：
    <version>3.3.6</version>
然后运行：
```bash
mvn clean compile spring-boot:run
```

默认配置来自 `travel-platform-server/src/main/resources/application.yml`：

- 端口：`8080`
- 数据库：`travel_platform`
- 数据库连接：通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 覆盖
- JWT 密钥：通过 `JWT_SECRET` 覆盖
- 上传目录：通过 `UPLOAD_DIR` 覆盖

说明：

- 本地源码方式默认由 Spring Boot 执行 `schema.sql` 和 `data-demo.sql`
- Docker Compose 方式由 MySQL 官方镜像执行编号化初始化脚本，后端设置 `SQL_INIT_MODE=never`，避免重复初始化
- 景点初始化采用幂等方式，不会因重启自动清空 `attraction` 表
- 本地上传图片默认保存在后端目录下的 `uploads/`

如果你本地使用高版本 JDK，也可以先打包后再启动：

```powershell
cd travel-platform-server
mvn package "-DskipTests"
java -jar target\travel-platform-server-0.0.1-SNAPSHOT.jar
```

### 4.5 单独启动前端

前端目录：

```text
travel-platform-web
```

启动方式：

```bash
cd travel-platform-web
npm ci
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
- Docker Compose 的本地配置放在不提交的 `.env` 中；仓库只提交 `.env.example`
- 如修改数据库连接信息，应优先修改环境变量，不要把真实密码写回 `application.yml`
