-- Add is_publish column to generation_records table
ALTER TABLE `generation_records` ADD COLUMN `is_publish` varchar(1) DEFAULT '0' COMMENT '是否发布：0-否，1-是';
