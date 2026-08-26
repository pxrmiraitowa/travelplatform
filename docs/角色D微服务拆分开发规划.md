# 角色 D 微服务拆分开发规划

## 1. 角色定位

角色 D 负责本项目后端微服务拆分工作，核心目标是将现有单体 Spring Boot 后端拆分为至少 3 个业务微服务，并保证服务边界清晰、数据归属明确、跨服务调用可说明、主要业务流程在微服务版本中仍可运行。

本项目建议拆分为 4 个业务微服务：

| 服务 | 负责内容 | 管理数据表 |
| --- | --- | --- |
| `user-service` | 用户、角色、联系人、登录身份、管理员身份 | `user`、`role`、`user_role`、`user_contact` |
| `product-service` | 航班、车次、酒店、房型、旅游产品、景点、优惠券、价格对比商品数据 | `flight`、`train_ticket`、`hotel`、`hotel_room`、`tour_package`、`attraction`、`coupon` |
| `order-service` | 统一订单、订单明细、订单状态流转、订单取消 | `orders`、`order_flight`、`order_train`、`order_hotel`、`order_tour` |
| `content-trip-service` | 行程、AI 或本地规则行程、旅行分享、评价、价格提醒 | `trip_plan`、`trip_plan_item`、`share_post`、`share_image`、`review`、`price_alert` |

## 2. 开始前提

### 2.1 单体系统可运行

微服务拆分前必须先确认原单体系统可以作为基线运行，避免拆分过程中无法判断问题来自旧系统还是新结构。

需要完成：

1. 后端 `travel-platform-server` 可以成功执行 `mvn test`。
2. 本地 MySQL 可连接，数据库名为 `travel_platform`。
3. 单体服务可以正常启动。
4. 登录、商品查询、下单、订单查询、行程、评价等核心接口至少能手工验证。

建议记录的基线接口：

| 接口 | 验证目标 |
| --- | --- |
| `POST /api/auth/login` | 用户登录正常，返回 token |
| `GET /api/public/flights` | 航班查询正常 |
| `GET /api/public/hotels` | 酒店查询正常 |
| `POST /api/orders/flights` | 机票下单正常 |
| `GET /api/orders` | 当前用户订单查询正常 |
| `GET /api/trip-plans` | 当前用户行程查询正常 |

### 2.2 数据表结构已稳定

当前数据库表已经能够支撑微服务拆分。拆分时应遵守一个原则：每张业务表只能由一个服务直接管理，其他服务不能跨库或跨服务直接查询该表。

订单表中可以保留 `user_id`、`product_id` 等业务 ID，但这些字段只作为业务关联标识，不做跨库外键，不通过联表查询访问其他服务的数据。

### 2.3 前端接口路径先保持稳定

为了降低联调风险，第一阶段不建议大改前端。各微服务可以继续保留原来的接口路径，例如：

| 原接口 | 拆分后服务 |
| --- | --- |
| `/api/auth/**` | `user-service` |
| `/api/users/**` | `user-service` |
| `/api/user-contacts/**` | `user-service` |
| `/api/public/flights/**` | `product-service` |
| `/api/public/trains/**` | `product-service` |
| `/api/public/hotels/**` | `product-service` |
| `/api/public/tours/**` | `product-service` |
| `/api/public/price-compare/**` | `product-service` |
| `/api/orders/**` | `order-service` |
| `/api/reviews/**` | `content-trip-service` |
| `/api/trip-plans/**` | `content-trip-service` |
| `/api/shares/**` | `content-trip-service` |
| `/api/price-alerts/**` | `content-trip-service` |
| `/api/admin/users/**` | `user-service` |
| `/api/admin/products/**` | `product-service` |
| `/api/admin/orders/**` | `order-service` |
| `/api/admin/content/**` | `content-trip-service` |

后续可以通过 Nginx、前端代理或 Spring Cloud Gateway 做统一转发。

### 2.4 不引入真实支付和第三方实时 API

本项目是课程实践项目，微服务拆分阶段仍应保持演示稳定。不接入真实支付，不接入第三方实时票务、酒店或旅游产品 API。价格、库存、优惠券、景点和行程生成仍使用站内演示数据或本地规则。

### 2.5 与其他角色协作边界明确

| 成员 | 与角色 D 的协作内容 |
| --- | --- |
| 成员 A | 协助整理服务划分图、接口清单、数据表归属表、跨服务调用说明 |
| 成员 B | 协助完成微服务版本回归测试和测试记录 |
| 成员 C | 协助完成微服务 Dockerfile、docker-compose、CI/CD 流水线 |
| 成员 E | 协助完成微服务 Kubernetes 部署、HPA、故障处理实验 |

## 3. 总体技术路线

建议采用“保留单体基线，新建微服务版本”的方式，不直接破坏原有 `travel-platform-server`。

推荐新增目录：

```text
travel-platform-microservices/
  pom.xml
  common-lib/
  user-service/
  product-service/
  order-service/
  content-trip-service/
```

各服务仍使用 Spring Boot 3、Spring Security、MyBatis-Plus、MySQL、JWT。第一阶段不强制引入注册中心、配置中心或消息队列，优先保证课程演示和端到端业务链路稳定。

公共模块 `common-lib` 建议包含：

1. 统一返回结构 `Result`。
2. 统一状态码 `ResultCode`。
3. 业务异常 `BusinessException`。
4. 全局异常处理 `GlobalExceptionHandler`。
5. 分页结构 `PageResult`。
6. 订单业务类型和订单状态常量。
7. 必要的公共工具类。

安全相关代码可以第一阶段复制到各服务中，保持 JWT secret 一致，使各服务都能独立解析登录 token。等微服务稳定后，再考虑抽成公共安全模块。

## 4. 分阶段开发规划

### 第 0 阶段：建立单体基线

预计时间：1-2 小时。

目标：确认拆分前系统状态正常，形成后续回归对照。

任务：

1. 从当前代码创建微服务拆分开发分支。
2. 运行后端测试。
3. 启动单体后端。
4. 手工验证登录、商品查询、下单、订单查询、行程查询等关键接口。
5. 保存运行截图、测试截图和接口调用结果。

交付物：

| 交付物 | 内容 |
| --- | --- |
| 单体基线运行记录 | 服务启动截图、端口、数据库配置 |
| 单体测试结果 | `mvn test` 结果 |
| 原接口清单初稿 | 当前前端依赖的主要接口 |

### 第 1 阶段：创建微服务工程骨架

预计时间：半天。

目标：建立可独立构建的多模块 Maven 工程。

任务：

1. 新建 `travel-platform-microservices` 目录。
2. 创建父级 `pom.xml`。
3. 创建 `common-lib`、`user-service`、`product-service`、`order-service`、`content-trip-service` 五个 Maven 子模块。
4. 每个业务服务配置独立端口。
5. 每个业务服务提供健康检查和版本接口。

建议端口：

| 服务 | 端口 |
| --- | --- |
| `user-service` | `8101` |
| `product-service` | `8102` |
| `order-service` | `8103` |
| `content-trip-service` | `8104` |

统一健康检查：

| 接口 | 用途 |
| --- | --- |
| `GET /api/public/health` | 服务健康检查 |
| `GET /api/public/version` | 返回服务名、版本号、启动时间 |

交付物：

| 交付物 | 内容 |
| --- | --- |
| 微服务 Maven 工程 | 父工程和 4 个业务服务 |
| 健康检查接口 | 每个服务可独立检查存活状态 |
| 版本接口 | 每个服务可展示服务名和版本 |

### 第 2 阶段：抽取公共能力

预计时间：半天。

目标：减少重复代码，统一接口返回和异常处理。

任务：

1. 将公共返回结构迁移到 `common-lib`。
2. 将业务异常和异常处理迁移到 `common-lib`。
3. 将订单类型、订单状态等常量迁移到 `common-lib`。
4. 各业务服务依赖 `common-lib`。
5. 保证各服务返回格式仍与单体一致。

交付物：

| 交付物 | 内容 |
| --- | --- |
| `common-lib` | 公共响应、异常、常量、分页结构 |
| 公共依赖说明 | 哪些类被各服务共享 |

### 第 3 阶段：拆分 user-service

预计时间：1 天。

目标：完成用户、角色、联系人和认证相关功能拆分。

迁移范围：

| 类型 | 内容 |
| --- | --- |
| Controller | `AuthController`、`UserController`、`UserContactController`、`AdminAuthController`、`AdminUserManageController` |
| Service | `AuthService`、`UserService`、`UserContactService`、`AdminAuthService`、`AdminUserManageService` |
| Entity | `User`、`Role`、`UserRole`、`UserContact` |
| Mapper | `UserMapper`、`RoleMapper`、`UserRoleMapper`、`UserContactMapper` |

保留接口：

| 接口 | 功能 |
| --- | --- |
| `POST /api/auth/register` | 用户注册 |
| `POST /api/auth/login` | 用户登录 |
| `POST /api/auth/logout` | 用户退出 |
| `GET /api/users/me` | 当前用户信息 |
| `PUT /api/users/me` | 修改当前用户信息 |
| `/api/user-contacts/**` | 常用联系人管理 |
| `/api/admin/auth/**` | 管理员认证 |
| `/api/admin/users/**` | 后台用户管理 |

注意事项：

1. JWT 生成逻辑仍由 `user-service` 负责。
2. 其他服务使用相同 JWT secret 解析 token。
3. 用户状态和角色只由 `user-service` 直接访问数据库。
4. 其他服务不要直接查询 `user`、`role`、`user_role`、`user_contact` 表。

交付物：

| 交付物 | 内容 |
| --- | --- |
| `user-service` | 可独立启动和构建 |
| 用户数据库脚本 | 用户相关表结构和演示数据 |
| 用户接口验证记录 | 注册、登录、联系人、管理员用户管理 |

### 第 4 阶段：拆分 product-service

预计时间：1 天。

目标：完成商品、库存、价格对比和景点基础数据拆分。

迁移范围：

| 类型 | 内容 |
| --- | --- |
| Controller | `FlightController`、`TrainController`、`HotelController`、`TourController`、`PriceCompareController`、`AdminProductManageController`、商品图片上传相关接口 |
| Service | `FlightService`、`TrainService`、`HotelService`、`TourService`、`PriceCompareService`、`AdminProductManageService`、`MediaUploadService` |
| Entity | `Flight`、`TrainTicket`、`Hotel`、`HotelRoom`、`TourPackage`、`Attraction`、`Coupon` |
| Mapper | `FlightMapper`、`TrainTicketMapper`、`HotelMapper`、`HotelRoomMapper`、`TourPackageMapper`、`AttractionMapper`、`CouponMapper` |

保留接口：

| 接口 | 功能 |
| --- | --- |
| `/api/public/flights/**` | 航班查询和详情 |
| `/api/public/trains/**` | 车次查询和详情 |
| `/api/public/hotels/**` | 酒店查询和详情 |
| `/api/public/tours/**` | 旅游产品查询和详情 |
| `/api/public/price-compare/**` | 价格对比 |
| `/api/admin/flights/**` | 后台航班管理 |
| `/api/admin/trains/**` | 后台车次管理 |
| `/api/admin/hotels/**` | 后台酒店管理 |
| `/api/admin/hotel-rooms/**` | 后台房型管理 |
| `/api/admin/tours/**` | 后台旅游产品管理 |

新增内部接口：

| 接口 | 调用方 | 功能 |
| --- | --- | --- |
| `GET /internal/products/{type}/{id}/snapshot` | `order-service`、`content-trip-service` | 查询商品名称、价格、库存、状态等快照 |
| `POST /internal/products/stock/deduct` | `order-service` | 下单时扣减库存 |
| `POST /internal/products/stock/restore` | `order-service` | 取消订单时恢复库存 |
| `GET /internal/attractions?city=...` | `content-trip-service` | 查询行程生成候选景点 |

注意事项：

1. 所有商品库存只允许 `product-service` 修改。
2. 订单服务不能直接访问商品表。
3. 行程服务不能直接访问景点表。
4. 价格对比依赖商品和优惠券数据，建议归入 `product-service`。

交付物：

| 交付物 | 内容 |
| --- | --- |
| `product-service` | 可独立启动和构建 |
| 商品数据库脚本 | 商品、优惠券、景点相关表结构和演示数据 |
| 内部商品接口 | 商品快照、扣库存、恢复库存、景点查询 |

### 第 5 阶段：拆分 order-service

预计时间：1-1.5 天。

目标：完成统一订单和订单状态流转拆分，并把商品访问改为跨服务调用。

迁移范围：

| 类型 | 内容 |
| --- | --- |
| Controller | `OrderController`、`AdminOrderManageController` |
| Service | `OrderService`、`AdminOrderManageService` |
| Entity | `Orders`、`OrderFlight`、`OrderTrain`、`OrderHotel`、`OrderTour` |
| Mapper | `OrdersMapper`、`OrderFlightMapper`、`OrderTrainMapper`、`OrderHotelMapper`、`OrderTourMapper` |

新增客户端：

| 客户端 | 调用目标 | 用途 |
| --- | --- | --- |
| `ProductClient` | `product-service` | 查询商品快照、扣库存、恢复库存 |

下单流程：

1. `order-service` 接收下单请求。
2. 调用 `product-service` 查询商品快照。
3. 校验商品状态、价格和库存。
4. 调用 `product-service` 扣减库存。
5. 扣库存成功后写入 `orders` 主表。
6. 写入对应订单明细表。
7. 返回订单详情或下单成功结果。

取消订单流程：

1. 查询订单并校验订单归属。
2. 判断订单状态是否允许取消。
3. 修改订单状态为已取消。
4. 根据订单业务类型调用 `product-service` 恢复库存。
5. 返回取消结果。

异常和补偿：

| 场景 | 处理方式 |
| --- | --- |
| 商品服务不可用 | 下单失败，提示商品信息暂不可用 |
| 商品不存在或下架 | 下单失败，提示商品不可预订 |
| 库存不足 | 下单失败，提示库存不足 |
| 扣库存成功但订单写入失败 | 调用库存恢复接口进行补偿，并记录日志 |
| 取消订单时恢复库存失败 | 记录日志，返回明确错误或进入人工处理说明 |

新增内部接口：

| 接口 | 调用方 | 功能 |
| --- | --- | --- |
| `GET /internal/orders/{id}/review-check?userId=...` | `content-trip-service` | 校验订单是否存在、是否属于用户、是否已完成 |
| `POST /internal/orders/{id}/reviewed` | `content-trip-service` | 可选，用于评价成功后标记订单评价状态 |

交付物：

| 交付物 | 内容 |
| --- | --- |
| `order-service` | 可独立启动和构建 |
| 订单数据库脚本 | 订单主表和订单明细表 |
| 跨服务下单链路 | 查询商品、扣库存、创建订单 |
| 订单取消链路 | 修改订单状态、恢复库存 |

### 第 6 阶段：拆分 content-trip-service

预计时间：1 天。

目标：完成行程、分享、评价和价格提醒拆分。

迁移范围：

| 类型 | 内容 |
| --- | --- |
| Controller | `ReviewController`、`ShareController`、`TripPlanController`、`PriceAlertController` |
| Service | `ReviewService`、`ShareService`、`TripPlanService`、`AiTripPlanService`、`PriceAlertService` |
| Entity | `Review`、`SharePost`、`ShareImage`、`TripPlan`、`TripPlanItem`、`PriceAlert` |
| Mapper | `ReviewMapper`、`SharePostMapper`、`ShareImageMapper`、`TripPlanMapper`、`TripPlanItemMapper`、`PriceAlertMapper` |

新增客户端：

| 客户端 | 调用目标 | 用途 |
| --- | --- | --- |
| `OrderClient` | `order-service` | 评价前校验订单状态和归属 |
| `ProductClient` | `product-service` | 行程生成查询景点，价格提醒查询商品价格 |

业务改造：

| 功能 | 改造方式 |
| --- | --- |
| 评价提交 | 调用 `order-service` 判断订单是否完成、是否属于当前用户、是否已评价 |
| AI 或本地规则行程 | 调用 `product-service` 获取目的地景点候选 |
| 价格提醒 | 调用 `product-service` 获取商品当前价格和商品名称 |

失败处理：

| 场景 | 处理方式 |
| --- | --- |
| 订单服务不可用 | 拒绝评价，提示订单状态暂不可确认 |
| 订单未完成 | 拒绝评价，提示订单暂不可评价 |
| 景点接口失败 | 使用本地规则或固定演示候选数据兜底 |
| 商品价格查询失败 | 保留价格提醒记录，当前价格显示为暂不可用 |

交付物：

| 交付物 | 内容 |
| --- | --- |
| `content-trip-service` | 可独立启动和构建 |
| 内容行程数据库脚本 | 行程、分享、评价、价格提醒表 |
| 评价跨服务校验 | 调用订单服务完成状态判断 |
| 行程跨服务查询 | 调用产品服务获取景点 |

### 第 7 阶段：数据库拆分

预计时间：半天到 1 天。

目标：做到每个服务只访问自己的数据库或 schema。

推荐使用同一个 MySQL 实例，不同数据库：

| 数据库 | 服务 | 表 |
| --- | --- | --- |
| `travel_user` | `user-service` | `user`、`role`、`user_role`、`user_contact` |
| `travel_product` | `product-service` | `flight`、`train_ticket`、`hotel`、`hotel_room`、`tour_package`、`attraction`、`coupon` |
| `travel_order` | `order-service` | `orders`、`order_flight`、`order_train`、`order_hotel`、`order_tour` |
| `travel_content_trip` | `content-trip-service` | `trip_plan`、`trip_plan_item`、`share_post`、`share_image`、`review`、`price_alert` |

注意事项：

1. 不使用跨库外键。
2. 不跨服务联表查询。
3. 订单明细应保存必要的商品快照字段，例如商品名称、航班号、酒店名、房型名、价格等。
4. 演示数据按服务拆分成独立 SQL 文件。

交付物：

| 交付物 | 内容 |
| --- | --- |
| 数据表归属表 | 每张表归属到唯一服务 |
| 独立 schema 脚本 | 每个服务一份建表和演示数据脚本 |
| 数据访问说明 | 说明服务不跨库查表 |

### 第 8 阶段：服务间调用和故障处理

预计时间：半天。

目标：把所有跨服务调用点讲清楚，并具备可演示的失败处理。

跨服务调用清单：

| 调用方 | 被调用方 | 场景 |
| --- | --- | --- |
| `order-service` | `product-service` | 下单前查询商品价格、状态、库存 |
| `order-service` | `product-service` | 下单时扣减库存 |
| `order-service` | `product-service` | 取消订单时恢复库存 |
| `content-trip-service` | `order-service` | 评价前校验订单状态 |
| `content-trip-service` | `product-service` | 行程生成时查询景点 |
| `content-trip-service` | `product-service` | 价格提醒查询商品当前价格 |

统一失败处理策略：

| 策略 | 说明 |
| --- | --- |
| 超时控制 | 服务调用超时时间建议设置为 2-3 秒 |
| 明确错误 | 返回可理解的业务错误，不暴露底层异常 |
| 日志记录 | 记录目标服务、接口、业务 ID、失败原因 |
| 降级处理 | 行程生成、价格提醒等非强一致场景可以使用备用结果 |
| 补偿处理 | 订单写入失败但库存已扣减时，调用库存恢复接口 |

交付物：

| 交付物 | 内容 |
| --- | --- |
| 跨服务调用说明 | 调用方、被调用方、接口、失败处理 |
| 故障处理说明 | 超时、异常、降级、补偿 |
| 故障演示记录 | 停止某个依赖服务后的返回结果截图 |

### 第 9 阶段：联调入口和路由转发

预计时间：半天。

目标：让前端能通过统一入口访问微服务版本。

推荐转发规则：

| 路径 | 目标服务 |
| --- | --- |
| `/api/auth/**` | `user-service` |
| `/api/users/**` | `user-service` |
| `/api/user-contacts/**` | `user-service` |
| `/api/public/flights/**` | `product-service` |
| `/api/public/trains/**` | `product-service` |
| `/api/public/hotels/**` | `product-service` |
| `/api/public/tours/**` | `product-service` |
| `/api/public/price-compare/**` | `product-service` |
| `/api/orders/**` | `order-service` |
| `/api/reviews/**` | `content-trip-service` |
| `/api/trip-plans/**` | `content-trip-service` |
| `/api/shares/**` | `content-trip-service` |
| `/api/price-alerts/**` | `content-trip-service` |
| `/api/admin/users/**` | `user-service` |
| `/api/admin/flights/**` | `product-service` |
| `/api/admin/trains/**` | `product-service` |
| `/api/admin/hotels/**` | `product-service` |
| `/api/admin/hotel-rooms/**` | `product-service` |
| `/api/admin/tours/**` | `product-service` |
| `/api/admin/orders/**` | `order-service` |
| `/api/admin/shares/**` | `content-trip-service` |
| `/api/admin/reviews/**` | `content-trip-service` |

可选实现方式：

1. 使用 Nginx 做路径转发。
2. 使用 Vite dev proxy 做本地联调。
3. 使用 Spring Cloud Gateway 做后端统一网关。

课程演示优先推荐 Nginx 或前端代理，配置简单、风险低。

交付物：

| 交付物 | 内容 |
| --- | --- |
| 转发配置 | Nginx、Vite proxy 或网关配置 |
| 联调记录 | 前端访问微服务接口成功截图 |

### 第 10 阶段：微服务回归测试

预计时间：1 天。

目标：证明单体中的主要业务流程在微服务版本中仍可运行。

重点回归用例：

| 用例 | 验证内容 |
| --- | --- |
| UC01 | 用户注册、登录、退出 |
| UC02 | 个人资料和常用联系人管理 |
| UC03 | 机票查询、详情查看、下单、库存扣减 |
| UC04 | 火车票查询、详情查看、下单、席别库存扣减 |
| UC05 | 酒店查询、详情查看、预订、房型库存扣减 |
| UC06 | 旅游产品浏览、详情查看、下单、库存扣减 |
| UC07 | 订单列表、订单详情、订单取消、库存恢复 |
| UC08 | 已完成订单评价 |
| UC09 | 手动行程规划管理 |
| UC10 | AI 或本地规则生成行程并保存 |
| UC11 | 旅行分享发布与浏览 |
| UC12 | 价格对比与价格提醒 |
| UC13 | 管理员商品与图片管理 |
| UC14 | 管理员用户、订单、内容管理 |

建议优先保证的演示链路：

1. 登录。
2. 航班查询。
3. 机票下单。
4. 订单中心查看订单。
5. 取消订单并恢复库存。
6. 后台修改订单状态。
7. 已完成订单评价。
8. 行程生成并保存。

交付物：

| 交付物 | 内容 |
| --- | --- |
| 微服务回归测试记录 | 每个用例的测试结果 |
| 接口测试截图 | 登录、下单、取消、评价、行程 |
| 服务日志截图 | 跨服务调用正常和异常日志 |

## 5. 推荐时间安排

| 时间 | 工作内容 | 主要产出 |
| --- | --- | --- |
| 第 1 天 | 建立单体基线、冻结拆分方案、创建微服务工程骨架 | 基线记录、工程骨架、服务划分初稿 |
| 第 2 天 | 拆分 `common-lib` 和 `user-service` | 用户服务、用户数据库脚本、用户接口验证 |
| 第 3 天 | 拆分 `product-service` | 产品服务、商品内部接口、商品数据库脚本 |
| 第 4 天 | 拆分 `order-service`，改造下单和取消链路 | 订单服务、跨服务库存调用、订单接口验证 |
| 第 5 天 | 拆分 `content-trip-service` | 内容行程服务、评价校验、行程生成调用 |
| 第 6 天 | 数据库拆分、路由转发、跨服务失败处理 | 独立 schema、转发配置、故障处理说明 |
| 第 7 天 | 微服务回归测试、整理交付材料 | 回归测试记录、接口清单、答辩说明材料 |

## 6. 风险点和处理建议

| 风险点 | 影响 | 处理建议 |
| --- | --- | --- |
| 一次性重构范围过大 | 容易导致系统无法运行 | 保留单体，新增微服务目录，逐个服务拆 |
| 前端接口路径变化过多 | 前后端联调成本上升 | 第一阶段保留原 `/api` 路径，通过代理转发 |
| 订单和商品耦合严重 | 下单、取消容易出错 | 先设计商品快照、扣库存、恢复库存内部接口 |
| 分布式事务复杂 | 课程项目时间不够 | 使用本地事务加简单补偿，不引入复杂事务框架 |
| JWT 鉴权重复 | 多服务安全配置重复 | 第一阶段复制安全配置，保证 secret 一致 |
| 数据库拆分后演示数据不一致 | 回归测试失败 | 按服务维护独立 SQL，提前准备演示数据 |
| 服务调用失败无提示 | 答辩时难以说明可靠性 | 每个调用点都设计超时、错误提示和日志 |

## 7. 最终交付清单

角色 D 最终需要提交以下内容：

| 交付物 | 说明 |
| --- | --- |
| 微服务代码 | `user-service`、`product-service`、`order-service`、`content-trip-service`、`common-lib` |
| 服务划分图 | 说明为什么按用户、产品、订单、内容行程拆分 |
| 数据表归属表 | 每张表归属到唯一服务 |
| 服务接口清单 | 外部接口和内部接口分开列 |
| 跨服务调用说明 | 订单查商品、扣库存、恢复库存、评价查订单、行程查景点 |
| 失败处理说明 | 超时、错误提示、降级和补偿 |
| 微服务回归记录 | 证明原系统主要业务流程在微服务版本仍可运行 |
| 健康检查记录 | 每个服务健康检查和版本接口截图 |
| 构建运行记录 | 每个服务可独立构建、启动和测试 |

## 8. 答辩说明重点

角色 D 答辩时建议围绕以下问题展开：

1. 为什么拆成这 4 个服务。
2. 每个服务负责哪些业务能力。
3. 每张业务表归哪个服务管理。
4. 为什么不能跨服务直接查表。
5. 订单下单时如何查询商品和扣减库存。
6. 订单取消时如何恢复库存。
7. 评价如何判断订单是否已完成。
8. 行程生成如何获取景点数据。
9. 服务调用失败时如何处理。
10. 如何证明微服务版本仍能跑通原来的核心业务流程。

## 9. 开工优先级建议

如果时间有限，建议按以下顺序推进：

1. 先完成服务划分、表归属、接口清单文档。
2. 再完成微服务工程骨架。
3. 优先拆 `user-service` 和 `product-service`，因为它们是其他服务的基础。
4. 重点攻克 `order-service`，这是最能体现微服务拆分价值的部分。
5. 最后拆 `content-trip-service`，并补齐评价和行程的跨服务调用。
6. 至少保证登录、商品查询、机票下单、订单取消、评价、行程生成这几条链路可演示。

