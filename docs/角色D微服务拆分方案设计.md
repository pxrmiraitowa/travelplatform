# 角色 D 微服务拆分方案设计

## 1. 设计目标

本方案用于指导综合旅游平台后端从单体 Spring Boot 应用拆分为微服务版本。拆分目标不是追求复杂技术栈，而是在课程实践要求下，形成边界清晰、职责明确、可独立构建运行、可回归验证的业务微服务架构。

核心目标：

1. 将后端拆分为至少 3 个业务微服务，本项目建议拆分为 4 个。
2. 每个服务有明确业务职责和数据归属。
3. 服务之间不能直接跨库或跨表查询。
4. 下单、取消、评价、行程生成等跨模块业务通过服务接口调用完成。
5. 微服务版本保留原主要业务流程，支持前端联调、容器化部署和 Kubernetes 实验。

## 2. 拆分原则

### 2.1 按业务领域拆分

本项目主要业务可以分为用户身份、商品资源、订单交易、内容行程四个领域。因此微服务也按这四个领域拆分：

| 领域 | 服务 |
| --- | --- |
| 用户身份 | `user-service` |
| 商品资源 | `product-service` |
| 订单交易 | `order-service` |
| 内容行程 | `content-trip-service` |

### 2.2 数据归属唯一

每张业务表只允许一个服务直接访问和维护。其他服务如果需要相关数据，只能通过该服务暴露的接口获取。

例如：

1. 订单服务需要航班价格和库存时，不能直接查询 `flight` 表，应调用 `product-service`。
2. 评价服务需要判断订单是否已完成时，不能直接查询 `orders` 表，应调用 `order-service`。
3. 行程服务需要景点数据时，不能直接查询 `attraction` 表，应调用 `product-service`。

### 2.3 保留原接口路径，降低前端改造成本

微服务第一阶段尽量保留单体时期的 `/api` 接口路径，再通过 Nginx、Vite proxy 或网关转发到不同服务。这样前端页面可以少改甚至不改。

### 2.4 优先保证演示稳定

本项目是课程实践项目，不引入真实支付、不引入第三方实时票务 API、不强制引入复杂分布式事务。涉及库存扣减和订单创建的一致性问题，采用本地事务加补偿接口方式处理。

## 3. 总体架构

微服务版本建议保留原前端，后端新增微服务工程：

```text
travel-platform-web
        |
        | HTTP /api
        v
Nginx / Gateway / Vite Proxy
        |
        +------------------------+
        |                        |
        v                        v
  user-service             product-service
        |                        |
        v                        v
   travel_user             travel_product

        +------------------------+
        |                        |
        v                        v
  order-service        content-trip-service
        |                        |
        v                        v
   travel_order         travel_content_trip
```

推荐工程目录：

```text
travel-platform-microservices/
  pom.xml
  common-lib/
  user-service/
  product-service/
  order-service/
  content-trip-service/
```

各服务说明：

| 服务 | 建议端口 | 说明 |
| --- | --- | --- |
| `user-service` | `8101` | 负责认证、用户、角色、联系人和管理员身份 |
| `product-service` | `8102` | 负责航班、车次、酒店、房型、旅游产品、景点、优惠券和商品价格 |
| `order-service` | `8103` | 负责统一订单、订单明细、订单状态和取消订单 |
| `content-trip-service` | `8104` | 负责行程、AI 或本地规则行程、旅行分享、评价和价格提醒 |

## 4. 服务职责设计

### 4.1 user-service

`user-service` 是用户身份中心，负责普通用户和管理员身份相关业务。

主要职责：

1. 用户注册、登录、退出。
2. JWT 生成。
3. 当前用户信息查询与修改。
4. 常用联系人管理。
5. 管理员登录。
6. 后台用户管理和角色管理。

负责接口：

| 接口 | 功能 |
| --- | --- |
| `POST /api/auth/register` | 用户注册 |
| `POST /api/auth/login` | 用户登录 |
| `POST /api/auth/logout` | 用户退出 |
| `GET /api/users/me` | 查询当前用户信息 |
| `PUT /api/users/me` | 修改当前用户信息 |
| `/api/user-contacts/**` | 常用联系人管理 |
| `/api/admin/auth/**` | 管理员认证 |
| `/api/admin/users/**` | 后台用户管理 |

管理数据表：

| 表名 | 说明 |
| --- | --- |
| `user` | 用户基础信息 |
| `role` | 角色信息 |
| `user_role` | 用户角色关系 |
| `user_contact` | 常用联系人 |

### 4.2 product-service

`product-service` 是商品资源中心，负责所有可预订资源、库存、优惠券和景点基础数据。

主要职责：

1. 航班查询和详情。
2. 火车票查询和详情。
3. 酒店、房型查询和详情。
4. 旅游产品查询和详情。
5. 后台商品管理。
6. 商品图片上传和维护。
7. 价格对比。
8. 优惠券查询。
9. 景点基础数据查询。
10. 对订单服务提供商品快照、扣库存和恢复库存接口。

负责接口：

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
| `/api/admin/media/upload` | 后台商品图片上传 |

内部接口：

| 接口 | 调用方 | 功能 |
| --- | --- | --- |
| `GET /internal/products/{type}/{id}/snapshot` | `order-service`、`content-trip-service` | 查询商品快照 |
| `POST /internal/products/stock/deduct` | `order-service` | 扣减商品库存 |
| `POST /internal/products/stock/restore` | `order-service` | 恢复商品库存 |
| `GET /internal/attractions?city=...` | `content-trip-service` | 查询候选景点 |

管理数据表：

| 表名 | 说明 |
| --- | --- |
| `flight` | 航班信息和库存 |
| `train_ticket` | 车次、席别价格和库存 |
| `hotel` | 酒店基础信息 |
| `hotel_room` | 酒店房型、价格和库存 |
| `tour_package` | 旅游产品信息和库存 |
| `attraction` | 景点候选数据 |
| `coupon` | 优惠券信息 |

### 4.3 order-service

`order-service` 是订单交易中心，负责订单创建、查询、详情、取消和后台订单管理。

主要职责：

1. 创建机票订单。
2. 创建火车票订单。
3. 创建酒店订单。
4. 创建旅游产品订单。
5. 查询当前用户订单列表。
6. 查询订单详情。
7. 取消订单。
8. 后台订单查询、详情和状态修改。
9. 为评价服务提供订单可评价校验。

负责接口：

| 接口 | 功能 |
| --- | --- |
| `POST /api/orders/flights` | 创建机票订单 |
| `POST /api/orders/trains` | 创建火车票订单 |
| `POST /api/orders/hotels` | 创建酒店订单 |
| `POST /api/orders/tours` | 创建旅游产品订单 |
| `GET /api/orders` | 查询当前用户订单列表 |
| `GET /api/orders/{id}` | 查询订单详情 |
| `POST /api/orders/{id}/cancel` | 取消订单 |
| `/api/admin/orders/**` | 后台订单管理 |

内部接口：

| 接口 | 调用方 | 功能 |
| --- | --- | --- |
| `GET /internal/orders/{id}/review-check?userId=...` | `content-trip-service` | 校验订单是否属于用户且已完成 |
| `POST /internal/orders/{id}/reviewed` | `content-trip-service` | 可选，评价成功后标记订单已评价 |

管理数据表：

| 表名 | 说明 |
| --- | --- |
| `orders` | 订单主表 |
| `order_flight` | 机票订单明细 |
| `order_train` | 火车票订单明细 |
| `order_hotel` | 酒店订单明细 |
| `order_tour` | 旅游产品订单明细 |

### 4.4 content-trip-service

`content-trip-service` 是内容与行程中心，负责用户行程、AI 或本地规则行程生成、旅行分享、评价和价格提醒。

主要职责：

1. 手动创建、修改、删除行程计划。
2. 维护每日行程安排。
3. AI 或本地规则生成行程预览并保存。
4. 发布旅行分享。
5. 上传分享图片。
6. 浏览分享列表和详情。
7. 提交订单评价。
8. 查询可评价订单。
9. 创建、查询、删除价格提醒。
10. 后台分享和评价内容管理。

负责接口：

| 接口 | 功能 |
| --- | --- |
| `/api/trip-plans/**` | 行程规划和 AI/规则行程生成 |
| `/api/shares/**` | 分享发布和用户分享管理 |
| `/api/public/shares/**` | 公开分享浏览 |
| `/api/reviews/**` | 订单评价 |
| `/api/price-alerts/**` | 价格提醒 |
| `/api/admin/shares/**` | 后台分享管理 |
| `/api/admin/reviews/**` | 后台评价管理 |

管理数据表：

| 表名 | 说明 |
| --- | --- |
| `trip_plan` | 行程计划主表 |
| `trip_plan_item` | 行程每日安排 |
| `share_post` | 旅行分享 |
| `share_image` | 分享图片 |
| `review` | 订单评价 |
| `price_alert` | 价格提醒 |

## 5. 数据归属设计

微服务拆分后建议使用同一个 MySQL 实例，但拆成不同数据库或 schema。

| 数据库 | 服务 | 表 |
| --- | --- | --- |
| `travel_user` | `user-service` | `user`、`role`、`user_role`、`user_contact` |
| `travel_product` | `product-service` | `flight`、`train_ticket`、`hotel`、`hotel_room`、`tour_package`、`attraction`、`coupon` |
| `travel_order` | `order-service` | `orders`、`order_flight`、`order_train`、`order_hotel`、`order_tour` |
| `travel_content_trip` | `content-trip-service` | `trip_plan`、`trip_plan_item`、`share_post`、`share_image`、`review`、`price_alert` |

数据设计约束：

1. 服务只能连接自己的数据库。
2. 不建立跨数据库外键。
3. 不跨服务直接联表查询。
4. 跨服务引用统一保存业务 ID。
5. 订单明细保存商品快照，避免商品后续修改影响历史订单展示。

订单快照建议字段：

| 订单类型 | 建议保存快照 |
| --- | --- |
| 机票订单 | 航班号、航空公司、出发到达城市、出发到达机场、起降时间、舱位、单价 |
| 火车票订单 | 车次号、车次类型、出发到达城市、车站、时间、席别、单价 |
| 酒店订单 | 酒店名、城市、地址、房型名、入住离店日期、单价 |
| 旅游产品订单 | 产品名、目的地、出发城市、天数、出行日期、单价 |

## 6. 跨服务调用设计

### 6.1 订单创建调用商品服务

场景：用户创建机票、火车票、酒店或旅游产品订单。

调用流程：

1. 前端提交下单请求到 `order-service`。
2. `order-service` 调用 `product-service` 查询商品快照。
3. `order-service` 根据商品快照校验状态、价格和库存。
4. `order-service` 调用 `product-service` 扣减库存。
5. 扣库存成功后，`order-service` 写入订单主表和订单明细表。
6. `order-service` 返回下单结果。

失败处理：

| 失败点 | 处理 |
| --- | --- |
| 商品服务不可用 | 下单失败，提示商品信息暂不可用 |
| 商品不存在或下架 | 下单失败，提示商品不可预订 |
| 库存不足 | 下单失败，提示库存不足 |
| 扣库存失败 | 下单失败，不创建订单 |
| 订单写入失败 | 调用库存恢复接口补偿，并记录日志 |

### 6.2 订单取消调用商品服务

场景：用户取消未完成订单。

调用流程：

1. 前端提交取消请求到 `order-service`。
2. `order-service` 校验订单归属和订单状态。
3. `order-service` 修改订单状态为已取消。
4. `order-service` 根据订单业务类型调用 `product-service` 恢复库存。
5. `order-service` 返回取消结果。

失败处理：

| 失败点 | 处理 |
| --- | --- |
| 订单不存在 | 返回订单不存在 |
| 订单不属于当前用户 | 返回无权操作 |
| 订单状态不可取消 | 返回当前状态不可取消 |
| 商品服务恢复库存失败 | 记录日志，返回明确提示或保留人工处理说明 |

### 6.3 评价调用订单服务

场景：用户对已完成订单提交评价。

调用流程：

1. 前端提交评价请求到 `content-trip-service`。
2. `content-trip-service` 调用 `order-service` 校验订单状态。
3. `order-service` 返回订单是否存在、是否属于当前用户、是否已完成。
4. 校验通过后，`content-trip-service` 写入 `review` 表。
5. 可选调用 `order-service` 标记订单已评价。

失败处理：

| 失败点 | 处理 |
| --- | --- |
| 订单服务不可用 | 拒绝评价，提示订单状态暂不可确认 |
| 订单不存在 | 拒绝评价 |
| 非本人订单 | 拒绝评价 |
| 订单未完成 | 拒绝评价 |
| 重复评价 | 拒绝评价 |

### 6.4 行程生成调用商品服务

场景：用户使用 AI 或本地规则生成行程。

调用流程：

1. 前端提交目的地、天数、出发日期和偏好到 `content-trip-service`。
2. `content-trip-service` 调用 `product-service` 查询目的地景点候选。
3. `content-trip-service` 使用 AI 或本地规则生成行程预览。
4. 用户确认后，`content-trip-service` 保存行程计划和每日安排。

失败处理：

| 失败点 | 处理 |
| --- | --- |
| 商品服务不可用 | 使用本地规则或固定演示景点兜底 |
| 景点数据为空 | 返回基础行程建议 |
| AI 服务不可用 | 回退本地规则生成 |

### 6.5 价格提醒调用商品服务

场景：用户创建或查看价格提醒。

调用流程：

1. 用户创建价格提醒到 `content-trip-service`。
2. `content-trip-service` 调用 `product-service` 查询商品当前价格。
3. 保存提醒记录。
4. 用户查看提醒列表时，调用 `product-service` 刷新当前价格。

失败处理：

| 失败点 | 处理 |
| --- | --- |
| 商品不存在 | 拒绝创建提醒 |
| 商品服务不可用 | 创建时拒绝，列表查询时显示当前价格暂不可用 |
| 当前价格获取失败 | 保留提醒记录，不影响列表展示 |

## 7. 鉴权设计

第一阶段建议继续使用 JWT。

设计方式：

1. `user-service` 负责登录和 token 签发。
2. 四个业务服务使用同一 JWT secret。
3. 每个服务本地解析 token，获取 `userId`、`username`、`roles`。
4. 普通用户接口校验登录状态。
5. 后台接口校验管理员角色。

优点：

1. 不需要每次请求都远程调用用户服务。
2. 实现简单，适合课程演示。
3. 每个服务可以独立运行。

限制：

1. 用户状态变更后，已签发 token 不会立刻失效。
2. 后续如果需要更严格权限控制，可以增加 token 黑名单同步或用户服务远程校验。

## 8. 健康检查和版本接口

每个服务都需要提供统一健康检查和版本接口，方便成员 C、E 做容器化、流水线和 Kubernetes 探针。

| 接口 | 返回内容 |
| --- | --- |
| `GET /api/public/health` | 服务名、状态、当前时间 |
| `GET /api/public/version` | 服务名、版本号、启动时间 |

示例返回：

```json
{
  "service": "order-service",
  "status": "UP",
  "time": "2026-08-26 10:00:00"
}
```

## 9. 路由转发设计

为减少前端改造，推荐使用统一入口转发。

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
| `/api/public/shares/**` | `content-trip-service` |
| `/api/price-alerts/**` | `content-trip-service` |
| `/api/admin/auth/**` | `user-service` |
| `/api/admin/users/**` | `user-service` |
| `/api/admin/flights/**` | `product-service` |
| `/api/admin/trains/**` | `product-service` |
| `/api/admin/hotels/**` | `product-service` |
| `/api/admin/hotel-rooms/**` | `product-service` |
| `/api/admin/tours/**` | `product-service` |
| `/api/admin/media/**` | `product-service` |
| `/api/admin/orders/**` | `order-service` |
| `/api/admin/shares/**` | `content-trip-service` |
| `/api/admin/reviews/**` | `content-trip-service` |

## 10. 服务拆分后业务用例归属

| 用例 | 涉及服务 | 说明 |
| --- | --- | --- |
| UC01 用户注册登录与退出 | `user-service` | 用户认证归用户服务 |
| UC02 个人资料与常用联系人管理 | `user-service` | 用户资料和联系人归用户服务 |
| UC03 机票查询、详情查看与下单 | `product-service`、`order-service` | 产品服务查航班和扣库存，订单服务创建订单 |
| UC04 火车票查询、详情查看与下单 | `product-service`、`order-service` | 产品服务查车次和扣库存，订单服务创建订单 |
| UC05 酒店查询、详情查看与预订 | `product-service`、`order-service` | 产品服务查酒店房型和扣库存，订单服务创建订单 |
| UC06 旅游产品浏览、详情查看与下单 | `product-service`、`order-service` | 产品服务查旅游产品和扣库存，订单服务创建订单 |
| UC07 订单查询、详情查看与取消 | `order-service`、`product-service` | 订单服务管理订单，取消时调用产品服务恢复库存 |
| UC08 已完成订单评价 | `content-trip-service`、`order-service` | 内容服务保存评价，订单服务校验状态 |
| UC09 手动行程规划管理 | `content-trip-service` | 行程数据归内容行程服务 |
| UC10 AI 或本地规则生成行程并保存 | `content-trip-service`、`product-service` | 内容服务生成行程，产品服务提供景点候选 |
| UC11 旅行分享发布与浏览 | `content-trip-service` | 分享和图片归内容行程服务 |
| UC12 价格对比与价格提醒 | `product-service`、`content-trip-service` | 价格对比归产品服务，提醒归内容行程服务 |
| UC13 管理员商品与图片管理 | `product-service`、`user-service` | 商品归产品服务，管理员身份由用户服务签发和校验 |
| UC14 管理员用户、订单、内容管理 | `user-service`、`order-service`、`content-trip-service` | 用户、订单、内容分别归对应服务 |

## 11. 设计取舍说明

### 11.1 为什么不拆支付服务

项目明确不接入真实支付，订单支付状态由后台修改模拟。因此支付不是独立业务领域，不单独拆分为支付服务。

### 11.2 为什么价格对比归 product-service

价格对比依赖商品、房型、航班、旅游产品和优惠券数据，这些数据都归产品领域。将价格对比放在 `product-service` 中可以减少跨服务调用。

### 11.3 为什么价格提醒归 content-trip-service

价格提醒是用户保存的提醒内容，和用户的个人内容、行程、分享类似。它需要读取商品价格，但提醒记录本身不属于商品主数据，所以放在 `content-trip-service`。

### 11.4 为什么后台管理不单独拆 admin-service

后台管理本质是对各领域数据的管理入口，不是独立业务数据。按数据归属拆分更清晰：

1. 后台用户管理归 `user-service`。
2. 后台商品管理归 `product-service`。
3. 后台订单管理归 `order-service`。
4. 后台内容管理归 `content-trip-service`。

### 11.5 为什么第一阶段不引入注册中心

课程项目重点是服务拆分、数据归属和跨服务调用。第一阶段可以通过配置文件写服务地址，降低复杂度。后续如果时间充足，可以再引入 Nacos、Eureka 或 Spring Cloud Gateway。

## 12. 最终验收标准

角色 D 的微服务拆分方案需要满足以下标准：

| 检查项 | 标准 |
| --- | --- |
| 服务数量 | 至少 3 个业务微服务，本方案为 4 个 |
| 服务职责 | 每个服务职责明确，无明显重复管理 |
| 数据归属 | 每张业务表有唯一归属服务 |
| 跨服务访问 | 不跨服务直接查表，通过接口调用 |
| 订单链路 | 下单时能查询商品、扣库存、创建订单 |
| 取消链路 | 取消订单后能恢复库存 |
| 评价链路 | 评价前能校验订单状态 |
| 行程链路 | 行程生成能获取景点候选，并有本地回退 |
| 健康检查 | 每个服务有健康检查和版本接口 |
| 回归验证 | 原系统主要业务用例在微服务版本中仍可运行 |

