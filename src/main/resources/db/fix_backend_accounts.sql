-- 修复 backend_accounts 表的 site_id 字段
-- 将 site_id 设置为默认值 0（0表示全局账号）

USE `meitou_admin`;

-- 修改 site_id 字段，设置默认值为 0
ALTER TABLE `backend_accounts` 
MODIFY COLUMN `site_id` BIGINT NOT NULL DEFAULT 0 COMMENT '站点ID（多租户字段，0表示全局账号）';

-- 将现有数据的 site_id 设置为 0（如果之前有值或为NULL的话）
UPDATE `backend_accounts` SET `site_id` = 0 WHERE `site_id` IS NULL OR `site_id` != 0;

