# 成员B产品内容服务迁移记录

## 1. 当前目标

成员 B 负责 `product-service` 和 `content-trip-service` 两条线。本阶段先从产品查询读接口开始，建立一套可复用的迁移模板，再继续迁移酒店、火车票、旅游产品、分享、行程等模块。

## 2. 已完成内容

### 2.1 product-service 航班查询最小闭环

已从原单体项目迁移以下接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/public/flights` | 航班列表查询，支持城市、日期、价格、起飞时间范围和分页 |
| `GET` | `/api/public/flights/{id}` | 航班详情查询 |

已迁移分层：

| 层级 | 文件 |
| --- | --- |
| Controller | `FlightController` |
| Service | `FlightService`、`FlightServiceImpl` |
| Mapper | `FlightMapper` |
| Entity | `Flight`、`BaseEntity` |
| DTO | `FlightQueryRequest` |
| VO | `FlightListItemVO`、`FlightDetailVO` |

### 2.2 公共分页模型

`PageResult` 已迁入 `common-lib`，供产品、内容、订单等服务复用。

### 2.3 product-service 旅游产品查询最小闭环

已从原单体项目迁移以下接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/public/tours` | 旅游产品列表查询，支持目的地筛选和分页 |
| `GET` | `/api/public/tours/{id}` | 旅游产品详情查询 |

已迁移分层：

| 层级 | 文件 |
| --- | --- |
| Controller | `TourController` |
| Service | `TourService`、`TourServiceImpl` |
| Mapper | `TourPackageMapper` |
| Entity | `TourPackage` |
| VO | `TourListItemVO`、`TourDetailVO` |
| Util | `ProductMediaUtils` |

### 2.4 product-service 酒店查询最小闭环

已从原单体项目迁移以下接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/public/hotels` | 酒店列表查询，支持城市筛选和分页，返回最低价与可订房量 |
| `GET` | `/api/public/hotels/{id}` | 酒店详情查询，返回酒店基础信息和可订房型列表 |

已迁移分层：

| 层级 | 文件 |
| --- | --- |
| Controller | `HotelController` |
| Service | `HotelService`、`HotelServiceImpl` |
| Mapper | `HotelMapper`、`HotelRoomMapper` |
| Entity | `Hotel`、`HotelRoom` |
| DTO | `HotelQueryRequest` |
| VO | `HotelListItemVO`、`HotelDetailVO`、`HotelRoomVO` |

### 2.5 product-service 火车票查询最小闭环

已从原单体项目迁移以下接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/public/trains` | 火车票列表查询，支持出发城市、到达城市、出行日期、车次类型、价格区间和分页 |
| `GET` | `/api/public/trains/{id}` | 火车票详情查询，返回车次基础信息和座席价格库存 |

已迁移分层：

| 层级 | 文件 |
| --- | --- |
| Controller | `TrainController` |
| Service | `TrainService`、`TrainServiceImpl` |
| Mapper | `TrainTicketMapper` |
| Entity | `TrainTicket` |
| DTO | `TrainQueryRequest` |
| VO | `TrainListItemVO`、`TrainDetailVO`、`SeatOptionVO` |

### 2.6 product-service 价格比较最小闭环

已从原单体项目迁移以下接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/public/price-compare/hotels/{id}` | 酒店同城同星级价格比较，返回低价标记和可用优惠券 |
| `GET` | `/api/public/price-compare/flights/{id}` | 航班同航线同日期价格比较，返回低价标记和可用优惠券 |
| `GET` | `/api/public/price-compare/tours/{id}` | 旅游产品同目的地相近天数价格比较，返回低价标记和可用优惠券 |

已迁移分层：

| 层级 | 文件 |
| --- | --- |
| Controller | `PriceCompareController` |
| Service | `PriceCompareService`、`PriceCompareServiceImpl` |
| Mapper | `CouponMapper` |
| Entity | `Coupon` |
| VO | `PriceCompareVO`、`CompareItemVO`、`CouponVO` |

说明：`PriceCompareServiceImpl` 保留 `ProductSnapshot` 和产品类型规范化能力，供后续 `PriceAlertController` 迁移时复用。

### 2.7 content-trip-service 公开分享查询最小闭环

已从原单体项目迁移以下接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/public/shares` | 公开分享列表查询，支持分页，返回封面、作者和图片数量 |
| `GET` | `/api/public/shares/{id}` | 公开分享详情查询，返回正文、作者和图片列表，并累加浏览量 |

已迁移分层：

| 层级 | 文件 |
| --- | --- |
| Controller | `ShareController` |
| Service | `ShareService`、`ShareServiceImpl` |
| Mapper | `SharePostMapper`、`ShareImageMapper`、`UserMapper` |
| Entity | `SharePost`、`ShareImage`、`User`、`BaseEntity` |
| VO | `SharePostListItemVO`、`SharePostDetailVO` |

说明：本阶段只迁移公开读接口。发布分享、上传图片、我的分享列表依赖登录态与上传能力，待成员 A 的用户认证公共能力稳定后再接入。

### 2.8 content-trip-service 分享发布与我的分享闭环

已从原单体项目迁移以下接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/shares/upload` | 上传分享图片，返回公开访问 URL |
| `POST` | `/api/shares` | 发布分享，写入分享正文和图片列表 |
| `GET` | `/api/shares/mine` | 查询当前用户分享列表 |

已迁移分层：

| 层级 | 文件 |
| --- | --- |
| Controller | `ShareController` |
| Service | `ShareService`、`ShareServiceImpl`、`MediaUploadService`、`MediaUploadServiceImpl` |
| DTO | `SharePostCreateRequest` |
| Config | `WebMvcConfig` |

说明：私有分享接口当前复用 `CurrentUserProvider`，通过请求头 `X-User-Id` 获取当前用户。上传文件保存到 `travel.upload-dir`，并通过 `/api/public/uploads/**` 公开访问。

### 2.9 content-trip-service 手动行程计划最小闭环

已从原单体项目迁移以下接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/trip-plans` | 查询当前用户行程计划列表 |
| `POST` | `/api/trip-plans` | 创建手动行程计划 |
| `GET` | `/api/trip-plans/{id}` | 查询当前用户行程计划详情 |
| `PUT` | `/api/trip-plans/{id}` | 更新当前用户行程计划 |
| `DELETE` | `/api/trip-plans/{id}` | 删除当前用户行程计划及每日安排 |
| `POST` | `/api/trip-plans/{id}/items` | 新增每日安排 |
| `PUT` | `/api/trip-plans/{planId}/items/{itemId}` | 更新每日安排 |
| `DELETE` | `/api/trip-plans/{planId}/items/{itemId}` | 删除每日安排 |

已迁移分层：

| 层级 | 文件 |
| --- | --- |
| Controller | `TripPlanController` |
| Service | `TripPlanService`、`TripPlanServiceImpl` |
| Mapper | `TripPlanMapper`、`TripPlanItemMapper` |
| Entity | `TripPlan`、`TripPlanItem` |
| DTO | `TripPlanCreateRequest`、`TripPlanUpdateRequest`、`TripPlanItemCreateRequest`、`TripPlanItemUpdateRequest` |
| VO | `TripPlanListItemVO`、`TripPlanDetailVO`、`TripPlanItemVO` |
| Auth Adapter | `CurrentUserProvider` |

说明：当前用户身份先通过请求头 `X-User-Id` 获取，后续接入网关/JWT 后只需替换 `CurrentUserProvider`。本阶段不迁移 AI 行程预览和 AI 行程保存。

### 2.10 content-trip-service AI 行程本地生成闭环

已从原单体项目迁移以下接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/trip-plans/ai-preview` | 基于本地景点库生成 AI 行程预览 |
| `POST` | `/api/trip-plans/ai-save` | 将 AI 行程预览保存到当前用户行程计划 |

已迁移分层：

| 层级 | 文件 |
| --- | --- |
| Controller | `TripPlanController` |
| Service | `AiTripPlanService`、`AiTripPlanServiceImpl` |
| Mapper | `AttractionMapper` |
| Entity | `Attraction` |
| DTO | `AiTripPlanPreviewRequest`、`AiTripPlanSaveRequest`、`AiTripPlanSaveDayRequest` |
| VO | `AiTripPlanPreviewVO`、`AiTripPlanPreviewDayVO`、`AiTripPlanAttractionVO` |

说明：当前迁移采用本地景点库规则生成，`generationMode` 固定为 `LOCAL_FALLBACK`，不接真实外部 AI API，保证课程演示环境稳定。

### 2.11 数据库配置

`product-service` 已增加 MyBatis-Plus 和 MySQL 驱动依赖，并配置默认数据库：

```text
travel_product
```

`content-trip-service` 已增加 MyBatis-Plus 和 MySQL 驱动依赖，并配置默认数据库：

```text
travel_content_trip
```

如本地仍使用单体数据库演示，可临时把 `product-service/src/main/resources/application.yml` 中的数据库名改为：

```text
travel_platform
```

`content-trip-service` 本地演示时同理可临时指向：

```text
travel_platform
```

## 3. 后续迁移顺序

建议成员 B 按以下顺序继续：

1. 迁移 `ReviewController` 和 `PriceAlertController`，需要等待订单服务和产品内部快照接口稳定。
2. 梳理内容服务与用户服务的用户展示信息同步方式，逐步替代跨库读取 `user` 表。

## 4. 成员B独立开发边界

成员 B 可以独立修改：

| 服务 | 可修改内容 |
| --- | --- |
| `product-service` | 产品查询、产品管理、价格对比、商品快照、库存接口 |
| `content-trip-service` | 分享、行程、评价、价格提醒 |
| `common-lib` | 只新增跨服务共用的 VO/DTO/常量，修改前需同步成员 A |

成员 B 暂不修改：

| 服务 | 原因 |
| --- | --- |
| `user-service` | 由成员 A 负责用户和认证 |
| `order-service` | 由成员 A 负责订单和支付状态流转 |

## 5. 本阶段验收方式

编译验证：

```bash
mvn install -N
mvn -pl common-lib install
mvn -pl product-service compile
```

接口验证：

```text
GET http://localhost:8102/api/public/health
GET http://localhost:8102/api/public/flights
GET http://localhost:8102/api/public/flights/1
GET http://localhost:8102/api/public/tours
GET http://localhost:8102/api/public/tours/1
GET http://localhost:8102/api/public/hotels
GET http://localhost:8102/api/public/hotels/1
GET http://localhost:8102/api/public/trains
GET http://localhost:8102/api/public/trains/1
GET http://localhost:8102/api/public/price-compare/hotels/1
GET http://localhost:8102/api/public/price-compare/flights/1
GET http://localhost:8102/api/public/price-compare/tours/1
GET http://localhost:8104/api/public/health
GET http://localhost:8104/api/public/shares
GET http://localhost:8104/api/public/shares/1
POST http://localhost:8104/api/shares/upload
POST http://localhost:8104/api/shares
GET http://localhost:8104/api/shares/mine
GET http://localhost:8104/api/trip-plans
POST http://localhost:8104/api/trip-plans
GET http://localhost:8104/api/trip-plans/1
PUT http://localhost:8104/api/trip-plans/1
DELETE http://localhost:8104/api/trip-plans/1
POST http://localhost:8104/api/trip-plans/ai-preview
POST http://localhost:8104/api/trip-plans/ai-save
```

调用 `/api/trip-plans` 私有接口时，需在请求头临时传入：

```text
X-User-Id: 1
```
