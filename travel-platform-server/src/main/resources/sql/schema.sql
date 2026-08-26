-- Keep SQL literals in UTF-8 even when the MySQL command-line client defaults to latin1.
SET NAMES utf8mb4;

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
    `cabin_class` VARCHAR(50) NOT NULL DEFAULT 'Economy',
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
    `detail_images` TEXT DEFAULT NULL,
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
    `detail_images` TEXT DEFAULT NULL,
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

CREATE TABLE IF NOT EXISTS `attraction` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `city` VARCHAR(50) NOT NULL,
    `district` VARCHAR(50) DEFAULT NULL,
    `attraction_name` VARCHAR(100) NOT NULL,
    `attraction_type` VARCHAR(50) DEFAULT NULL,
    `tags` VARCHAR(255) DEFAULT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `suggested_duration` VARCHAR(50) DEFAULT NULL,
    `priority` INT NOT NULL DEFAULT 1,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_attraction_city_name` (`city`, `attraction_name`),
    KEY `idx_attraction_city_status` (`city`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
