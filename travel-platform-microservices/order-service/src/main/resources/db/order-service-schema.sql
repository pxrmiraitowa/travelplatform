CREATE DATABASE IF NOT EXISTS travel_order
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE travel_order;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(40) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    biz_type VARCHAR(20) NOT NULL,
    biz_id BIGINT NOT NULL,
    product_name VARCHAR(160) NOT NULL,
    product_summary VARCHAR(500),
    unit_price DECIMAL(12, 2) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    original_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(12, 2) NOT NULL,
    coupon_id BIGINT,
    coupon_name VARCHAR(100),
    order_status INT NOT NULL,
    travel_date DATE,
    contact_name VARCHAR(60) NOT NULL,
    contact_phone VARCHAR(30) NOT NULL,
    paid_at DATETIME,
    refund_reason VARCHAR(255),
    refunded_at DATETIME,
    remark VARCHAR(255),
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_orders_user_status (user_id, order_status),
    INDEX idx_orders_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表（保存商品下单快照）';
