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

### 2.4 数据库配置

`product-service` 已增加 MyBatis-Plus 和 MySQL 驱动依赖，并配置默认数据库：

```text
travel_product
```

如本地仍使用单体数据库演示，可临时把 `product-service/src/main/resources/application.yml` 中的数据库名改为：

```text
travel_platform
```

## 3. 后续迁移顺序

建议成员 B 按以下顺序继续：

1. 迁移 `HotelController`、`HotelService`、`Hotel`、`HotelRoom`，完成酒店列表和详情。
2. 迁移 `TrainController`、`TrainService`、`TrainTicket`，完成火车票列表和详情。
3. 迁移 `PriceCompareController`，保留在 `product-service` 内部聚合价格比较。
4. 迁移 `ShareController` 到 `content-trip-service`，先完成公开分享列表和详情。
5. 迁移 `TripPlanController` 到 `content-trip-service`，登录态依赖由成员 A 的用户服务公共认证能力确定后再接入。
6. 迁移 `ReviewController` 和 `PriceAlertController`，需要等待订单服务和产品内部快照接口稳定。

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
```
