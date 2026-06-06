UPDATE `flight`
SET
    `airline_name` = '上海航空',
    `departure_airport` = '虹桥T1',
    `arrival_airport` = '大兴',
    `cabin_class` = '经济舱',
    `baggage_policy` = '免费托运20KG',
    `refund_policy` = '起飞前可改期'
WHERE `flight_no` = 'FM8201'
  AND `departure_time` = '2026-05-01 09:15:00';

UPDATE `flight`
SET
    `airline_name` = '吉祥航空',
    `departure_airport` = '虹桥T2',
    `arrival_airport` = '首都T2',
    `cabin_class` = '经济舱',
    `baggage_policy` = '免费托运20KG',
    `refund_policy` = '起飞前可改期'
WHERE `flight_no` = 'HO1257'
  AND `departure_time` = '2026-05-01 19:20:00';

UPDATE `hotel`
SET
    `hotel_name` = '上海外滩精选酒店',
    `city` = '上海',
    `district` = '黄浦区',
    `address` = '中山东一路188号',
    `description` = '用于价格对比演示的外滩商圈酒店样例。'
WHERE `hotel_name` IN ('Shanghai Bund Select Hotel', '上海外滩精选酒店');

UPDATE `hotel`
SET
    `hotel_name` = '上海地铁花园酒店',
    `city` = '上海',
    `district` = '静安区',
    `address` = '南京西路66号',
    `description` = '用于价格对比演示的市中心酒店样例。'
WHERE `hotel_name` IN ('Shanghai Metro Garden Hotel', '上海地铁花园酒店');

UPDATE `hotel_room`
SET
    `room_name` = '豪华大床房',
    `bed_type` = '1张特大床',
    `breakfast` = '含双早',
    `room_area` = '35平方米',
    `cancel_rule` = '入住前一天18:00前可免费取消'
WHERE `room_name` = 'Deluxe King Room';

UPDATE `hotel_room`
SET
    `room_name` = '商务双床房',
    `bed_type` = '2张单人床',
    `breakfast` = '含双早',
    `room_area` = '38平方米',
    `cancel_rule` = '入住前一天18:00前可免费取消'
WHERE `room_name` = 'Business Twin Room';

UPDATE `hotel_room`
SET
    `room_name` = '精选大床房',
    `bed_type` = '1张大床',
    `breakfast` = '含早餐',
    `room_area` = '30平方米',
    `cancel_rule` = '入住前一天18:00前可免费取消'
WHERE `room_name` = 'Select Queen Room';

UPDATE `hotel_room`
SET
    `room_name` = '行政大床房',
    `bed_type` = '1张特大床',
    `breakfast` = '含双早',
    `room_area` = '42平方米',
    `cancel_rule` = '入住前一天18:00前可免费取消'
WHERE `room_name` = 'Executive Room';

UPDATE `tour_package`
SET
    `package_name` = '三亚自由行4天3晚',
    `description` = '用于价格对比演示的三亚自由行样例。'
WHERE `package_name` = 'Sanya Free Travel 4D3N';

UPDATE `tour_package`
SET
    `package_name` = '三亚度假酒店6天5晚',
    `description` = '用于价格对比演示的三亚度假样例。'
WHERE `package_name` = 'Sanya Resort 6D5N';

UPDATE `flight`
SET
    `airline_name` = CASE MOD(CAST(SUBSTRING(`flight_no`, 3) AS UNSIGNED) - 1, 6)
        WHEN 0 THEN '中国国航'
        WHEN 1 THEN '东方航空'
        WHEN 2 THEN '南方航空'
        WHEN 3 THEN '海南航空'
        WHEN 4 THEN '春秋航空'
        ELSE '厦门航空'
    END,
    `departure_city` = CASE MOD(CAST(SUBSTRING(`flight_no`, 3) AS UNSIGNED) - 1, 10)
        WHEN 0 THEN '上海'
        WHEN 1 THEN '北京'
        WHEN 2 THEN '广州'
        WHEN 3 THEN '深圳'
        WHEN 4 THEN '成都'
        WHEN 5 THEN '杭州'
        WHEN 6 THEN '南京'
        WHEN 7 THEN '武汉'
        WHEN 8 THEN '西安'
        ELSE '重庆'
    END,
    `arrival_city` = CASE MOD(CAST(SUBSTRING(`flight_no`, 3) AS UNSIGNED), 10)
        WHEN 1 THEN '广州'
        WHEN 2 THEN '深圳'
        WHEN 3 THEN '成都'
        WHEN 4 THEN '杭州'
        WHEN 5 THEN '南京'
        WHEN 6 THEN '武汉'
        WHEN 7 THEN '西安'
        WHEN 8 THEN '重庆'
        WHEN 9 THEN '上海'
        ELSE '北京'
    END,
    `departure_airport` = CASE MOD(CAST(SUBSTRING(`flight_no`, 3) AS UNSIGNED) - 1, 10)
        WHEN 0 THEN '上海虹桥T2'
        WHEN 1 THEN '北京首都T3'
        WHEN 2 THEN '广州白云T2'
        WHEN 3 THEN '深圳宝安T3'
        WHEN 4 THEN '成都天府T2'
        WHEN 5 THEN '杭州萧山T4'
        WHEN 6 THEN '南京禄口T1'
        WHEN 7 THEN '武汉天河T3'
        WHEN 8 THEN '西安咸阳T5'
        ELSE '重庆江北T3'
    END,
    `arrival_airport` = CASE MOD(CAST(SUBSTRING(`flight_no`, 3) AS UNSIGNED), 10)
        WHEN 1 THEN '广州白云T1'
        WHEN 2 THEN '深圳宝安T3'
        WHEN 3 THEN '成都双流T2'
        WHEN 4 THEN '杭州萧山T3'
        WHEN 5 THEN '南京禄口T2'
        WHEN 6 THEN '武汉天河T2'
        WHEN 7 THEN '西安咸阳T3'
        WHEN 8 THEN '重庆江北T2'
        WHEN 9 THEN '上海浦东T2'
        ELSE '北京大兴'
    END,
    `cabin_class` = CASE MOD(CAST(SUBSTRING(`flight_no`, 3) AS UNSIGNED) - 1, 3)
        WHEN 0 THEN '经济舱'
        WHEN 1 THEN '超级经济舱'
        ELSE '商务舱'
    END,
    `baggage_policy` = CASE MOD(CAST(SUBSTRING(`flight_no`, 3) AS UNSIGNED) - 1, 3)
        WHEN 0 THEN '免费托运20KG'
        WHEN 1 THEN '免费托运25KG'
        ELSE '免费托运30KG'
    END,
    `refund_policy` = CASE MOD(CAST(SUBSTRING(`flight_no`, 3) AS UNSIGNED) - 1, 3)
        WHEN 0 THEN '起飞前可免费改期一次'
        WHEN 1 THEN '起飞前退票收取手续费'
        ELSE '起飞前可改可退'
    END
WHERE `flight_no` REGEXP '^TP[0-9]{4}$';

UPDATE `train_ticket`
SET
    `train_type` = CASE MOD(CAST(SUBSTRING(`train_no`, 2) AS UNSIGNED) - 2001, 4)
        WHEN 0 THEN '高铁'
        WHEN 1 THEN '动车'
        WHEN 2 THEN '城际'
        ELSE '直达特快'
    END,
    `departure_city` = CASE MOD(CAST(SUBSTRING(`train_no`, 2) AS UNSIGNED) - 2001, 10)
        WHEN 0 THEN '上海'
        WHEN 1 THEN '北京'
        WHEN 2 THEN '广州'
        WHEN 3 THEN '深圳'
        WHEN 4 THEN '成都'
        WHEN 5 THEN '杭州'
        WHEN 6 THEN '南京'
        WHEN 7 THEN '武汉'
        WHEN 8 THEN '西安'
        ELSE '重庆'
    END,
    `arrival_city` = CASE MOD(CAST(SUBSTRING(`train_no`, 2) AS UNSIGNED) - 2000, 10)
        WHEN 1 THEN '广州'
        WHEN 2 THEN '深圳'
        WHEN 3 THEN '成都'
        WHEN 4 THEN '杭州'
        WHEN 5 THEN '南京'
        WHEN 6 THEN '武汉'
        WHEN 7 THEN '西安'
        WHEN 8 THEN '重庆'
        WHEN 9 THEN '上海'
        ELSE '北京'
    END,
    `departure_station` = CASE MOD(CAST(SUBSTRING(`train_no`, 2) AS UNSIGNED) - 2001, 10)
        WHEN 0 THEN '上海虹桥'
        WHEN 1 THEN '北京南'
        WHEN 2 THEN '广州南'
        WHEN 3 THEN '深圳北'
        WHEN 4 THEN '成都东'
        WHEN 5 THEN '杭州东'
        WHEN 6 THEN '南京南'
        WHEN 7 THEN '武汉站'
        WHEN 8 THEN '西安北'
        ELSE '重庆西'
    END,
    `arrival_station` = CASE MOD(CAST(SUBSTRING(`train_no`, 2) AS UNSIGNED) - 2000, 10)
        WHEN 1 THEN '广州南'
        WHEN 2 THEN '深圳北'
        WHEN 3 THEN '成都东'
        WHEN 4 THEN '杭州东'
        WHEN 5 THEN '南京南'
        WHEN 6 THEN '武汉站'
        WHEN 7 THEN '西安北'
        WHEN 8 THEN '重庆北'
        WHEN 9 THEN '上海虹桥'
        ELSE '北京丰台'
    END
WHERE `train_no` REGEXP '^[GDCZ][0-9]{4}$'
  AND CAST(SUBSTRING(`train_no`, 2) AS UNSIGNED) BETWEEN 2001 AND 2120;

UPDATE `hotel`
SET
    `hotel_name` = CONCAT(
        CASE MOD(CAST(RIGHT(`hotel_name`, 3) AS UNSIGNED) - 1, 10)
            WHEN 0 THEN '上海'
            WHEN 1 THEN '北京'
            WHEN 2 THEN '广州'
            WHEN 3 THEN '深圳'
            WHEN 4 THEN '成都'
            WHEN 5 THEN '杭州'
            WHEN 6 THEN '南京'
            WHEN 7 THEN '武汉'
            WHEN 8 THEN '西安'
            ELSE '重庆'
        END,
        '精选酒店',
        RIGHT(`hotel_name`, 3)
    ),
    `city` = CASE MOD(CAST(RIGHT(`hotel_name`, 3) AS UNSIGNED) - 1, 10)
        WHEN 0 THEN '上海'
        WHEN 1 THEN '北京'
        WHEN 2 THEN '广州'
        WHEN 3 THEN '深圳'
        WHEN 4 THEN '成都'
        WHEN 5 THEN '杭州'
        WHEN 6 THEN '南京'
        WHEN 7 THEN '武汉'
        WHEN 8 THEN '西安'
        ELSE '重庆'
    END,
    `district` = CASE MOD(CAST(RIGHT(`hotel_name`, 3) AS UNSIGNED) - 1, 5)
        WHEN 0 THEN '市中心'
        WHEN 1 THEN '商务区'
        WHEN 2 THEN '景区附近'
        WHEN 3 THEN '高铁站商圈'
        ELSE '江景休闲区'
    END,
    `address` = CONCAT('演示大道', 100 + CAST(RIGHT(`hotel_name`, 3) AS UNSIGNED), '号'),
    `description` = CONCAT('适合本地演示的中文酒店样例', RIGHT(`hotel_name`, 3), '，可用于筛选、详情浏览和下单流程。')
WHERE `hotel_name` REGEXP '^[A-Za-z]+ Demo Hotel [0-9]{3}$';

UPDATE `hotel_room`
SET
    `room_name` = CASE `room_name`
        WHEN 'Comfort Queen Room' THEN '舒适大床房'
        WHEN 'Superior Twin Room' THEN '高级双床房'
        WHEN 'Family Suite' THEN '家庭套房'
        ELSE `room_name`
    END,
    `bed_type` = CASE `room_name`
        WHEN 'Comfort Queen Room' THEN '1张大床'
        WHEN 'Superior Twin Room' THEN '2张单人床'
        WHEN 'Family Suite' THEN '1张大床+1张单人床'
        ELSE `bed_type`
    END,
    `breakfast` = CASE `room_name`
        WHEN 'Comfort Queen Room' THEN '含双早'
        WHEN 'Superior Twin Room' THEN '含双早'
        WHEN 'Family Suite' THEN '含三早'
        ELSE `breakfast`
    END,
    `room_area` = CASE `room_name`
        WHEN 'Comfort Queen Room' THEN '28平方米'
        WHEN 'Superior Twin Room' THEN '32平方米'
        WHEN 'Family Suite' THEN '45平方米'
        ELSE `room_area`
    END,
    `cancel_rule` = CASE `room_name`
        WHEN 'Comfort Queen Room' THEN '入住前一天18:00前可免费取消'
        WHEN 'Superior Twin Room' THEN '入住前一天18:00前可免费取消'
        WHEN 'Family Suite' THEN '入住前一天12:00前可免费取消'
        ELSE `cancel_rule`
    END
WHERE `room_name` IN ('Comfort Queen Room', 'Superior Twin Room', 'Family Suite');

UPDATE `hotel_room` r
JOIN `hotel` h ON h.`id` = r.`hotel_id`
SET
    r.`price` = CASE
        WHEN r.`room_name` IN ('舒适大床房', 'Comfort Queen Room') THEN 328 + MOD(CAST(RIGHT(h.`hotel_name`, 3) AS UNSIGNED), 12) * 17 + MOD(CAST(RIGHT(h.`hotel_name`, 3) AS UNSIGNED), 5) * 6
        WHEN r.`room_name` IN ('高级双床房', 'Superior Twin Room') THEN 418 + MOD(CAST(RIGHT(h.`hotel_name`, 3) AS UNSIGNED), 12) * 19 + MOD(CAST(RIGHT(h.`hotel_name`, 3) AS UNSIGNED), 4) * 8
        WHEN r.`room_name` IN ('家庭套房', 'Family Suite') THEN 638 + MOD(CAST(RIGHT(h.`hotel_name`, 3) AS UNSIGNED), 12) * 23 + MOD(CAST(RIGHT(h.`hotel_name`, 3) AS UNSIGNED), 3) * 12
        ELSE r.`price`
    END,
    r.`stock` = CASE
        WHEN r.`room_name` IN ('舒适大床房', 'Comfort Queen Room') THEN 10 + MOD(CAST(RIGHT(h.`hotel_name`, 3) AS UNSIGNED), 11)
        WHEN r.`room_name` IN ('高级双床房', 'Superior Twin Room') THEN 6 + MOD(CAST(RIGHT(h.`hotel_name`, 3) AS UNSIGNED), 9)
        WHEN r.`room_name` IN ('家庭套房', 'Family Suite') THEN 4 + MOD(CAST(RIGHT(h.`hotel_name`, 3) AS UNSIGNED), 6)
        ELSE r.`stock`
    END
WHERE h.`hotel_name` REGEXP '.*[0-9]{3}$'
  AND r.`room_name` IN ('舒适大床房', 'Comfort Queen Room', '高级双床房', 'Superior Twin Room', '家庭套房', 'Family Suite');

UPDATE `tour_package`
SET
    `package_name` = CONCAT(
        CASE MOD(CAST(RIGHT(`package_name`, 3) AS UNSIGNED) - 1, 10)
            WHEN 0 THEN '三亚'
            WHEN 1 THEN '丽江'
            WHEN 2 THEN '张家界'
            WHEN 3 THEN '厦门'
            WHEN 4 THEN '青岛'
            WHEN 5 THEN '桂林'
            WHEN 6 THEN '哈尔滨'
            WHEN 7 THEN '苏州'
            WHEN 8 THEN '昆明'
            ELSE '大理'
        END,
        '精选线路',
        RIGHT(`package_name`, 3)
    ),
    `destination` = CASE MOD(CAST(RIGHT(`package_name`, 3) AS UNSIGNED) - 1, 10)
        WHEN 0 THEN '三亚'
        WHEN 1 THEN '丽江'
        WHEN 2 THEN '张家界'
        WHEN 3 THEN '厦门'
        WHEN 4 THEN '青岛'
        WHEN 5 THEN '桂林'
        WHEN 6 THEN '哈尔滨'
        WHEN 7 THEN '苏州'
        WHEN 8 THEN '昆明'
        ELSE '大理'
    END,
    `departure_city` = CASE MOD(CAST(RIGHT(`package_name`, 3) AS UNSIGNED), 10)
        WHEN 1 THEN '广州'
        WHEN 2 THEN '深圳'
        WHEN 3 THEN '成都'
        WHEN 4 THEN '杭州'
        WHEN 5 THEN '南京'
        WHEN 6 THEN '武汉'
        WHEN 7 THEN '西安'
        WHEN 8 THEN '重庆'
        WHEN 9 THEN '上海'
        ELSE '北京'
    END,
    `description` = CONCAT('中文旅游产品演示样例', RIGHT(`package_name`, 3), '，适合用于线路浏览、详情展示与订单演示。')
WHERE `package_name` REGEXP '^[A-Za-z]+ Demo Tour [0-9]{3}$';
