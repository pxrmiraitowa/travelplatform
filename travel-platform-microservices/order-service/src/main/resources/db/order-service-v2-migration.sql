-- 仅用于从单体版 orders 表升级到订单微服务快照结构；执行一次。
USE travel_order;

ALTER TABLE orders
    ADD COLUMN product_name VARCHAR(160) NULL AFTER biz_id,
    ADD COLUMN product_summary VARCHAR(500) NULL AFTER product_name,
    ADD COLUMN unit_price DECIMAL(12, 2) NULL AFTER product_summary,
    ADD COLUMN quantity INT NOT NULL DEFAULT 1 AFTER unit_price,
    ADD COLUMN paid_at DATETIME NULL AFTER travel_date;

-- 历史订单缺少商品快照时保留可读占位值，新订单始终写入真实快照。
UPDATE orders
SET product_name = COALESCE(product_name, CONCAT(biz_type, ' #', biz_id)),
    unit_price = COALESCE(unit_price, total_amount / GREATEST(quantity, 1));
