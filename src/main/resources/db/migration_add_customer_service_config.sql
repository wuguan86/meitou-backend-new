CREATE TABLE IF NOT EXISTS `customer_service_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `site_id` bigint(20) NOT NULL COMMENT '站点ID',
  `qr_code_url` varchar(500) DEFAULT NULL COMMENT '二维码图片URL',
  `contact_text` text DEFAULT NULL COMMENT '联系方式文本说明',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_site_id` (`site_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服配置表';
