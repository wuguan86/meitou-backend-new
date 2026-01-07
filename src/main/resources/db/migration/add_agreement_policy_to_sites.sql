-- Add user_agreement and privacy_policy fields to sites table

USE `meitou_admin`;

ALTER TABLE `sites`
ADD COLUMN `user_agreement` LONGTEXT COMMENT '用户协议',
ADD COLUMN `privacy_policy` LONGTEXT COMMENT '隐私政策';
