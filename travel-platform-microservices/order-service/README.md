# order-service

订单服务负责订单创建、列表、详情、模拟支付、取消、退款、演示完成及评价上下文。

## 启动前

1. 执行 `src/main/resources/db/order-service-schema.sql`。
2. 通过环境变量配置数据库：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`。
3. 确保 product-service 已在 8102 端口运行；也可用 `PRODUCT_SERVICE_URL` 覆盖地址。

已有单体版 `orders` 表时，按顺序执行 `order-service-v2-migration.sql` 和
`order-service-v3-refund-migration.sql`，不会删除历史订单。

客户端身份暂由网关透传的 `X-User-Id` 请求头提供。创建订单时订单服务调用
product-service 的 `/api/internal/products/snapshot` 内部接口，并保存服务端返回的价格、
名称、摘要和库存快照，不信任前端传入的价格。
