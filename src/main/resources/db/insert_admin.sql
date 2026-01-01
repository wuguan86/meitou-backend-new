-- 插入默认管理员账号
-- 账号: admin@meitou.com
-- 密码: 123456
-- 角色: admin (超级管理员)
-- 站点: 0 (全局)

INSERT INTO `backend_accounts` 
(`email`, `password`, `role`, `status`, `created_at`, `updated_at`, `deleted`, `site_id`) 
VALUES 
('admin@meitou.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOcXBch.jZtH.', 'admin', 'active', NOW(), NOW(), 0, 0);
