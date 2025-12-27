-- 修复 payment_configs 表的唯一索引，支持多租户
-- 如果数据库已经创建，需要执行此SQL来修改唯一索引

USE `meitou_admin`;

-- 删除旧的唯一索引
ALTER TABLE `payment_configs` DROP INDEX `uk_payment_type`;

-- 添加新的唯一索引（支持多租户：payment_type + site_id 组合唯一）
ALTER TABLE `payment_configs` 
ADD UNIQUE KEY `uk_payment_type_site` (`payment_type`, `site_id`);

-- 添加 site_id 索引（如果不存在）
-- 检查索引是否存在，如果不存在则添加（MySQL不支持直接检查，需要手动执行）
-- ALTER TABLE `payment_configs` ADD KEY `idx_site_id` (`site_id`);

-- 添加外键约束（如果不存在）
-- ALTER TABLE `payment_configs` 
-- ADD CONSTRAINT `fk_payment_configs_site` FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`);

