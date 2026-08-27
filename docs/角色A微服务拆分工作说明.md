# 角色A微服务拆分工作说明

## 1. 工作目标

角色 A 主要负责用户服务、订单服务、认证透传和网关路由相关工作。当前角色 B 已完成 product-service 与 content-trip-service 的主要迁移，并保留了部分单库演示阶段的临时适配。角色 A 后续工作的核心是把用户、订单、认证入口补齐，让 A/B 两侧可以合并成一套可运行的微服务演示版本。

建议角色 A 优先关注“能联通、能演示、契约稳定”，不必一开始引入复杂的服务治理组件。课程项目阶段可以先使用固定端口和简单 HTTP 调用完成服务间协作。

## 2. 当前 B 侧已完成内容

| 模块 | 服务 | 状态 |
| --- | --- | --- |
| 航班查询 | product-service | 已迁移 |
| 火车查询 | product-service | 已迁移 |
| 酒店查询 | product-service | 已迁移 |
| 跟团游查询 | product-service | 已迁移 |
| 价格比对 | product-service | 已迁移 |
| 游记分享 | content-trip-service | 已迁移 |
| 游记图片上传 | content-trip-service | 已迁移 |
| 手动行程规划 | content-trip-service | 已迁移 |
| 本地 AI 行程规划 | content-trip-service | 已迁移 |
| 价格提醒 | content-trip-service | 已迁移，商品读取暂为本地适配 |
| 订单评价 | content-trip-service | 已迁移，订单读取暂为本地适配 |

B 侧当前可独立编译的模块：

```bash
mvn -pl product-service compile
mvn -pl content-trip-service compile
```

## 3. A 侧优先任务

### 3.1 user-service

角色 A 需要在 user-service 中补齐用户认证和用户基础信息能力。

优先接口：

| 接口 | 用途 |
| --- | --- |
| `POST /api/auth/login` | 登录并返回 JWT 或登录凭证 |
| `POST /api/auth/register` | 用户注册 |
| `GET /api/users/me` | 获取当前登录用户信息 |
| `GET /api/internal/users/basic?ids=1,2,3` | 给 content-trip-service 批量查询用户昵称和头像 |

内部用户基础信息接口建议响应：

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

### 3.2 order-service

角色 A 需要在 order-service 中补齐订单主流程，并向 content-trip-service 提供评价所需的订单上下文。

优先接口：

| 接口 | 用途 |
| --- | --- |
| `POST /api/orders` | 创建订单 |
| `GET /api/orders` | 查询我的订单 |
| `GET /api/orders/{id}` | 查询订单详情 |
| `POST /api/orders/{id}/pay` | 模拟支付 |
| `POST /api/orders/{id}/cancel` | 取消订单 |
| `POST /api/orders/{id}/complete` | 演示用完成订单 |
| `GET /api/internal/orders/{orderId}/review-context?userId=1` | 给评价服务判断单个订单是否可评价 |
| `GET /api/internal/orders/reviewable?userId=1&pageNum=1&pageSize=10` | 给评价服务查询可评价订单 |

订单评价上下文建议响应：

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

评价相关规则建议由两个服务共同承担：

| 规则 | 负责服务 |
| --- | --- |
| 订单是否存在 | order-service |
| 订单是否属于当前用户 | order-service |
| 订单是否已完成 | order-service |
| 订单是否已经评价 | content-trip-service |
| 评价内容是否合法 | content-trip-service |

## 4. 认证与用户上下文

为了让 content-trip-service 和 order-service 不直接解析 JWT，建议由 gateway 或统一认证过滤器完成登录态校验，然后向下游服务透传：

```http
X-User-Id: 1
```

可选透传：

```http
X-Username: zhangsan
X-User-Role: USER
Authorization: Bearer xxx
```

B 侧 content-trip-service 当前已经通过 `CurrentUserProvider` 集中读取 `X-User-Id`。如果 A 最终确认 Header 名称不同，只需要通知 B 修改该适配类，不需要改大量业务代码。

## 5. 网关路由建议

如果 A 负责 gateway，建议先保持前端业务路径不变，只按路径转发到不同服务。

| 路径 | 转发服务 |
| --- | --- |
| `/api/auth/**` | user-service |
| `/api/users/**` | user-service |
| `/api/orders/**` | order-service |
| `/api/public/flights/**` | product-service |
| `/api/public/trains/**` | product-service |
| `/api/public/hotels/**` | product-service |
| `/api/public/tours/**` | product-service |
| `/api/public/price-compare/**` | product-service |
| `/api/public/shares/**` | content-trip-service |
| `/api/public/uploads/**` | content-trip-service |
| `/api/shares/**` | content-trip-service |
| `/api/trip-plans/**` | content-trip-service |
| `/api/price-alerts/**` | content-trip-service |
| `/api/reviews/**` | content-trip-service |
| `/api/orders/reviewable` | content-trip-service |
| `/api/orders/{id}/review` | content-trip-service |

注意：`/api/orders/reviewable` 和 `/api/orders/{id}/review` 是前端评价入口，建议优先转发到 content-trip-service；普通订单路径再转发到 order-service。网关匹配时应保证更具体路径优先。

## 6. 与 B 侧的接口协作点

| 协作点 | A 需要提供 | B 后续动作 |
| --- | --- | --- |
| 当前用户 | 登录后透传 `X-User-Id` | content-trip-service 继续读取该 Header |
| 用户展示 | `/api/internal/users/basic` | 替换游记、评价中的本地用户读取 |
| 订单评价 | `/api/internal/orders/{id}/review-context` | 替换评价模块本地订单读取 |
| 可评价订单 | `/api/internal/orders/reviewable` | 替换可评价订单本地查询 |
| 商品快照 | A 作为 order-service 调用方确认字段 | B 在 product-service 提供快照接口 |

## 7. 建议开发顺序

1. 先保证 user-service、order-service 可以独立启动和编译。
2. 补齐 user-service 登录、注册、当前用户查询。
3. 确定 `X-User-Id` 透传方案。
4. 补齐 order-service 创建订单、查询订单、模拟支付、完成订单。
5. 增加 order-service 评价上下文内部接口。
6. 增加 user-service 用户基础信息批量查询内部接口。
7. 配置 gateway 路由，并保证评价路径优先进入 content-trip-service。
8. 与 B 做联调：登录、商品查询、下单、完成订单、评价、游记、价格提醒。

## 8. 合并前确认

合并前建议 A/B 一起确认以下事项：

| 检查项 | 目标 |
| --- | --- |
| 四个服务都能独立编译 | 避免合并后基础构建失败 |
| common-lib 没有重复定义 | 统一响应和订单常量只保留一份 |
| Header 名称已确认 | 所有需要登录的服务都能拿到当前用户 |
| 内部接口字段已确认 | B 侧可以按契约替换临时本地读取 |
| 网关路由已确认 | 前端不需要大范围改接口路径 |
| 单库演示策略已确认 | 课程展示阶段可以共用数据库，文档中说明后续可拆库 |

## 9. 给角色 A 的结论

角色 A 当前最适合先做 user-service、order-service 和 gateway 的“主链路闭环”：登录后能带上用户身份，下单后订单能进入完成状态，评价服务能通过内部接口确认订单是否可评价。

只要 A 侧先把这些契约稳定下来，B 侧就可以继续把 content-trip-service 中的临时本地读取逐步替换成跨服务调用。这样合并时不会互相卡住，也能保证最终演示流程完整。
