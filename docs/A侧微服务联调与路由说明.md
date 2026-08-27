# A 侧微服务联调与路由说明

## 1. 当前完成范围

- `user-service`：注册、登录、退出、个人资料、常用出行人、内部用户基础信息、后台用户管理。
- `order-service`：创建、列表、详情、取消、模拟支付、模拟退款、演示完成、评价上下文。
- 订单创建通过 `ProductSnapshotClient` 调用 `product-service` 的商品详情接口，并将名称、摘要和价格保存为订单快照。

## 2. 本地端口与配置

| 服务 | 端口 | 数据库 |
| --- | ---: | --- |
| user-service | 8101 | travel_user |
| product-service | 8102 | travel_product |
| order-service | 8103 | travel_order |
| content-trip-service | 8104 | travel_content_trip |

订单服务数据库配置优先读取 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`，其他服务也可用
Spring Boot 启动参数覆盖数据源配置。订单服务通过 `PRODUCT_SERVICE_URL` 指定商品服务地址，
默认是 `http://localhost:8102`。

## 3. 网关路由预留

前端继续统一请求 `/api`，网关按路径转发：

| 路径 | 目标服务 |
| --- | --- |
| `/api/auth/**`、`/api/users/**`、`/api/user-contacts/**` | user-service |
| `/api/admin/users/**`、`/api/admin/roles` | user-service |
| `/api/public/flights/**`、`/api/public/trains/**`、`/api/public/hotels/**`、`/api/public/tours/**` | product-service |
| `/api/orders/**`、`/api/internal/orders/**` | order-service |
| 攻略、游记、评价、收藏、行程相关路径 | content-trip-service |

网关完成 JWT 校验后，需要把用户 ID 写入 `X-User-Id` 再转发给订单服务和内容行程服务。
当前没有网关模块时，可在直接调用 8103/8104 接口时手动传该请求头进行联调。

## 4. 调用失败策略

- 商品服务不可用或商品信息不完整时，订单创建失败并提示稍后重试，不使用客户端价格或过期价格兜底。
- 商品下架或库存小于购买数量时，订单不落库。
- 重复支付、重复退款、已完成订单取消等非法状态流转返回业务错误，不重复修改订单。
- 跨用户读取订单返回 403；用户后台接口仅允许 `ROLE_ADMIN`。
- 内部接口应只由网关或服务网络访问，部署时不可直接暴露到公网。

## 5. 数据库初始化

- 新订单库执行 `order-service-schema.sql`。
- 从单体版订单表升级时，依次执行 `order-service-v2-migration.sql`、`order-service-v3-refund-migration.sql`。
- 常用出行人表执行 user-service 下的 `user-contact-schema.sql`。

这些脚本不删除原单体数据库，也不删除历史订单。
