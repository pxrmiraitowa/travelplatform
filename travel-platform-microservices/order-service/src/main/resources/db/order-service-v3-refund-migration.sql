-- 从订单微服务初版升级退款字段；执行一次。
USE travel_order;

ALTER TABLE orders
    ADD COLUMN refund_reason VARCHAR(255) NULL AFTER paid_at,
    ADD COLUMN refunded_at DATETIME NULL AFTER refund_reason;
