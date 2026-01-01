-- 为 recharge_orders 表添加 site_id 字段
ALTER TABLE `recharge_orders` ADD COLUMN `site_id` bigint NOT NULL DEFAULT 1 COMMENT '站点ID';
CREATE INDEX `idx_site_id` ON `recharge_orders` (`site_id`);
