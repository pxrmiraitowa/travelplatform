-- Keep test fixtures consistent across local MySQL and containerized MySQL clients.
SET NAMES utf8mb4;

INSERT INTO `role` (`role_code`, `role_name`, `status`)
VALUES
('ROLE_USER', '普通用户', 1),
('ROLE_ADMIN', '系统管理员', 1)
ON DUPLICATE KEY UPDATE
    `role_name` = VALUES(`role_name`),
    `status` = VALUES(`status`);

INSERT INTO `user` (`username`, `password`, `nickname`, `phone`, `status`)
SELECT 'demo_user', '$2a$10$ujhAXWqWhkHyzQIC5ywpjuBNnShqqvIj4b3hWe3BShQHWvJyrfPvu', '演示用户', '13800000000', 1
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'demo_user');

INSERT INTO `user` (`username`, `password`, `nickname`, `phone`, `status`)
SELECT 'admin', '$2a$10$RwUGSDk/j9fnReTZnKYcdO8vn0yZyL0z5pQ0a6GnsHVKu/9xCxV/.', '系统管理员', '13900000009', 1
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'admin');

INSERT INTO `user_role` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `user` u
JOIN `role` r ON r.role_code = 'ROLE_USER'
WHERE u.username IN ('demo_user', 'admin')
  AND NOT EXISTS (
      SELECT 1 FROM `user_role` ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO `user_role` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `user` u
JOIN `role` r ON r.role_code = 'ROLE_ADMIN'
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM `user_role` ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO `flight` (`flight_no`, `airline_name`, `departure_city`, `arrival_city`, `departure_airport`, `arrival_airport`, `departure_time`, `arrival_time`, `price`, `stock`, `cabin_class`, `baggage_policy`, `refund_policy`, `status`)
SELECT 'IT1001', '测试航空', '上海', '北京', '上海虹桥T2', '北京首都T3', '2030-08-01 08:00:00', '2030-08-01 10:20:00', 800.00, 20, 'Economy', '免费托运20KG', '起飞前可退改签', 1
WHERE NOT EXISTS (SELECT 1 FROM `flight` WHERE `flight_no` = 'IT1001' AND `departure_time` = '2030-08-01 08:00:00');

INSERT INTO `flight` (`flight_no`, `airline_name`, `departure_city`, `arrival_city`, `departure_airport`, `arrival_airport`, `departure_time`, `arrival_time`, `price`, `stock`, `cabin_class`, `baggage_policy`, `refund_policy`, `status`)
SELECT 'IT1002', '测试航空', '广州', '成都', '广州白云T2', '成都天府T2', '2030-08-02 09:30:00', '2030-08-02 12:10:00', 760.00, 18, 'Economy', '免费托运20KG', '起飞前可退改签', 1
WHERE NOT EXISTS (SELECT 1 FROM `flight` WHERE `flight_no` = 'IT1002' AND `departure_time` = '2030-08-02 09:30:00');

INSERT INTO `train_ticket` (`train_no`, `train_type`, `departure_city`, `arrival_city`, `departure_station`, `arrival_station`, `departure_time`, `arrival_time`, `duration_minutes`, `business_price`, `first_class_price`, `second_class_price`, `business_stock`, `first_class_stock`, `second_class_stock`, `status`)
SELECT 'G9001', '高铁', '上海', '杭州', '上海虹桥', '杭州东', '2030-08-03 08:30:00', '2030-08-03 09:35:00', 65, 320.00, 220.00, 120.00, 8, 20, 50, 1
WHERE NOT EXISTS (SELECT 1 FROM `train_ticket` WHERE `train_no` = 'G9001' AND `departure_time` = '2030-08-03 08:30:00');

INSERT INTO `hotel` (`hotel_name`, `city`, `district`, `address`, `description`, `star_level`, `cover_image`, `detail_images`, `check_in_time`, `check_out_time`, `status`)
SELECT '集成测试酒店A', '上海', '浦东新区', '测试路100号', '用于集成测试的酒店样例。', 4, '', '[]', '14:00', '12:00', 1
WHERE NOT EXISTS (SELECT 1 FROM `hotel` WHERE `hotel_name` = '集成测试酒店A');

INSERT INTO `hotel` (`hotel_name`, `city`, `district`, `address`, `description`, `star_level`, `cover_image`, `detail_images`, `check_in_time`, `check_out_time`, `status`)
SELECT '集成测试酒店B', '上海', '静安区', '测试路200号', '用于集成测试的酒店样例。', 5, '', '[]', '14:00', '12:00', 1
WHERE NOT EXISTS (SELECT 1 FROM `hotel` WHERE `hotel_name` = '集成测试酒店B');

INSERT INTO `hotel_room` (`hotel_id`, `room_name`, `bed_type`, `breakfast`, `room_area`, `guest_count`, `price`, `stock`, `cancel_rule`, `status`)
SELECT h.id, '标准大床房', '1张大床', '含早餐', '28平方米', 2, 580.00, 10, '入住前一天18:00前可免费取消', 1
FROM `hotel` h
WHERE h.hotel_name = '集成测试酒店A'
  AND NOT EXISTS (SELECT 1 FROM `hotel_room` r WHERE r.hotel_id = h.id AND r.room_name = '标准大床房');

INSERT INTO `hotel_room` (`hotel_id`, `room_name`, `bed_type`, `breakfast`, `room_area`, `guest_count`, `price`, `stock`, `cancel_rule`, `status`)
SELECT h.id, '行政双床房', '2张单人床', '双早', '36平方米', 2, 760.00, 8, '入住前一天18:00前可免费取消', 1
FROM `hotel` h
WHERE h.hotel_name = '集成测试酒店B'
  AND NOT EXISTS (SELECT 1 FROM `hotel_room` r WHERE r.hotel_id = h.id AND r.room_name = '行政双床房');

INSERT INTO `tour_package` (`package_name`, `destination`, `departure_city`, `days`, `price`, `stock`, `travel_dates`, `description`, `cover_image`, `detail_images`, `status`)
SELECT '集成测试三亚线', '三亚', '深圳', 4, 3200.00, 10, '2030-08-10,2030-08-17,2030-08-24', '用于集成测试的旅游线路。', '', '[]', 1
WHERE NOT EXISTS (SELECT 1 FROM `tour_package` WHERE `package_name` = '集成测试三亚线');

INSERT INTO `coupon` (`coupon_name`, `product_type`, `threshold_amount`, `discount_amount`, `discount_type`, `description`, `status`, `start_time`, `end_time`)
SELECT 'IT Flight Coupon', 'FLIGHT', 500.00, 20.00, 'INSTANT', '集成测试机票券', 1, '2020-01-01 00:00:00', '2035-12-31 23:59:59'
WHERE NOT EXISTS (SELECT 1 FROM `coupon` WHERE `coupon_name` = 'IT Flight Coupon');

INSERT INTO `coupon` (`coupon_name`, `product_type`, `threshold_amount`, `discount_amount`, `discount_type`, `description`, `status`, `start_time`, `end_time`)
SELECT 'IT Hotel Coupon', 'HOTEL', 500.00, 40.00, 'FULL_REDUCTION', '集成测试酒店券', 1, '2020-01-01 00:00:00', '2035-12-31 23:59:59'
WHERE NOT EXISTS (SELECT 1 FROM `coupon` WHERE `coupon_name` = 'IT Hotel Coupon');

INSERT INTO `coupon` (`coupon_name`, `product_type`, `threshold_amount`, `discount_amount`, `discount_type`, `description`, `status`, `start_time`, `end_time`)
SELECT 'IT Tour Coupon', 'TOUR', 3000.00, 200.00, 'FULL_REDUCTION', '集成测试线路券', 1, '2020-01-01 00:00:00', '2035-12-31 23:59:59'
WHERE NOT EXISTS (SELECT 1 FROM `coupon` WHERE `coupon_name` = 'IT Tour Coupon');

INSERT INTO `attraction` (`city`, `district`, `attraction_name`, `attraction_type`, `tags`, `description`, `suggested_duration`, `priority`, `status`)
SELECT '上海', '黄浦区', '测试外滩', '街区', '地标,夜景', '用于集成测试的景点样例。', '2小时', 10, 1
WHERE NOT EXISTS (SELECT 1 FROM `attraction` WHERE `city` = '上海' AND `attraction_name` = '测试外滩');
