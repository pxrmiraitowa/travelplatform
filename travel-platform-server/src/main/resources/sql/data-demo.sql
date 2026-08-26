-- The official MySQL image may invoke its client with latin1 as the session default.
-- Declare the file encoding explicitly before inserting Chinese demo text.
SET NAMES utf8mb4;

INSERT INTO `role` (`role_code`, `role_name`, `status`)
SELECT 'ROLE_USER', '普通用户', 1
WHERE NOT EXISTS (SELECT 1 FROM `role` WHERE `role_code` = 'ROLE_USER');

INSERT INTO `role` (`role_code`, `role_name`, `status`)
SELECT 'ROLE_ADMIN', '系统管理员', 1
WHERE NOT EXISTS (SELECT 1 FROM `role` WHERE `role_code` = 'ROLE_ADMIN');

INSERT INTO `user` (`username`, `password`, `nickname`, `phone`, `status`)
SELECT 'demo_user', '$2a$10$ujhAXWqWhkHyzQIC5ywpjuBNnShqqvIj4b3hWe3BShQHWvJyrfPvu', '演示用户', '13800000000', 1
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'demo_user');

INSERT INTO `user` (`username`, `password`, `nickname`, `phone`, `status`)
SELECT 'admin', '$2a$10$RwUGSDk/j9fnReTZnKYcdO8vn0yZyL0z5pQ0a6GnsHVKu/9xCxV/.', '系统管理员', '13900000000', 1
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'admin');

INSERT INTO `user_role` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `user` u
JOIN `role` r ON r.role_code = 'ROLE_USER'
WHERE u.username = 'demo_user'
  AND NOT EXISTS (
      SELECT 1 FROM `user_role` ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO `user_role` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `user` u
JOIN `role` r ON r.role_code = 'ROLE_USER'
WHERE u.username = 'admin'
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

INSERT INTO `user_contact` (`user_id`, `name`, `phone`, `id_card`, `contact_type`, `is_default`, `remark`)
SELECT u.id, '张三', '13800000000', '310101199001011234', 1, 1, '默认出行人'
FROM `user` u
WHERE u.username = 'demo_user'
  AND NOT EXISTS (
      SELECT 1 FROM `user_contact` uc WHERE uc.user_id = u.id AND uc.phone = '13800000000'
  );

INSERT INTO `flight` (`flight_no`, `airline_name`, `departure_city`, `arrival_city`, `departure_airport`, `arrival_airport`, `departure_time`, `arrival_time`, `price`, `stock`, `cabin_class`, `baggage_policy`, `refund_policy`, `status`)
SELECT 'MU5101', '东方航空', '上海', '北京', '上海虹桥T2', '北京首都T2', '2030-07-01 08:20:00', '2030-07-01 10:35:00', 880.00, 28, 'Economy', '免费托运20KG', '起飞前可退改签，收取手续费', 1
WHERE NOT EXISTS (SELECT 1 FROM `flight` WHERE `flight_no` = 'MU5101' AND `departure_time` = '2030-07-01 08:20:00');

INSERT INTO `flight` (`flight_no`, `airline_name`, `departure_city`, `arrival_city`, `departure_airport`, `arrival_airport`, `departure_time`, `arrival_time`, `price`, `stock`, `cabin_class`, `baggage_policy`, `refund_policy`, `status`)
SELECT 'CA1832', '中国国航', '上海', '北京', '上海浦东T1', '北京首都T3', '2030-07-01 14:10:00', '2030-07-01 16:30:00', 960.00, 16, 'Economy', '免费托运20KG', '起飞前可退改签，收取手续费', 1
WHERE NOT EXISTS (SELECT 1 FROM `flight` WHERE `flight_no` = 'CA1832' AND `departure_time` = '2030-07-01 14:10:00');

INSERT INTO `flight` (`flight_no`, `airline_name`, `departure_city`, `arrival_city`, `departure_airport`, `arrival_airport`, `departure_time`, `arrival_time`, `price`, `stock`, `cabin_class`, `baggage_policy`, `refund_policy`, `status`)
SELECT 'CZ6107', '南方航空', '广州', '成都', '广州白云T2', '成都天府T2', '2030-07-02 09:00:00', '2030-07-02 11:35:00', 720.00, 22, 'Economy', '免费托运20KG', '起飞前可退改签，收取手续费', 1
WHERE NOT EXISTS (SELECT 1 FROM `flight` WHERE `flight_no` = 'CZ6107' AND `departure_time` = '2030-07-02 09:00:00');

INSERT INTO `train_ticket` (`train_no`, `train_type`, `departure_city`, `arrival_city`, `departure_station`, `arrival_station`, `departure_time`, `arrival_time`, `duration_minutes`, `business_price`, `first_class_price`, `second_class_price`, `business_stock`, `first_class_stock`, `second_class_stock`, `status`)
SELECT 'G1024', '高铁', '上海', '杭州', '上海虹桥', '杭州东', '2030-07-03 08:00:00', '2030-07-03 09:05:00', 65, 368.00, 228.00, 138.00, 8, 18, 48, 1
WHERE NOT EXISTS (SELECT 1 FROM `train_ticket` WHERE `train_no` = 'G1024' AND `departure_time` = '2030-07-03 08:00:00');

INSERT INTO `train_ticket` (`train_no`, `train_type`, `departure_city`, `arrival_city`, `departure_station`, `arrival_station`, `departure_time`, `arrival_time`, `duration_minutes`, `business_price`, `first_class_price`, `second_class_price`, `business_stock`, `first_class_stock`, `second_class_stock`, `status`)
SELECT 'D2208', '动车', '成都', '重庆', '成都东', '重庆北', '2030-07-03 10:15:00', '2030-07-03 12:08:00', 113, 318.00, 198.00, 118.00, 6, 16, 40, 1
WHERE NOT EXISTS (SELECT 1 FROM `train_ticket` WHERE `train_no` = 'D2208' AND `departure_time` = '2030-07-03 10:15:00');

INSERT INTO `hotel` (`hotel_name`, `city`, `district`, `address`, `description`, `star_level`, `cover_image`, `detail_images`, `check_in_time`, `check_out_time`, `status`)
SELECT '上海外滩精选酒店', '上海', '黄浦区', '中山东一路88号', '位于外滩商圈，适合城市观光与商务出行。', 5, '', '[]', '14:00', '12:00', 1
WHERE NOT EXISTS (SELECT 1 FROM `hotel` WHERE `hotel_name` = '上海外滩精选酒店');

INSERT INTO `hotel` (`hotel_name`, `city`, `district`, `address`, `description`, `star_level`, `cover_image`, `detail_images`, `check_in_time`, `check_out_time`, `status`)
SELECT '上海地铁花园酒店', '上海', '静安区', '南京西路66号', '靠近地铁与商圈，适合短住与度假。', 4, '', '[]', '14:00', '12:00', 1
WHERE NOT EXISTS (SELECT 1 FROM `hotel` WHERE `hotel_name` = '上海地铁花园酒店');

INSERT INTO `hotel_room` (`hotel_id`, `room_name`, `bed_type`, `breakfast`, `room_area`, `guest_count`, `price`, `stock`, `cancel_rule`, `status`)
SELECT h.id, '豪华大床房', '1张特大床', '双早', '35平方米', 2, 618.00, 10, '入住前一天18:00前可免费取消', 1
FROM `hotel` h
WHERE h.hotel_name = '上海外滩精选酒店'
  AND NOT EXISTS (SELECT 1 FROM `hotel_room` r WHERE r.hotel_id = h.id AND r.room_name = '豪华大床房');

INSERT INTO `hotel_room` (`hotel_id`, `room_name`, `bed_type`, `breakfast`, `room_area`, `guest_count`, `price`, `stock`, `cancel_rule`, `status`)
SELECT h.id, '商务双床房', '2张单人床', '双早', '38平方米', 2, 738.00, 8, '入住前一天18:00前可免费取消', 1
FROM `hotel` h
WHERE h.hotel_name = '上海外滩精选酒店'
  AND NOT EXISTS (SELECT 1 FROM `hotel_room` r WHERE r.hotel_id = h.id AND r.room_name = '商务双床房');

INSERT INTO `hotel_room` (`hotel_id`, `room_name`, `bed_type`, `breakfast`, `room_area`, `guest_count`, `price`, `stock`, `cancel_rule`, `status`)
SELECT h.id, '精选大床房', '1张大床', '含早餐', '30平方米', 2, 558.00, 14, '入住前一天18:00前可免费取消', 1
FROM `hotel` h
WHERE h.hotel_name = '上海地铁花园酒店'
  AND NOT EXISTS (SELECT 1 FROM `hotel_room` r WHERE r.hotel_id = h.id AND r.room_name = '精选大床房');

INSERT INTO `hotel_room` (`hotel_id`, `room_name`, `bed_type`, `breakfast`, `room_area`, `guest_count`, `price`, `stock`, `cancel_rule`, `status`)
SELECT h.id, '行政大床房', '1张特大床', '双早', '42平方米', 2, 699.00, 7, '入住前一天18:00前可免费取消', 1
FROM `hotel` h
WHERE h.hotel_name = '上海地铁花园酒店'
  AND NOT EXISTS (SELECT 1 FROM `hotel_room` r WHERE r.hotel_id = h.id AND r.room_name = '行政大床房');

INSERT INTO `tour_package` (`package_name`, `destination`, `departure_city`, `days`, `price`, `stock`, `travel_dates`, `description`, `cover_image`, `detail_images`, `status`)
SELECT '三亚自由行4天3晚', '三亚', '深圳', 4, 2899.00, 15, '2030-07-10,2030-07-17,2030-07-24', '适合海边度假的轻松线路。', '', '[]', 1
WHERE NOT EXISTS (SELECT 1 FROM `tour_package` WHERE `package_name` = '三亚自由行4天3晚');

INSERT INTO `tour_package` (`package_name`, `destination`, `departure_city`, `days`, `price`, `stock`, `travel_dates`, `description`, `cover_image`, `detail_images`, `status`)
SELECT '丽江慢游6天5晚', '丽江', '北京', 6, 3899.00, 12, '2030-07-12,2030-07-19,2030-07-26', '兼顾古城休闲与自然风光。', '', '[]', 1
WHERE NOT EXISTS (SELECT 1 FROM `tour_package` WHERE `package_name` = '丽江慢游6天5晚');

INSERT INTO `coupon` (`coupon_name`, `product_type`, `threshold_amount`, `discount_amount`, `discount_type`, `description`, `status`, `start_time`, `end_time`)
SELECT 'Hotel Save 40', 'HOTEL', 500.00, 40.00, 'FULL_REDUCTION', '酒店满减券', 1, '2020-01-01 00:00:00', '2035-12-31 23:59:59'
WHERE NOT EXISTS (SELECT 1 FROM `coupon` WHERE `coupon_name` = 'Hotel Save 40');

INSERT INTO `coupon` (`coupon_name`, `product_type`, `threshold_amount`, `discount_amount`, `discount_type`, `description`, `status`, `start_time`, `end_time`)
SELECT 'Flight Instant 20', 'FLIGHT', 500.00, 20.00, 'INSTANT', '机票立减券', 1, '2020-01-01 00:00:00', '2035-12-31 23:59:59'
WHERE NOT EXISTS (SELECT 1 FROM `coupon` WHERE `coupon_name` = 'Flight Instant 20');

INSERT INTO `coupon` (`coupon_name`, `product_type`, `threshold_amount`, `discount_amount`, `discount_type`, `description`, `status`, `start_time`, `end_time`)
SELECT 'Tour Save 200', 'TOUR', 3000.00, 200.00, 'FULL_REDUCTION', '跟团线路满减券', 1, '2020-01-01 00:00:00', '2035-12-31 23:59:59'
WHERE NOT EXISTS (SELECT 1 FROM `coupon` WHERE `coupon_name` = 'Tour Save 200');

INSERT INTO `attraction` (`city`, `district`, `attraction_name`, `attraction_type`, `tags`, `description`, `suggested_duration`, `priority`, `status`)
SELECT '上海', '黄浦区', '外滩', '街区', '城市地标,夜景,休闲', '上海经典城市地标，适合白天和夜间观景。', '2-3小时', 10, 1
WHERE NOT EXISTS (SELECT 1 FROM `attraction` WHERE `city` = '上海' AND `attraction_name` = '外滩');

INSERT INTO `attraction` (`city`, `district`, `attraction_name`, `attraction_type`, `tags`, `description`, `suggested_duration`, `priority`, `status`)
SELECT '北京', '东城区', '故宫博物院', '博物馆', '历史,地标,亲子', '适合历史文化主题游览。', '4-6小时', 10, 1
WHERE NOT EXISTS (SELECT 1 FROM `attraction` WHERE `city` = '北京' AND `attraction_name` = '故宫博物院');

INSERT INTO `attraction` (`city`, `district`, `attraction_name`, `attraction_type`, `tags`, `description`, `suggested_duration`, `priority`, `status`)
SELECT '杭州', '西湖区', '西湖风景名胜区', '湖泊', '自然风光,休闲,地标', '杭州代表性景点，适合分时段游览。', '4-6小时', 10, 1
WHERE NOT EXISTS (SELECT 1 FROM `attraction` WHERE `city` = '杭州' AND `attraction_name` = '西湖风景名胜区');

INSERT INTO `price_alert` (`user_id`, `product_type`, `product_id`, `target_price`, `status`, `remark`)
SELECT u.id, 'HOTEL', h.id, 650.00, 1, 'Demo hotel alert'
FROM `user` u
JOIN `hotel` h ON h.hotel_name = '上海外滩精选酒店'
WHERE u.username = 'demo_user'
  AND NOT EXISTS (
      SELECT 1 FROM `price_alert` pa
      WHERE pa.user_id = u.id AND pa.product_type = 'HOTEL' AND pa.product_id = h.id
  );

INSERT INTO `price_alert` (`user_id`, `product_type`, `product_id`, `target_price`, `status`, `remark`)
SELECT u.id, 'FLIGHT', f.id, 900.00, 1, 'Demo flight alert'
FROM `user` u
JOIN `flight` f ON f.flight_no = 'CA1832' AND f.departure_time = '2030-07-01 14:10:00'
WHERE u.username = 'demo_user'
  AND NOT EXISTS (
      SELECT 1 FROM `price_alert` pa
      WHERE pa.user_id = u.id AND pa.product_type = 'FLIGHT' AND pa.product_id = f.id
  );
