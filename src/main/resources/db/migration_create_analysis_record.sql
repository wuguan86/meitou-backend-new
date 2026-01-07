CREATE TABLE `analysis_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `type` varchar(20) NOT NULL COMMENT '类型：image-图片分析, video-视频分析, prompt-提示词优化',
  `content` text COMMENT '内容：图片URL、视频URL或提示词',
  `result` text COMMENT '分析结果或优化后的提示词',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '状态：0-进行中, 1-成功, 2-失败',
  `error_msg` text COMMENT '错误信息',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除',
  `site_id` bigint NOT NULL COMMENT '站点ID',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_site_id` (`site_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分析记录表';
