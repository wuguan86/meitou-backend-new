-- 发布功能和灵感广场相关表的迁移脚本
-- 执行时间：2025-01-XX

USE `meitou_admin`;

-- 1. 为用户表添加头像字段（如果不存在）
ALTER TABLE `users` 
ADD COLUMN IF NOT EXISTS `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '用户头像URL' AFTER `status`;

-- 2. 创建发布内容表
CREATE TABLE IF NOT EXISTS `published_contents` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '发布内容ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `user_name` VARCHAR(50) NOT NULL COMMENT '用户名',
  `user_avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '用户头像URL',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `description` TEXT DEFAULT NULL COMMENT '描述',
  `type` VARCHAR(20) NOT NULL COMMENT '类型：image-图片，video-视频',
  `generation_type` VARCHAR(20) NOT NULL COMMENT '生成类型：txt2img-文生图，img2img-图生图，txt2video-文生视频，img2video-图生视频',
  `content_url` VARCHAR(500) NOT NULL COMMENT '内容URL',
  `thumbnail` VARCHAR(500) DEFAULT NULL COMMENT '缩略图URL',
  `generation_config` JSON DEFAULT NULL COMMENT '生成配置参数（JSON格式）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'published' COMMENT '状态：published-已发布，hidden-已隐藏',
  `is_pinned` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `published_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `site_id` BIGINT NOT NULL COMMENT '站点ID（多租户字段）',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_site_id` (`site_id`),
  KEY `idx_type` (`type`),
  KEY `idx_generation_type` (`generation_type`),
  KEY `idx_status` (`status`),
  KEY `idx_published_at` (`published_at`),
  KEY `idx_is_pinned` (`is_pinned`),
  CONSTRAINT `fk_published_contents_site` FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`),
  CONSTRAINT `fk_published_contents_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发布内容表';

-- 3. 创建点赞关系表
CREATE TABLE IF NOT EXISTS `likes` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `content_id` BIGINT NOT NULL COMMENT '发布内容ID（关联published_contents.id）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_content` (`user_id`, `content_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_content_id` (`content_id`),
  CONSTRAINT `fk_likes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_likes_content` FOREIGN KEY (`content_id`) REFERENCES `published_contents` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞关系表';
