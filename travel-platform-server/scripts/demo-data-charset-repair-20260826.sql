-- Repair demo text imported by a latin1 MySQL client from UTF-8 SQL files.
-- The updates use stable business keys and preserve product IDs, stock and order relations.
SET NAMES utf8mb4;

UPDATE `role`
SET `role_name` = CASE `role_code`
    WHEN 'ROLE_USER' THEN '普通用户'
    WHEN 'ROLE_ADMIN' THEN '系统管理员'
    ELSE `role_name`
END
WHERE `role_code` IN ('ROLE_USER', 'ROLE_ADMIN');

UPDATE `user`
SET `nickname` = CASE `username`
    WHEN 'demo_user' THEN '演示用户'
    WHEN 'admin' THEN '系统管理员'
    ELSE `nickname`
END
WHERE `username` IN ('demo_user', 'admin');

UPDATE `user_contact` c
JOIN `user` u ON u.`id` = c.`user_id`
SET
    c.`name` = '张三',
    c.`remark` = '默认出行人'
WHERE u.`username` = 'demo_user'
  AND c.`phone` = '13800000000';

UPDATE `flight`
SET
    `airline_name` = '东方航空',
    `departure_city` = '上海',
    `arrival_city` = '北京',
    `departure_airport` = '上海虹桥T2',
    `arrival_airport` = '北京首都T2',
    `baggage_policy` = '免费托运20KG',
    `refund_policy` = '起飞前可退改签，收取手续费'
WHERE `flight_no` = 'MU5101'
  AND `departure_time` = '2030-07-01 08:20:00';

UPDATE `flight`
SET
    `airline_name` = '中国国航',
    `departure_city` = '上海',
    `arrival_city` = '北京',
    `departure_airport` = '上海浦东T1',
    `arrival_airport` = '北京首都T3',
    `baggage_policy` = '免费托运20KG',
    `refund_policy` = '起飞前可退改签，收取手续费'
WHERE `flight_no` = 'CA1832'
  AND `departure_time` = '2030-07-01 14:10:00';

UPDATE `flight`
SET
    `airline_name` = '南方航空',
    `departure_city` = '广州',
    `arrival_city` = '成都',
    `departure_airport` = '广州白云T2',
    `arrival_airport` = '成都天府T2',
    `baggage_policy` = '免费托运20KG',
    `refund_policy` = '起飞前可退改签，收取手续费'
WHERE `flight_no` = 'CZ6107'
  AND `departure_time` = '2030-07-02 09:00:00';

UPDATE `train_ticket`
SET
    `train_type` = '高铁',
    `departure_city` = '上海',
    `arrival_city` = '杭州',
    `departure_station` = '上海虹桥',
    `arrival_station` = '杭州东'
WHERE `train_no` = 'G1024'
  AND `departure_time` = '2030-07-03 08:00:00';

UPDATE `train_ticket`
SET
    `train_type` = '动车',
    `departure_city` = '成都',
    `arrival_city` = '重庆',
    `departure_station` = '成都东',
    `arrival_station` = '重庆北'
WHERE `train_no` = 'D2208'
  AND `departure_time` = '2030-07-03 10:15:00';

UPDATE `hotel` h
SET
    h.`hotel_name` = '上海外滩精选酒店',
    h.`city` = '上海',
    h.`district` = '黄浦区',
    h.`address` = '中山东一路88号',
    h.`description` = '位于外滩商圈，适合城市观光与商务出行。'
WHERE h.`star_level` = 5
  AND EXISTS (
      SELECT 1 FROM `hotel_room` r
      WHERE r.`hotel_id` = h.`id` AND r.`price` = 618.00
  );

UPDATE `hotel` h
SET
    h.`hotel_name` = '上海地铁花园酒店',
    h.`city` = '上海',
    h.`district` = '静安区',
    h.`address` = '南京西路66号',
    h.`description` = '靠近地铁与商圈，适合短住与度假。'
WHERE h.`star_level` = 4
  AND EXISTS (
      SELECT 1 FROM `hotel_room` r
      WHERE r.`hotel_id` = h.`id` AND r.`price` = 558.00
  );

UPDATE `hotel_room` r
JOIN `hotel` h ON h.`id` = r.`hotel_id`
SET
    r.`room_name` = '豪华大床房',
    r.`bed_type` = '1张特大床',
    r.`breakfast` = '双早',
    r.`room_area` = '35平方米',
    r.`cancel_rule` = '入住前一天18:00前可免费取消'
WHERE h.`hotel_name` = '上海外滩精选酒店'
  AND r.`price` = 618.00;

UPDATE `hotel_room` r
JOIN `hotel` h ON h.`id` = r.`hotel_id`
SET
    r.`room_name` = '商务双床房',
    r.`bed_type` = '2张单人床',
    r.`breakfast` = '双早',
    r.`room_area` = '38平方米',
    r.`cancel_rule` = '入住前一天18:00前可免费取消'
WHERE h.`hotel_name` = '上海外滩精选酒店'
  AND r.`price` = 738.00;

UPDATE `hotel_room` r
JOIN `hotel` h ON h.`id` = r.`hotel_id`
SET
    r.`room_name` = '精选大床房',
    r.`bed_type` = '1张大床',
    r.`breakfast` = '含早餐',
    r.`room_area` = '30平方米',
    r.`cancel_rule` = '入住前一天18:00前可免费取消'
WHERE h.`hotel_name` = '上海地铁花园酒店'
  AND r.`price` = 558.00;

UPDATE `hotel_room` r
JOIN `hotel` h ON h.`id` = r.`hotel_id`
SET
    r.`room_name` = '行政大床房',
    r.`bed_type` = '1张特大床',
    r.`breakfast` = '双早',
    r.`room_area` = '42平方米',
    r.`cancel_rule` = '入住前一天18:00前可免费取消'
WHERE h.`hotel_name` = '上海地铁花园酒店'
  AND r.`price` = 699.00;

UPDATE `tour_package`
SET
    `package_name` = '三亚自由行4天3晚',
    `destination` = '三亚',
    `departure_city` = '深圳',
    `description` = '适合海边度假的轻松线路。'
WHERE `days` = 4
  AND `price` = 2899.00
  AND `travel_dates` = '2030-07-10,2030-07-17,2030-07-24';

UPDATE `tour_package`
SET
    `package_name` = '丽江慢游6天5晚',
    `destination` = '丽江',
    `departure_city` = '北京',
    `description` = '兼顾古城休闲与自然风光。'
WHERE `days` = 6
  AND `price` = 3899.00
  AND `travel_dates` = '2030-07-12,2030-07-19,2030-07-26';

UPDATE `coupon`
SET `description` = '酒店满减券'
WHERE `coupon_name` = 'Hotel Save 40';

UPDATE `coupon`
SET `description` = '机票立减券'
WHERE `coupon_name` = 'Flight Instant 20';

UPDATE `coupon`
SET `description` = '跟团线路满减券'
WHERE `coupon_name` = 'Tour Save 200';

UPDATE `attraction`
SET
    `city` = '上海',
    `district` = '黄浦区',
    `attraction_name` = '外滩',
    `attraction_type` = '街区',
    `tags` = '城市地标,夜景,休闲',
    `description` = '上海经典城市地标，适合白天和夜间观景。',
    `suggested_duration` = '2-3小时'
WHERE `id` = 1 AND `priority` = 10;

UPDATE `attraction`
SET
    `city` = '北京',
    `district` = '东城区',
    `attraction_name` = '故宫博物院',
    `attraction_type` = '博物馆',
    `tags` = '历史,地标,亲子',
    `description` = '适合历史文化主题游览。',
    `suggested_duration` = '4-6小时'
WHERE `id` = 2 AND `priority` = 10;

UPDATE `attraction`
SET
    `city` = '杭州',
    `district` = '西湖区',
    `attraction_name` = '西湖风景名胜区',
    `attraction_type` = '湖泊',
    `tags` = '自然风光,休闲,地标',
    `description` = '杭州代表性景点，适合分时段游览。',
    `suggested_duration` = '4-6小时'
WHERE `id` = 3 AND `priority` = 10;

-- Refresh denormalized order snapshots after repairing their source products.
UPDATE `orders` o
JOIN `user_contact` c
  ON c.`user_id` = o.`user_id` AND c.`phone` = o.`contact_phone`
SET o.`contact_name` = c.`name`;

UPDATE `order_flight` d
JOIN `flight` f ON f.`id` = d.`flight_id`
JOIN `orders` o ON o.`id` = d.`order_id`
SET
    d.`airline_name` = f.`airline_name`,
    d.`departure_city` = f.`departure_city`,
    d.`arrival_city` = f.`arrival_city`,
    d.`departure_airport` = f.`departure_airport`,
    d.`arrival_airport` = f.`arrival_airport`,
    d.`passenger_name` = o.`contact_name`;

UPDATE `order_train` d
JOIN `train_ticket` t ON t.`id` = d.`train_ticket_id`
JOIN `orders` o ON o.`id` = d.`order_id`
SET
    d.`train_type` = t.`train_type`,
    d.`departure_city` = t.`departure_city`,
    d.`arrival_city` = t.`arrival_city`,
    d.`departure_station` = t.`departure_station`,
    d.`arrival_station` = t.`arrival_station`,
    d.`passenger_name` = o.`contact_name`;

UPDATE `order_hotel` d
JOIN `hotel` h ON h.`id` = d.`hotel_id`
JOIN `hotel_room` r ON r.`id` = d.`hotel_room_id`
JOIN `orders` o ON o.`id` = d.`order_id`
SET
    d.`hotel_name` = h.`hotel_name`,
    d.`city` = h.`city`,
    d.`address` = h.`address`,
    d.`room_name` = r.`room_name`,
    d.`bed_type` = r.`bed_type`,
    d.`breakfast` = r.`breakfast`,
    d.`guest_name` = o.`contact_name`;

UPDATE `order_tour` d
JOIN `tour_package` t ON t.`id` = d.`tour_package_id`
JOIN `orders` o ON o.`id` = d.`order_id`
SET
    d.`package_name` = t.`package_name`,
    d.`destination` = t.`destination`,
    d.`departure_city` = t.`departure_city`,
    d.`guest_name` = o.`contact_name`;
