CREATE TABLE IF NOT EXISTS `role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `role_code` VARCHAR(50) NOT NULL,
    `role_name` VARCHAR(50) NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(100) NOT NULL,
    `nickname` VARCHAR(50) NOT NULL,
    `real_name` VARCHAR(50) DEFAULT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `email` VARCHAR(100) DEFAULT NULL,
    `gender` TINYINT DEFAULT NULL,
    `avatar` VARCHAR(255) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `last_login_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    UNIQUE KEY `uk_user_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_contact` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `phone` VARCHAR(20) NOT NULL,
    `id_card` VARCHAR(30) NOT NULL,
    `contact_type` TINYINT NOT NULL DEFAULT 1,
    `is_default` TINYINT NOT NULL DEFAULT 0,
    `remark` VARCHAR(100) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_contact_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `flight` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `flight_no` VARCHAR(30) NOT NULL,
    `airline_name` VARCHAR(100) NOT NULL,
    `departure_city` VARCHAR(50) NOT NULL,
    `arrival_city` VARCHAR(50) NOT NULL,
    `departure_airport` VARCHAR(100) NOT NULL,
    `arrival_airport` VARCHAR(100) NOT NULL,
    `departure_time` DATETIME NOT NULL,
    `arrival_time` DATETIME NOT NULL,
    `price` DECIMAL(10, 2) NOT NULL,
    `stock` INT NOT NULL DEFAULT 0,
    `cabin_class` VARCHAR(50) NOT NULL DEFAULT '经济舱',
    `baggage_policy` VARCHAR(255) DEFAULT NULL,
    `refund_policy` VARCHAR(255) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_flight_route_date` (`departure_city`, `arrival_city`, `departure_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_no` VARCHAR(50) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `biz_type` VARCHAR(30) NOT NULL,
    `biz_id` BIGINT NOT NULL,
    `order_status` TINYINT NOT NULL,
    `original_amount` DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    `discount_amount` DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    `total_amount` DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    `coupon_id` BIGINT DEFAULT NULL,
    `coupon_name` VARCHAR(100) DEFAULT NULL,
    `contact_name` VARCHAR(50) NOT NULL,
    `contact_phone` VARCHAR(20) NOT NULL,
    `travel_date` DATE NOT NULL,
    `remark` VARCHAR(255) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_orders_order_no` (`order_no`),
    KEY `idx_orders_user_id` (`user_id`),
    KEY `idx_orders_biz_type` (`biz_type`),
    KEY `idx_orders_user_query` (`user_id`, `biz_type`, `order_status`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @orders_original_amount_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'original_amount'
);
SET @orders_original_amount_sql = IF(
    @orders_original_amount_exists = 0,
    'ALTER TABLE `orders` ADD COLUMN `original_amount` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 AFTER `order_status`',
    'SELECT 1'
);
PREPARE orders_original_amount_stmt FROM @orders_original_amount_sql;
EXECUTE orders_original_amount_stmt;
DEALLOCATE PREPARE orders_original_amount_stmt;

SET @orders_discount_amount_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'discount_amount'
);
SET @orders_discount_amount_sql = IF(
    @orders_discount_amount_exists = 0,
    'ALTER TABLE `orders` ADD COLUMN `discount_amount` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 AFTER `original_amount`',
    'SELECT 1'
);
PREPARE orders_discount_amount_stmt FROM @orders_discount_amount_sql;
EXECUTE orders_discount_amount_stmt;
DEALLOCATE PREPARE orders_discount_amount_stmt;

SET @orders_coupon_id_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'coupon_id'
);
SET @orders_coupon_id_sql = IF(
    @orders_coupon_id_exists = 0,
    'ALTER TABLE `orders` ADD COLUMN `coupon_id` BIGINT DEFAULT NULL AFTER `total_amount`',
    'SELECT 1'
);
PREPARE orders_coupon_id_stmt FROM @orders_coupon_id_sql;
EXECUTE orders_coupon_id_stmt;
DEALLOCATE PREPARE orders_coupon_id_stmt;

SET @orders_coupon_name_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'coupon_name'
);
SET @orders_coupon_name_sql = IF(
    @orders_coupon_name_exists = 0,
    'ALTER TABLE `orders` ADD COLUMN `coupon_name` VARCHAR(100) DEFAULT NULL AFTER `coupon_id`',
    'SELECT 1'
);
PREPARE orders_coupon_name_stmt FROM @orders_coupon_name_sql;
EXECUTE orders_coupon_name_stmt;
DEALLOCATE PREPARE orders_coupon_name_stmt;

CREATE TABLE IF NOT EXISTS `order_flight` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL,
    `flight_id` BIGINT NOT NULL,
    `flight_no` VARCHAR(30) NOT NULL,
    `airline_name` VARCHAR(100) NOT NULL,
    `departure_city` VARCHAR(50) NOT NULL,
    `arrival_city` VARCHAR(50) NOT NULL,
    `departure_airport` VARCHAR(100) NOT NULL,
    `arrival_airport` VARCHAR(100) NOT NULL,
    `departure_time` DATETIME NOT NULL,
    `arrival_time` DATETIME NOT NULL,
    `passenger_name` VARCHAR(50) NOT NULL,
    `passenger_phone` VARCHAR(20) NOT NULL,
    `passenger_id_card` VARCHAR(30) NOT NULL,
    `ticket_price` DECIMAL(10, 2) NOT NULL,
    `status` TINYINT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_flight_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `train_ticket` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `train_no` VARCHAR(30) NOT NULL,
    `train_type` VARCHAR(30) NOT NULL,
    `departure_city` VARCHAR(50) NOT NULL,
    `arrival_city` VARCHAR(50) NOT NULL,
    `departure_station` VARCHAR(100) NOT NULL,
    `arrival_station` VARCHAR(100) NOT NULL,
    `departure_time` DATETIME NOT NULL,
    `arrival_time` DATETIME NOT NULL,
    `duration_minutes` INT NOT NULL,
    `business_price` DECIMAL(10, 2) DEFAULT NULL,
    `first_class_price` DECIMAL(10, 2) DEFAULT NULL,
    `second_class_price` DECIMAL(10, 2) DEFAULT NULL,
    `business_stock` INT NOT NULL DEFAULT 0,
    `first_class_stock` INT NOT NULL DEFAULT 0,
    `second_class_stock` INT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_train_route_date` (`departure_city`, `arrival_city`, `departure_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `order_train` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL,
    `train_ticket_id` BIGINT NOT NULL,
    `train_no` VARCHAR(30) NOT NULL,
    `train_type` VARCHAR(30) NOT NULL,
    `departure_city` VARCHAR(50) NOT NULL,
    `arrival_city` VARCHAR(50) NOT NULL,
    `departure_station` VARCHAR(100) NOT NULL,
    `arrival_station` VARCHAR(100) NOT NULL,
    `departure_time` DATETIME NOT NULL,
    `arrival_time` DATETIME NOT NULL,
    `seat_type` VARCHAR(30) NOT NULL,
    `seat_price` DECIMAL(10, 2) NOT NULL,
    `passenger_name` VARCHAR(50) NOT NULL,
    `passenger_phone` VARCHAR(20) NOT NULL,
    `passenger_id_card` VARCHAR(30) NOT NULL,
    `status` TINYINT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_train_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `hotel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `hotel_name` VARCHAR(100) NOT NULL,
    `city` VARCHAR(50) NOT NULL,
    `district` VARCHAR(50) DEFAULT NULL,
    `address` VARCHAR(255) NOT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `star_level` INT NOT NULL DEFAULT 3,
    `cover_image` VARCHAR(255) DEFAULT NULL,
    `check_in_time` VARCHAR(20) DEFAULT '14:00',
    `check_out_time` VARCHAR(20) DEFAULT '12:00',
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_hotel_city_status` (`city`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `hotel_room` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `hotel_id` BIGINT NOT NULL,
    `room_name` VARCHAR(100) NOT NULL,
    `bed_type` VARCHAR(50) NOT NULL,
    `breakfast` VARCHAR(50) DEFAULT NULL,
    `room_area` VARCHAR(50) DEFAULT NULL,
    `guest_count` INT NOT NULL DEFAULT 2,
    `price` DECIMAL(10, 2) NOT NULL,
    `stock` INT NOT NULL DEFAULT 0,
    `cancel_rule` VARCHAR(255) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_hotel_room_hotel_id` (`hotel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `order_hotel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL,
    `hotel_id` BIGINT NOT NULL,
    `hotel_room_id` BIGINT NOT NULL,
    `hotel_name` VARCHAR(100) NOT NULL,
    `city` VARCHAR(50) NOT NULL,
    `address` VARCHAR(255) NOT NULL,
    `room_name` VARCHAR(100) NOT NULL,
    `bed_type` VARCHAR(50) NOT NULL,
    `breakfast` VARCHAR(50) DEFAULT NULL,
    `check_in_date` DATE NOT NULL,
    `check_out_date` DATE NOT NULL,
    `guest_name` VARCHAR(50) NOT NULL,
    `guest_phone` VARCHAR(20) NOT NULL,
    `guest_id_card` VARCHAR(30) NOT NULL,
    `room_price` DECIMAL(10, 2) NOT NULL,
    `night_count` INT NOT NULL,
    `status` TINYINT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_hotel_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tour_package` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `package_name` VARCHAR(100) NOT NULL,
    `destination` VARCHAR(100) NOT NULL,
    `departure_city` VARCHAR(50) DEFAULT NULL,
    `days` INT NOT NULL,
    `price` DECIMAL(10, 2) NOT NULL,
    `stock` INT NOT NULL DEFAULT 0,
    `travel_dates` VARCHAR(255) DEFAULT NULL,
    `description` VARCHAR(1000) DEFAULT NULL,
    `cover_image` VARCHAR(255) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_tour_destination_status` (`destination`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `trip_plan` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `plan_name` VARCHAR(100) NOT NULL,
    `total_days` INT NOT NULL,
    `start_date` DATE DEFAULT NULL,
    `remark` VARCHAR(255) DEFAULT NULL,
    `source_type` VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_trip_plan_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `trip_plan_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `plan_id` BIGINT NOT NULL,
    `day_no` INT NOT NULL,
    `destination` VARCHAR(100) NOT NULL,
    `hotel` VARCHAR(100) DEFAULT NULL,
    `transport_type` VARCHAR(50) DEFAULT NULL,
    `remark` VARCHAR(255) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trip_plan_day` (`plan_id`, `day_no`),
    KEY `idx_trip_plan_item_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `order_tour` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL,
    `tour_package_id` BIGINT NOT NULL,
    `package_name` VARCHAR(100) NOT NULL,
    `destination` VARCHAR(100) NOT NULL,
    `departure_city` VARCHAR(50) DEFAULT NULL,
    `travel_date` DATE NOT NULL,
    `days` INT NOT NULL,
    `guest_name` VARCHAR(50) NOT NULL,
    `guest_phone` VARCHAR(20) NOT NULL,
    `guest_id_card` VARCHAR(30) NOT NULL,
    `package_price` DECIMAL(10, 2) NOT NULL,
    `status` TINYINT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_tour_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `role` (`role_code`, `role_name`, `status`)
SELECT 'ROLE_USER', '普通用户', 1
WHERE NOT EXISTS (
    SELECT 1 FROM `role` WHERE `role_code` = 'ROLE_USER'
);

INSERT INTO `role` (`role_code`, `role_name`, `status`)
SELECT 'ROLE_ADMIN', '管理员', 1
WHERE NOT EXISTS (
    SELECT 1 FROM `role` WHERE `role_code` = 'ROLE_ADMIN'
);

INSERT INTO `user` (`username`, `password`, `nickname`, `phone`, `status`)
SELECT 'demo_user', '$2a$10$ujhAXWqWhkHyzQIC5ywpjuBNnShqqvIj4b3hWe3BShQHWvJyrfPvu', '演示用户', '13800000000', 1
WHERE NOT EXISTS (
    SELECT 1 FROM `user` WHERE `username` = 'demo_user'
);

INSERT INTO `user` (`username`, `password`, `nickname`, `phone`, `status`)
SELECT 'admin', '$2a$10$RwUGSDk/j9fnReTZnKYcdO8vn0yZyL0z5pQ0a6GnsHVKu/9xCxV/.', '系统管理员', '13900000000', 1
WHERE NOT EXISTS (
    SELECT 1 FROM `user` WHERE `username` = 'admin'
);

INSERT INTO `user_role` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `user` u
JOIN `role` r ON r.role_code = 'ROLE_USER'
WHERE u.username = 'demo_user'
  AND NOT EXISTS (
      SELECT 1
      FROM `user_role` ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO `user_role` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `user` u
JOIN `role` r ON r.role_code = 'ROLE_USER'
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM `user_role` ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO `user_role` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `user` u
JOIN `role` r ON r.role_code = 'ROLE_ADMIN'
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM `user_role` ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO `user_contact` (`user_id`, `name`, `phone`, `id_card`, `contact_type`, `is_default`, `remark`)
SELECT u.id, '张三', '13800000000', '310101199001011234', 1, 1, '演示乘机人'
FROM `user` u
WHERE u.username = 'demo_user'
  AND NOT EXISTS (
      SELECT 1 FROM `user_contact` uc WHERE uc.user_id = u.id AND uc.phone = '13800000000'
  );

INSERT INTO `flight` (`flight_no`, `airline_name`, `departure_city`, `arrival_city`, `departure_airport`, `arrival_airport`, `departure_time`, `arrival_time`, `price`, `stock`, `cabin_class`, `baggage_policy`, `refund_policy`, `status`)
SELECT 'MU5101', '东方航空', '上海', '北京', '上海虹桥T2', '北京首都T2', '2026-05-01 08:20:00', '2026-05-01 10:35:00', 880.00, 28, '经济舱', '免费托运20KG', '起飞前可退改签，收取手续费', 1
WHERE NOT EXISTS (SELECT 1 FROM `flight` WHERE `flight_no` = 'MU5101' AND `departure_time` = '2026-05-01 08:20:00');

INSERT INTO `flight` (`flight_no`, `airline_name`, `departure_city`, `arrival_city`, `departure_airport`, `arrival_airport`, `departure_time`, `arrival_time`, `price`, `stock`, `cabin_class`, `baggage_policy`, `refund_policy`, `status`)
SELECT 'CA1832', '中国国航', '上海', '北京', '上海浦东T1', '北京首都T3', '2026-05-01 14:10:00', '2026-05-01 16:30:00', 960.00, 16, '经济舱', '免费托运20KG', '起飞前可退改签，收取手续费', 1
WHERE NOT EXISTS (SELECT 1 FROM `flight` WHERE `flight_no` = 'CA1832' AND `departure_time` = '2026-05-01 14:10:00');

INSERT INTO `flight` (`flight_no`, `airline_name`, `departure_city`, `arrival_city`, `departure_airport`, `arrival_airport`, `departure_time`, `arrival_time`, `price`, `stock`, `cabin_class`, `baggage_policy`, `refund_policy`, `status`)
SELECT 'CZ6107', '南方航空', '广州', '成都', '广州白云T2', '成都天府T2', '2026-05-02 09:00:00', '2026-05-02 11:35:00', 720.00, 22, '经济舱', '免费托运20KG', '起飞前可退改签，收取手续费', 1
WHERE NOT EXISTS (SELECT 1 FROM `flight` WHERE `flight_no` = 'CZ6107' AND `departure_time` = '2026-05-02 09:00:00');

INSERT INTO `flight` (`flight_no`, `airline_name`, `departure_city`, `arrival_city`, `departure_airport`, `arrival_airport`, `departure_time`, `arrival_time`, `price`, `stock`, `cabin_class`, `baggage_policy`, `refund_policy`, `status`)
SELECT 'HU7603', '海南航空', '北京', '深圳', '北京首都T1', '深圳宝安T3', '2026-05-03 18:45:00', '2026-05-03 22:00:00', 1080.00, 12, '经济舱', '免费托运20KG', '起飞前可退改签，收取手续费', 1
WHERE NOT EXISTS (SELECT 1 FROM `flight` WHERE `flight_no` = 'HU7603' AND `departure_time` = '2026-05-03 18:45:00');

INSERT INTO `train_ticket` (`train_no`, `train_type`, `departure_city`, `arrival_city`, `departure_station`, `arrival_station`, `departure_time`, `arrival_time`, `duration_minutes`, `business_price`, `first_class_price`, `second_class_price`, `business_stock`, `first_class_stock`, `second_class_stock`, `status`)
SELECT 'G101', '高铁', '上海', '北京', '上海虹桥', '北京南', '2026-05-06 07:00:00', '2026-05-06 12:38:00', 338, 1748.00, 933.00, 553.00, 8, 16, 42, 1
WHERE NOT EXISTS (SELECT 1 FROM `train_ticket` WHERE `train_no` = 'G101' AND `departure_time` = '2026-05-06 07:00:00');

INSERT INTO `train_ticket` (`train_no`, `train_type`, `departure_city`, `arrival_city`, `departure_station`, `arrival_station`, `departure_time`, `arrival_time`, `duration_minutes`, `business_price`, `first_class_price`, `second_class_price`, `business_stock`, `first_class_stock`, `second_class_stock`, `status`)
SELECT 'G215', '高铁', '上海', '杭州', '上海虹桥', '杭州东', '2026-05-06 09:12:00', '2026-05-06 10:05:00', 53, 298.00, 156.00, 92.00, 10, 28, 65, 1
WHERE NOT EXISTS (SELECT 1 FROM `train_ticket` WHERE `train_no` = 'G215' AND `departure_time` = '2026-05-06 09:12:00');

INSERT INTO `train_ticket` (`train_no`, `train_type`, `departure_city`, `arrival_city`, `departure_station`, `arrival_station`, `departure_time`, `arrival_time`, `duration_minutes`, `business_price`, `first_class_price`, `second_class_price`, `business_stock`, `first_class_stock`, `second_class_stock`, `status`)
SELECT 'D3021', '动车', '广州', '深圳', '广州东', '深圳北', '2026-05-07 08:20:00', '2026-05-07 09:48:00', 88, 268.00, 139.00, 89.00, 12, 24, 80, 1
WHERE NOT EXISTS (SELECT 1 FROM `train_ticket` WHERE `train_no` = 'D3021' AND `departure_time` = '2026-05-07 08:20:00');

INSERT INTO `train_ticket` (`train_no`, `train_type`, `departure_city`, `arrival_city`, `departure_station`, `arrival_station`, `departure_time`, `arrival_time`, `duration_minutes`, `business_price`, `first_class_price`, `second_class_price`, `business_stock`, `first_class_stock`, `second_class_stock`, `status`)
SELECT 'K456', '快速', '成都', '西安', '成都西', '西安', '2026-05-08 13:35:00', '2026-05-08 21:20:00', 465, 0.00, 0.00, 243.00, 0, 0, 96, 1
WHERE NOT EXISTS (SELECT 1 FROM `train_ticket` WHERE `train_no` = 'K456' AND `departure_time` = '2026-05-08 13:35:00');

INSERT INTO `train_ticket` (`train_no`, `train_type`, `departure_city`, `arrival_city`, `departure_station`, `arrival_station`, `departure_time`, `arrival_time`, `duration_minutes`, `business_price`, `first_class_price`, `second_class_price`, `business_stock`, `first_class_stock`, `second_class_stock`, `status`)
SELECT 'Z98', '直达', '北京', '广州', '北京西', '广州', '2026-05-09 19:50:00', '2026-05-10 06:32:00', 642, 0.00, 0.00, 468.00, 0, 0, 88, 1
WHERE NOT EXISTS (SELECT 1 FROM `train_ticket` WHERE `train_no` = 'Z98' AND `departure_time` = '2026-05-09 19:50:00');

INSERT INTO `hotel` (`hotel_name`, `city`, `district`, `address`, `description`, `star_level`, `cover_image`, `check_in_time`, `check_out_time`, `status`)
SELECT '上海外滩云景酒店', '上海', '黄浦区', '上海市黄浦区中山东二路88号', '靠近外滩与南京东路，适合城市观光与商务出行。', 5, '', '14:00', '12:00', 1
WHERE NOT EXISTS (SELECT 1 FROM `hotel` WHERE `hotel_name` = '上海外滩云景酒店');

INSERT INTO `hotel` (`hotel_name`, `city`, `district`, `address`, `description`, `star_level`, `cover_image`, `check_in_time`, `check_out_time`, `status`)
SELECT '北京国贸商务酒店', '北京', '朝阳区', '北京市朝阳区建国门外大街66号', '临近国贸商圈，交通便利，适合商务差旅。', 4, '', '14:00', '12:00', 1
WHERE NOT EXISTS (SELECT 1 FROM `hotel` WHERE `hotel_name` = '北京国贸商务酒店');

INSERT INTO `hotel` (`hotel_name`, `city`, `district`, `address`, `description`, `star_level`, `cover_image`, `check_in_time`, `check_out_time`, `status`)
SELECT '杭州西湖雅居民宿酒店', '杭州', '西湖区', '杭州市西湖区灵隐路18号', '靠近西湖景区，适合周末度假和家庭出游。', 4, '', '15:00', '12:00', 1
WHERE NOT EXISTS (SELECT 1 FROM `hotel` WHERE `hotel_name` = '杭州西湖雅居民宿酒店');

INSERT INTO `hotel_room` (`hotel_id`, `room_name`, `bed_type`, `breakfast`, `room_area`, `guest_count`, `price`, `stock`, `cancel_rule`, `status`)
SELECT h.id, '高级大床房', '1张大床', '双早', '32㎡', 2, 688.00, 12, '入住前一天18:00前可免费取消', 1
FROM `hotel` h
WHERE h.hotel_name = '上海外滩云景酒店'
  AND NOT EXISTS (SELECT 1 FROM `hotel_room` r WHERE r.hotel_id = h.id AND r.room_name = '高级大床房');

INSERT INTO `hotel_room` (`hotel_id`, `room_name`, `bed_type`, `breakfast`, `room_area`, `guest_count`, `price`, `stock`, `cancel_rule`, `status`)
SELECT h.id, '江景双床房', '2张单床', '双早', '40㎡', 2, 888.00, 8, '入住前一天18:00前可免费取消', 1
FROM `hotel` h
WHERE h.hotel_name = '上海外滩云景酒店'
  AND NOT EXISTS (SELECT 1 FROM `hotel_room` r WHERE r.hotel_id = h.id AND r.room_name = '江景双床房');

INSERT INTO `hotel_room` (`hotel_id`, `room_name`, `bed_type`, `breakfast`, `room_area`, `guest_count`, `price`, `stock`, `cancel_rule`, `status`)
SELECT h.id, '商务标准间', '2张单床', '双早', '30㎡', 2, 568.00, 15, '入住前一天18:00前可免费取消', 1
FROM `hotel` h
WHERE h.hotel_name = '北京国贸商务酒店'
  AND NOT EXISTS (SELECT 1 FROM `hotel_room` r WHERE r.hotel_id = h.id AND r.room_name = '商务标准间');

INSERT INTO `hotel_room` (`hotel_id`, `room_name`, `bed_type`, `breakfast`, `room_area`, `guest_count`, `price`, `stock`, `cancel_rule`, `status`)
SELECT h.id, '行政大床房', '1张大床', '双早', '38㎡', 2, 768.00, 10, '入住前一天18:00前可免费取消', 1
FROM `hotel` h
WHERE h.hotel_name = '北京国贸商务酒店'
  AND NOT EXISTS (SELECT 1 FROM `hotel_room` r WHERE r.hotel_id = h.id AND r.room_name = '行政大床房');

INSERT INTO `hotel_room` (`hotel_id`, `room_name`, `bed_type`, `breakfast`, `room_area`, `guest_count`, `price`, `stock`, `cancel_rule`, `status`)
SELECT h.id, '园景大床房', '1张大床', '双早', '28㎡', 2, 458.00, 18, '入住前一天18:00前可免费取消', 1
FROM `hotel` h
WHERE h.hotel_name = '杭州西湖雅居民宿酒店'
  AND NOT EXISTS (SELECT 1 FROM `hotel_room` r WHERE r.hotel_id = h.id AND r.room_name = '园景大床房');

INSERT INTO `hotel_room` (`hotel_id`, `room_name`, `bed_type`, `breakfast`, `room_area`, `guest_count`, `price`, `stock`, `cancel_rule`, `status`)
SELECT h.id, '家庭套房', '1张大床+1张单床', '三早', '48㎡', 3, 698.00, 6, '入住前一天18:00前可免费取消', 1
FROM `hotel` h
WHERE h.hotel_name = '杭州西湖雅居民宿酒店'
  AND NOT EXISTS (SELECT 1 FROM `hotel_room` r WHERE r.hotel_id = h.id AND r.room_name = '家庭套房');

INSERT INTO `tour_package` (`package_name`, `destination`, `departure_city`, `days`, `price`, `stock`, `travel_dates`, `description`, `cover_image`, `status`)
SELECT '三亚海岛度假5日游', '三亚', '上海', 5, 3280.00, 20, '2026-06-01,2026-06-08,2026-06-15', '包含酒店住宿、景点门票和接送机服务，适合家庭与情侣度假。', '', 1
WHERE NOT EXISTS (SELECT 1 FROM `tour_package` WHERE `package_name` = '三亚海岛度假5日游');

INSERT INTO `tour_package` (`package_name`, `destination`, `departure_city`, `days`, `price`, `stock`, `travel_dates`, `description`, `cover_image`, `status`)
SELECT '云南丽江大理6日游', '云南丽江大理', '北京', 6, 3680.00, 18, '2026-06-03,2026-06-10,2026-06-17', '经典双城线路，覆盖古城、洱海与雪山景观，适合毕业旅行与朋友出行。', '', 1
WHERE NOT EXISTS (SELECT 1 FROM `tour_package` WHERE `package_name` = '云南丽江大理6日游');

INSERT INTO `tour_package` (`package_name`, `destination`, `departure_city`, `days`, `price`, `stock`, `travel_dates`, `description`, `cover_image`, `status`)
SELECT '张家界山水风景4日游', '张家界', '广州', 4, 2580.00, 16, '2026-06-05,2026-06-12,2026-06-19', '主打自然景观与轻徒步体验，包含核心景区门票与舒适型酒店。', '', 1
WHERE NOT EXISTS (SELECT 1 FROM `tour_package` WHERE `package_name` = '张家界山水风景4日游');
CREATE TABLE IF NOT EXISTS `coupon` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `coupon_name` VARCHAR(100) NOT NULL,
    `product_type` VARCHAR(30) NOT NULL,
    `threshold_amount` DECIMAL(10, 2) DEFAULT NULL,
    `discount_amount` DECIMAL(10, 2) NOT NULL,
    `discount_type` VARCHAR(30) NOT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `start_time` DATETIME NOT NULL,
    `end_time` DATETIME NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_coupon_type_status` (`product_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `price_alert` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `product_type` VARCHAR(30) NOT NULL,
    `product_id` BIGINT NOT NULL,
    `target_price` DECIMAL(10, 2) NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `remark` VARCHAR(100) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_price_alert_user_id` (`user_id`),
    KEY `idx_price_alert_product` (`product_type`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `flight` (`flight_no`, `airline_name`, `departure_city`, `arrival_city`, `departure_airport`, `arrival_airport`, `departure_time`, `arrival_time`, `price`, `stock`, `cabin_class`, `baggage_policy`, `refund_policy`, `status`)
SELECT 'FM8201', 'Shanghai Airlines', '涓婃捣', '鍖椾含', 'Hongqiao T1', 'Daxing', '2026-05-01 09:15:00', '2026-05-01 11:35:00', 799.00, 20, 'Economy', '20KG checked baggage', 'Change allowed before departure', 1
WHERE NOT EXISTS (SELECT 1 FROM `flight` WHERE `flight_no` = 'FM8201' AND `departure_time` = '2026-05-01 09:15:00');

INSERT INTO `flight` (`flight_no`, `airline_name`, `departure_city`, `arrival_city`, `departure_airport`, `arrival_airport`, `departure_time`, `arrival_time`, `price`, `stock`, `cabin_class`, `baggage_policy`, `refund_policy`, `status`)
SELECT 'HO1257', 'Juneyao Airlines', '涓婃捣', '鍖椾含', 'Hongqiao T2', 'Capital T2', '2026-05-01 19:20:00', '2026-05-01 21:40:00', 869.00, 11, 'Economy', '20KG checked baggage', 'Change allowed before departure', 1
WHERE NOT EXISTS (SELECT 1 FROM `flight` WHERE `flight_no` = 'HO1257' AND `departure_time` = '2026-05-01 19:20:00');

INSERT INTO `hotel` (`hotel_name`, `city`, `district`, `address`, `description`, `star_level`, `cover_image`, `check_in_time`, `check_out_time`, `status`)
SELECT 'Shanghai Bund Select Hotel', '涓婃捣', '榛勬郸鍖?', 'No. 188 East Zhongshan Road', 'Demo comparable hotel near the Bund.', 5, '', '14:00', '12:00', 1
WHERE NOT EXISTS (SELECT 1 FROM `hotel` WHERE `hotel_name` = 'Shanghai Bund Select Hotel');

INSERT INTO `hotel` (`hotel_name`, `city`, `district`, `address`, `description`, `star_level`, `cover_image`, `check_in_time`, `check_out_time`, `status`)
SELECT 'Shanghai Metro Garden Hotel', '涓婃捣', '闈欏畨鍖?', 'No. 66 Nanjing West Road', 'Demo comparable hotel for price comparison.', 5, '', '14:00', '12:00', 1
WHERE NOT EXISTS (SELECT 1 FROM `hotel` WHERE `hotel_name` = 'Shanghai Metro Garden Hotel');

INSERT INTO `hotel_room` (`hotel_id`, `room_name`, `bed_type`, `breakfast`, `room_area`, `guest_count`, `price`, `stock`, `cancel_rule`, `status`)
SELECT h.id, 'Deluxe King Room', '1 King Bed', 'Double Breakfast', '35m2', 2, 618.00, 10, 'Free cancellation before 18:00 one day ahead', 1
FROM `hotel` h
WHERE h.hotel_name = 'Shanghai Bund Select Hotel'
  AND NOT EXISTS (SELECT 1 FROM `hotel_room` r WHERE r.hotel_id = h.id AND r.room_name = 'Deluxe King Room');

INSERT INTO `hotel_room` (`hotel_id`, `room_name`, `bed_type`, `breakfast`, `room_area`, `guest_count`, `price`, `stock`, `cancel_rule`, `status`)
SELECT h.id, 'Business Twin Room', '2 Single Beds', 'Double Breakfast', '38m2', 2, 738.00, 8, 'Free cancellation before 18:00 one day ahead', 1
FROM `hotel` h
WHERE h.hotel_name = 'Shanghai Bund Select Hotel'
  AND NOT EXISTS (SELECT 1 FROM `hotel_room` r WHERE r.hotel_id = h.id AND r.room_name = 'Business Twin Room');

INSERT INTO `hotel_room` (`hotel_id`, `room_name`, `bed_type`, `breakfast`, `room_area`, `guest_count`, `price`, `stock`, `cancel_rule`, `status`)
SELECT h.id, 'Select Queen Room', '1 Queen Bed', 'Breakfast Included', '30m2', 2, 558.00, 14, 'Free cancellation before 18:00 one day ahead', 1
FROM `hotel` h
WHERE h.hotel_name = 'Shanghai Metro Garden Hotel'
  AND NOT EXISTS (SELECT 1 FROM `hotel_room` r WHERE r.hotel_id = h.id AND r.room_name = 'Select Queen Room');

INSERT INTO `hotel_room` (`hotel_id`, `room_name`, `bed_type`, `breakfast`, `room_area`, `guest_count`, `price`, `stock`, `cancel_rule`, `status`)
SELECT h.id, 'Executive Room', '1 King Bed', 'Double Breakfast', '42m2', 2, 699.00, 7, 'Free cancellation before 18:00 one day ahead', 1
FROM `hotel` h
WHERE h.hotel_name = 'Shanghai Metro Garden Hotel'
  AND NOT EXISTS (SELECT 1 FROM `hotel_room` r WHERE r.hotel_id = h.id AND r.room_name = 'Executive Room');

INSERT INTO `tour_package` (`package_name`, `destination`, `departure_city`, `days`, `price`, `stock`, `travel_dates`, `description`, `cover_image`, `status`)
SELECT 'Sanya Free Travel 4D3N', '涓変簹', '娣卞湷', 4, 2899.00, 15, '2026-06-02,2026-06-09,2026-06-16', 'Demo free travel package for price compare.', '', 1
WHERE NOT EXISTS (SELECT 1 FROM `tour_package` WHERE `package_name` = 'Sanya Free Travel 4D3N');

INSERT INTO `tour_package` (`package_name`, `destination`, `departure_city`, `days`, `price`, `stock`, `travel_dates`, `description`, `cover_image`, `status`)
SELECT 'Sanya Resort 6D5N', '涓変簹', '鍖椾含', 6, 3899.00, 12, '2026-06-04,2026-06-11,2026-06-18', 'Demo resort package for price compare.', '', 1
WHERE NOT EXISTS (SELECT 1 FROM `tour_package` WHERE `package_name` = 'Sanya Resort 6D5N');

INSERT INTO `coupon` (`coupon_name`, `product_type`, `threshold_amount`, `discount_amount`, `discount_type`, `description`, `status`, `start_time`, `end_time`)
SELECT 'Hotel Save 40', 'HOTEL', 500.00, 40.00, 'FULL_REDUCTION', 'Demo hotel coupon for detail page display.', 1, '2026-01-01 00:00:00', '2026-12-31 23:59:59'
WHERE NOT EXISTS (SELECT 1 FROM `coupon` WHERE `coupon_name` = 'Hotel Save 40');

INSERT INTO `coupon` (`coupon_name`, `product_type`, `threshold_amount`, `discount_amount`, `discount_type`, `description`, `status`, `start_time`, `end_time`)
SELECT 'Hotel Instant 30', 'HOTEL', 300.00, 30.00, 'INSTANT', 'Demo hotel instant discount.', 1, '2026-01-01 00:00:00', '2026-12-31 23:59:59'
WHERE NOT EXISTS (SELECT 1 FROM `coupon` WHERE `coupon_name` = 'Hotel Instant 30');

INSERT INTO `coupon` (`coupon_name`, `product_type`, `threshold_amount`, `discount_amount`, `discount_type`, `description`, `status`, `start_time`, `end_time`)
SELECT 'Flight Instant 20', 'FLIGHT', 500.00, 20.00, 'INSTANT', 'Demo flight instant discount.', 1, '2026-01-01 00:00:00', '2026-12-31 23:59:59'
WHERE NOT EXISTS (SELECT 1 FROM `coupon` WHERE `coupon_name` = 'Flight Instant 20');

INSERT INTO `coupon` (`coupon_name`, `product_type`, `threshold_amount`, `discount_amount`, `discount_type`, `description`, `status`, `start_time`, `end_time`)
SELECT 'Flight Save 60', 'FLIGHT', 800.00, 60.00, 'FULL_REDUCTION', 'Demo flight coupon for comparison module.', 1, '2026-01-01 00:00:00', '2026-12-31 23:59:59'
WHERE NOT EXISTS (SELECT 1 FROM `coupon` WHERE `coupon_name` = 'Flight Save 60');

INSERT INTO `coupon` (`coupon_name`, `product_type`, `threshold_amount`, `discount_amount`, `discount_type`, `description`, `status`, `start_time`, `end_time`)
SELECT 'Tour Save 200', 'TOUR', 3000.00, 200.00, 'FULL_REDUCTION', 'Demo tour package coupon.', 1, '2026-01-01 00:00:00', '2026-12-31 23:59:59'
WHERE NOT EXISTS (SELECT 1 FROM `coupon` WHERE `coupon_name` = 'Tour Save 200');

INSERT INTO `coupon` (`coupon_name`, `product_type`, `threshold_amount`, `discount_amount`, `discount_type`, `description`, `status`, `start_time`, `end_time`)
SELECT 'Tour Instant 100', 'TOUR', 2500.00, 100.00, 'INSTANT', 'Demo tour instant discount.', 1, '2026-01-01 00:00:00', '2026-12-31 23:59:59'
WHERE NOT EXISTS (SELECT 1 FROM `coupon` WHERE `coupon_name` = 'Tour Instant 100');

INSERT INTO `price_alert` (`user_id`, `product_type`, `product_id`, `target_price`, `status`, `remark`)
SELECT u.id, 'HOTEL', h.id, 650.00, 1, 'Demo hotel alert'
FROM `user` u
JOIN `hotel` h ON h.hotel_name = 'Shanghai Bund Select Hotel'
WHERE u.username = 'demo_user'
  AND NOT EXISTS (
      SELECT 1 FROM `price_alert` pa
      WHERE pa.user_id = u.id AND pa.product_type = 'HOTEL' AND pa.product_id = h.id
  );

INSERT INTO `price_alert` (`user_id`, `product_type`, `product_id`, `target_price`, `status`, `remark`)
SELECT u.id, 'FLIGHT', f.id, 900.00, 1, 'Demo flight alert'
FROM `user` u
JOIN `flight` f ON f.flight_no = 'CA1832' AND f.departure_time = '2026-05-01 14:10:00'
WHERE u.username = 'demo_user'
  AND NOT EXISTS (
      SELECT 1 FROM `price_alert` pa
      WHERE pa.user_id = u.id AND pa.product_type = 'FLIGHT' AND pa.product_id = f.id
  );

CREATE TABLE IF NOT EXISTS `share_post` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `summary` VARCHAR(255) NOT NULL,
    `content` TEXT NOT NULL,
    `cover_image` VARCHAR(255) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `view_count` INT NOT NULL DEFAULT 0,
    `like_count` INT NOT NULL DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_share_post_user_id` (`user_id`),
    KEY `idx_share_post_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `share_image` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `image_url` VARCHAR(255) NOT NULL,
    `sort_no` INT NOT NULL DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_share_image_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `biz_type` VARCHAR(30) NOT NULL,
    `biz_id` BIGINT NOT NULL,
    `rating` TINYINT NOT NULL,
    `content` VARCHAR(500) NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_review_order_id` (`order_id`),
    KEY `idx_review_user_id` (`user_id`),
    KEY `idx_review_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
