# 成员A-B接口契约对齐

## 1. 对齐目标

本文用于对齐成员 A 与成员 B 在微服务拆分阶段的接口边界，避免后续合并时出现“接口已迁移但调用方仍读本地表”“网关转发路径不一致”“当前登录用户来源不一致”等问题。

本次对齐重点不是立即把所有跨服务调用都改完，而是先锁定接口契约、字段格式、责任人和替换顺序。后续可以按契约逐步把当前的本地临时适配替换成真实服务调用。

## 2. 当前服务边界

| 服务 | 端口 | 主要负责人 | 当前职责 |
| --- | --- | --- | --- |
| user-service | 8101 | 成员 A | 用户注册、登录、JWT、用户基础信息 |
| product-service | 8102 | 成员 B | 航班、火车、酒店、跟团游、价格比对、商品快照 |
| order-service | 8103 | 成员 A | 订单创建、订单状态、支付模拟、订单查询、订单评价资格 |
| content-trip-service | 8104 | 成员 B | 游记分享、图片上传、行程规划、价格提醒、评价内容 |
| common-lib | 无独立端口 | A/B 共用 | 统一响应、分页对象、异常、通用常量 |

## 3. 统一响应契约

所有服务对外接口和内部调用接口统一使用 `Result<T>` 返回结构。

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

分页接口统一使用 `PageResult<T>` 作为 `data`。

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 0,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

错误码建议保持以下规则：

| 场景 | HTTP 状态 | Result.code | 说明 |
| --- | --- | --- | --- |
| 参数错误 | 400 | 400 | 缺少必要参数、枚举值非法、分页参数非法 |
| 未登录 | 401 | 401 | JWT 缺失、无效或网关未传递用户上下文 |
| 无权限 | 403 | 403 | 用户访问非本人资源 |
| 数据不存在 | 404 | 404 | 商品、订单、用户、内容不存在 |
| 业务冲突 | 409 | 409 | 重复评价、重复收藏、库存不足等 |
| 服务异常 | 500 | 500 | 未预期异常 |

## 4. 当前登录用户契约

### 4.1 临时阶段

成员 B 当前已在 content-trip-service 中使用 `CurrentUserProvider` 获取当前用户，临时读取请求头：

```http
X-User-Id: 1
```

这可以支撑本地演示和分模块开发，但合并前需要与成员 A 的登录、JWT、网关方案对齐。

### 4.2 合并后建议契约

网关或统一认证过滤器完成 JWT 校验后，向下游服务透传以下请求头：

| Header | 必填 | 说明 |
| --- | --- | --- |
| X-User-Id | 是 | 当前登录用户 ID，下游服务只信任网关写入的值 |
| X-Username | 否 | 当前用户名，用于日志或展示兜底 |
| X-User-Role | 否 | 当前用户角色，用于管理端权限判断 |
| Authorization | 否 | 原始 Bearer Token，只有需要二次校验时再使用 |

成员 A 负责在 user-service 或 gateway 中确定登录校验方式，并保证进入 order-service、content-trip-service 的请求携带 `X-User-Id`。

成员 B 负责保留 content-trip-service 的 `CurrentUserProvider` 抽象。如果 Header 名称调整，只需要改这个适配层，不应在业务代码中散落读取 Header 的逻辑。

## 5. 商品快照接口契约

### 5.1 背景

content-trip-service 的价格提醒、order-service 的下单明细都需要读取商品名称、当前价格、是否可用等信息。

当前成员 B 已在 content-trip-service 内保留了临时 `ProductSnapshotService`，用于单库演示阶段直接读取商品表。合并后应替换为调用 product-service 的商品快照接口。

### 5.2 product-service 提供接口

```http
GET /api/internal/products/snapshot?productType=HOTEL&productId=1
```

请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| productType | string | 是 | 商品类型：`FLIGHT`、`TRAIN`、`HOTEL`、`TOUR` |
| productId | long | 是 | 商品 ID |

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "productType": "HOTEL",
    "productId": 1,
    "productName": "海景度假酒店",
    "currentPrice": 399.00,
    "available": true,
    "stockSummary": "可预订",
    "coverImage": "/uploads/hotel/demo.jpg"
  }
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| productType | string | 是 | 商品类型 |
| productId | long | 是 | 商品 ID |
| productName | string | 是 | 展示名称 |
| currentPrice | decimal | 是 | 当前价格 |
| available | boolean | 是 | 是否可购买或可预订 |
| stockSummary | string | 否 | 库存、座位、余量等提示 |
| coverImage | string | 否 | 商品封面，给内容展示兜底 |

### 5.3 调用方

| 调用方 | 使用场景 | 替换目标 |
| --- | --- | --- |
| order-service | 创建订单、订单详情展示 | 不再直接读取商品表 |
| content-trip-service | 价格提醒创建、价格提醒列表 | 替换本地 `ProductSnapshotService` 查询 |

### 5.4 异常约定

| 场景 | 返回 |
| --- | --- |
| `productType` 不支持 | 400 |
| 商品不存在 | 404 |
| 商品已下架或不可购买 | 200，`available=false` |

## 6. 订单评价上下文接口契约

### 6.1 背景

content-trip-service 已迁移评价内容接口，但当前评价资格和订单详情仍通过本地订单表快照读取。合并后应由 order-service 提供订单评价上下文，content-trip-service 只负责评价内容本身。

### 6.2 order-service 提供接口

#### 查询单个订单评价上下文

```http
GET /api/internal/orders/{orderId}/review-context?userId=1
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderId": 1001,
    "orderNo": "T202608270001",
    "userId": 1,
    "bizType": "HOTEL",
    "bizId": 10,
    "orderStatus": 30,
    "travelDate": "2026-08-30",
    "summaryTitle": "海景度假酒店",
    "summarySubtitle": "入住 2026-08-30",
    "reviewable": true
  }
}
```

#### 查询可评价订单

```http
GET /api/internal/orders/reviewable?userId=1&pageNum=1&pageSize=10
```

响应 `data` 使用 `PageResult<OrderReviewContextVO>`。

### 6.3 评价规则分工

| 规则 | 判断服务 | 说明 |
| --- | --- | --- |
| 订单是否存在 | order-service | 订单主数据归 order-service |
| 订单是否属于当前用户 | order-service | 根据 `userId` 判断 |
| 订单是否已完成 | order-service | 建议使用 `OrderStatusConstant.COMPLETED = 30` |
| 是否已经评价 | content-trip-service | 评价内容归 content-trip-service |
| 评价内容是否合法 | content-trip-service | 星级、文本、图片等 |

content-trip-service 在提交评价时流程建议为：

1. 从 `CurrentUserProvider` 获取当前用户 ID。
2. 调用 order-service 查询 `review-context`。
3. 校验 `reviewable=true`。
4. 查询本服务评价表，确认该订单未评价。
5. 写入评价内容。

## 7. 用户基础信息接口契约

### 7.1 背景

游记分享、评价列表需要展示作者昵称和头像。当前 content-trip-service 可通过单库临时方式读取用户表，但微服务边界中用户信息应由 user-service 提供。

### 7.2 user-service 提供接口

```http
GET /api/internal/users/basic?ids=1,2,3
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "userId": 1,
      "nickname": "张三",
      "avatar": "/uploads/avatar/u1.png",
      "status": 1
    }
  ]
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| userId | long | 是 | 用户 ID |
| nickname | string | 是 | 展示昵称 |
| avatar | string | 否 | 头像地址 |
| status | int | 是 | 用户状态，禁用用户可由调用方做展示降级 |

### 7.3 调用方

| 调用方 | 使用场景 |
| --- | --- |
| content-trip-service | 游记列表、游记详情、评价列表展示作者信息 |
| order-service | 订单管理端展示用户基础信息，可选 |

## 8. 内容上传与静态资源契约

content-trip-service 负责游记图片上传和静态资源访问。

上传接口：

```http
POST /api/shares/upload
Content-Type: multipart/form-data
```

表单字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| file | file | 是 | 图片文件 |

约束：

| 项 | 规则 |
| --- | --- |
| 文件大小 | 最大 5MB |
| 文件类型 | jpg、jpeg、png、webp、gif |
| 返回路径 | `/api/public/uploads/...` |

网关需要将以下路径转发到 content-trip-service：

```http
/api/shares/upload
/api/public/uploads/**
```

## 9. 网关路由契约

如果成员 A 后续加入 gateway，建议先按服务职责做路径转发，不改变前端已使用的业务路径。

| 路径 | 目标服务 | 说明 |
| --- | --- | --- |
| `/api/public/flights/**` | product-service | 航班查询 |
| `/api/public/trains/**` | product-service | 火车查询 |
| `/api/public/hotels/**` | product-service | 酒店查询 |
| `/api/public/tours/**` | product-service | 跟团游查询 |
| `/api/public/price-compare/**` | product-service | 价格比对 |
| `/api/public/shares/**` | content-trip-service | 公开游记查询 |
| `/api/public/uploads/**` | content-trip-service | 图片静态资源 |
| `/api/shares/**` | content-trip-service | 游记发布、我的游记、上传 |
| `/api/trip-plans/**` | content-trip-service | 行程规划 |
| `/api/price-alerts/**` | content-trip-service | 价格提醒 |
| `/api/reviews/**` | content-trip-service | 提交评价 |
| `/api/orders/reviewable` | content-trip-service | 前端可评价订单入口，内部再调 order-service |
| `/api/orders/{id}/review` | content-trip-service | 前端订单评价查询入口，内部再调 order-service |
| `/api/orders/**` | order-service | 订单主流程，排除评价内容入口 |
| `/api/users/**` | user-service | 用户相关接口 |
| `/api/auth/**` | user-service | 登录、注册、认证 |

注意：如果 `/api/orders/**` 和 `/api/orders/{id}/review` 同时存在，网关匹配顺序必须保证更具体的评价路径优先进入 content-trip-service。

## 10. A/B 责任拆分

| 契约项 | 成员 A | 成员 B |
| --- | --- | --- |
| 当前用户透传 | 负责 JWT 校验和 `X-User-Id` 透传 | 保持 `CurrentUserProvider` 统一读取 |
| 用户基础信息 | 提供 `/api/internal/users/basic` | 替换内容服务内用户表读取 |
| 商品快照 | 作为 order-service 调用方确认字段 | 提供 `/api/internal/products/snapshot` |
| 订单评价上下文 | 提供 `/api/internal/orders/{id}/review-context` 和 `/api/internal/orders/reviewable` | 替换评价模块本地订单读取 |
| 统一响应 | 复用 common-lib | 复用 common-lib |
| 网关路由 | 配置路由和鉴权 | 提供路径清单并配合联调 |

## 11. 合并前检查清单

| 检查项 | 负责人 | 状态 |
| --- | --- | --- |
| common-lib 中 `Result`、`PageResult`、异常结构没有冲突 | A/B | 待检查 |
| `OrderStatusConstant.COMPLETED = 30` 在 A/B 代码中一致 | A/B | 待检查 |
| `X-User-Id` 是否作为统一用户上下文 Header | A | 待确认 |
| product-service 可以独立编译 | B | 已通过本地编译 |
| content-trip-service 可以独立编译 | B | 已通过本地编译 |
| user-service 可以独立编译 | A | 待 A 确认 |
| order-service 可以独立编译 | A | 待 A 确认 |
| 网关路由对 `/api/orders/{id}/review` 做优先匹配 | A | 待确认 |
| content-trip-service 临时本地订单读取已标注替换点 | B | 已完成 |
| content-trip-service 临时商品快照读取已标注替换点 | B | 已完成 |

## 12. 后续替换顺序

建议按以下顺序推进，尽量降低合并冲突和联调成本：

1. 成员 A 先确定登录后下游统一使用 `X-User-Id`。
2. 成员 B 在 product-service 中补充 `/api/internal/products/snapshot`。
3. 成员 A 在 order-service 中补充订单评价上下文接口。
4. 成员 A 在 user-service 中补充用户基础信息批量查询接口。
5. 成员 B 将 content-trip-service 的价格提醒商品快照读取替换为 product-service 调用。
6. 成员 B 将 content-trip-service 的评价订单读取替换为 order-service 调用。
7. 成员 B 将游记、评价展示中的用户基础信息读取替换为 user-service 调用。
8. 成员 A 配置 gateway 路由，保证前端请求路径不需要大范围调整。
9. A/B 做一次端到端联调：登录、商品查询、下单、完成订单、评价、游记、行程规划、价格提醒。

## 13. 未决问题

| 问题 | 建议决策 | 决策人 |
| --- | --- | --- |
| 内部接口是否也经过 gateway | 演示阶段可服务间直连，最终文档中说明为内部调用 | A |
| 服务间调用使用 RestTemplate、WebClient 还是 OpenFeign | 课程项目建议优先使用简单 RestTemplate 或 WebClient，减少引入复杂治理组件 | A/B |
| 价格提醒是否支持火车票 | 如果 order-service 下单支持火车，product-service 快照也应补齐 `TRAIN` | B |
| 评价是否允许修改或删除 | 当前先按“每个订单只能评价一次”处理，修改删除可放后续版本 | B |
| 用户禁用后历史内容如何展示 | 建议内容保留，昵称展示为“用户已注销/已禁用”或保留历史昵称 | A/B |

## 14. 本次结论

当前 B 侧产品与内容服务已经具备独立编译和单库演示能力，可以继续推进接口替换。A/B 合并前最重要的是先确认三类契约：

1. 当前用户上下文统一使用 `X-User-Id`。
2. 商品信息统一通过 product-service 商品快照接口获取。
3. 订单评价资格统一通过 order-service 评价上下文接口判断。

只要这三类接口稳定，后续即使暂时保留部分本地适配，也不会影响整体微服务拆分方向。合并时可以先合并可编译版本，再按本文第 12 节逐项替换真实跨服务调用。
