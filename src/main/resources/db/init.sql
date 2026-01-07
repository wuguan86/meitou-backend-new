/*
Navicat MySQL Data Transfer

Source Server         : test1
Source Server Version : 80044
Source Host           : 119.91.142.187:3306
Source Database       : meitou_admin

Target Server Type    : MYSQL
Target Server Version : 80044
File Encoding         : 65001

Date: 2025-12-27 16:59:54
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for api_interfaces
-- ----------------------------
DROP TABLE IF EXISTS `api_interfaces`;
CREATE TABLE `api_interfaces` (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '接口ID',
                                  `platform_id` bigint NOT NULL COMMENT '平台ID',
                                  `url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '接口URL',
                                  `method` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'POST' COMMENT '请求方法：GET，POST',
                                  `response_mode` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'JSON' COMMENT '响应模式：JSON，Stream，Result',
                                  `headers` json DEFAULT NULL COMMENT '请求头配置（JSON）',
                                  `parameters_json` text COLLATE utf8mb4_unicode_ci COMMENT '参数配置（JSON）',
                                  `param_docs` json DEFAULT NULL COMMENT '参数文档（JSON数组）',
                                  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
                                  `site_id` bigint NOT NULL COMMENT '站点ID',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_platform_id` (`platform_id`),
                                  KEY `idx_site_id` (`site_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API接口表';

-- ----------------------------
-- Records of api_interfaces
-- ----------------------------
INSERT INTO `api_interfaces` VALUES ('6', '5', 'https://grsai.dakka.com.cn/v1/draw/completions', 'POST', 'JSON', '{\"Content-Type\": \"application/json\", \"Authorization\": \"Bearer apikey\"}', '{}', '[]', '2025-12-25 08:54:14', '2025-12-27 00:05:59', '1', '1');
INSERT INTO `api_interfaces` VALUES ('7', '5', 'https://grsai.dakka.com.cn/v1/draw/completions', 'POST', 'JSON', '{\"Content-Type\": \"application/json\", \"Authorization\": \"Bearer apikey\"}', '{}', '[]', '2025-12-25 09:26:05', '2025-12-27 00:05:59', '1', '1');
INSERT INTO `api_interfaces` VALUES ('8', '5', 'https://grsai.dakka.com.cn/v1/draw/completions', 'POST', 'JSON', '{}', '{}', '[]', '2025-12-25 09:32:43', '2025-12-27 00:05:59', '1', '1');
INSERT INTO `api_interfaces` VALUES ('9', '6', 'https://grsai.dakka.com.cn/v1/draw/completions', 'POST', 'JSON', '{\"Content-Type\": \"application/json\", \"Authorization\": \"Bearer apikey\"}', '{}', '[]', '2025-12-25 21:17:17', '2025-12-27 00:05:59', '1', '1');
INSERT INTO `api_interfaces` VALUES ('10', '6', 'https://grsai.dakka.com.cn/v1/draw/completions', 'POST', 'JSON', '{\"Content-Type\": \"application/json\", \"Authorization\": \"Bearer apikey\"}', '{}', '[]', '2025-12-25 21:58:16', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `api_interfaces` VALUES ('11', '5', 'https://grsai.dakka.com.cn/v1/draw/completions', 'POST', 'JSON', '{\"Content-Type\": \"application/json\", \"Authorization\": \"Bearer apikey\"}', '{}', '[]', '2025-12-25 22:13:45', '2025-12-27 08:33:17', '1', '1');
INSERT INTO `api_interfaces` VALUES ('12', '7', 'https://grsai.dakka.com.cn/v1/video/veo', 'POST', 'JSON', '{\"Content-Type\": \"application/json\", \"Authorization\": \"Bearer apikey\"}', '{}', '[]', '2025-12-25 22:59:32', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `api_interfaces` VALUES ('13', '8', 'https://grsai.dakka.com.cn/v1/video/veo', 'POST', 'JSON', '{\"Content-Type\": \"application/json\", \"Authorization\": \"Bearer apikey\"}', '{}', '[]', '2025-12-26 09:17:54', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `api_interfaces` VALUES ('14', '5', 'https://grsai.dakka.com.cn/v1/draw/completions', 'POST', 'JSON', '{\"Content-Type\": \"application/json\", \"Authorization\": \"Bearer apikey\"}', '{}', '[]', '2025-12-27 08:31:50', '2025-12-27 08:33:31', '1', '1');
INSERT INTO `api_interfaces` VALUES ('15', '5', 'https://grsai.dakka.com.cn/v1/draw/completions', 'POST', 'JSON', '{\"Content-Type\": \"application/json\", \"Authorization\": \"Bearer apikey\"}', '{}', '[]', '2025-12-27 08:32:04', '2025-12-27 08:33:41', '1', '2');
INSERT INTO `api_interfaces` VALUES ('16', '5', 'https://grsai.dakka.com.cn/v1/draw/completions', 'POST', 'JSON', '{\"Content-Type\": \"application/json\", \"Authorization\": \"Bearer apikey\"}', '{}', '[]', '2025-12-27 08:32:14', '2025-12-27 08:32:14', '0', '1');

-- ----------------------------
-- Table structure for api_platforms
-- ----------------------------
DROP TABLE IF EXISTS `api_platforms`;
CREATE TABLE `api_platforms` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '平台ID',
                                 `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台名称',
                                 `alias` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '别名',
                                 `api_key` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'API密钥',
                                 `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用：0-否，1-是',
                                 `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
                                 `node_info` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '节点信息：overseas-海外节点，domestic-国内直连，host-Host+接口',
                                 `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标',
                                 `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
                                 `site_id` bigint NOT NULL COMMENT '站点ID',
                                 `supported_models` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '支持的模型列表',
                                 `type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'API类型：image_analysis-图片分析，video_analysis-视频分析，txt2img-文生图，img2img-图生图，txt2video-文生视频，img2video-图生视频，voice_clone-声音克隆',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_is_enabled` (`is_enabled`),
                                 KEY `idx_site_id` (`site_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API平台表';

-- ----------------------------
-- Records of api_platforms
-- ----------------------------
INSERT INTO `api_platforms` VALUES ('5', 'grsais', 'grsai', 'h2j2jPSe4ZJgF/EKx0hyQGu2i173z6Ijy1Et/wp6YiJjRObMHgQLm2N2gdL14Xlw', '1', null, 'overseas', null, '2025-12-25 06:28:24', '2025-12-27 00:05:59', '0', '1', 'gpt-image-1.5', 'txt2img');
INSERT INTO `api_platforms` VALUES ('6', 'grsai', 'grsai', 'h2j2jPSe4ZJgF/EKx0hyQGu2i173z6Ijy1Et/wp6YiJjRObMHgQLm2N2gdL14Xlw', '1', null, 'overseas', null, '2025-12-25 21:17:17', '2025-12-27 00:05:59', '0', '1', 'sora-image#gpt-image-1.5', 'img2img');
INSERT INTO `api_platforms` VALUES ('7', 'grsai', 'grsai', 'h2j2jPSe4ZJgF/EKx0hyQGu2i173z6Ijy1Et/wp6YiJjRObMHgQLm2N2gdL14Xlw', '1', null, 'overseas', null, '2025-12-25 22:59:32', '2025-12-27 00:05:59', '0', '1', 'veo3-fast#veo3-pro', 'txt2video');
INSERT INTO `api_platforms` VALUES ('8', 'grsai', 'grsai', 'h2j2jPSe4ZJgF/EKx0hyQGu2i173z6Ijy1Et/wp6YiJjRObMHgQLm2N2gdL14Xlw', '1', null, 'overseas', null, '2025-12-26 09:17:54', '2025-12-27 00:05:59', '0', '1', 'veo3-fast#veo3.1-fast', 'img2video');

-- ----------------------------
-- Table structure for asset_folders
-- ----------------------------
DROP TABLE IF EXISTS `asset_folders`;
CREATE TABLE `asset_folders` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件夹ID',
                                 `name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件夹名称',
                                 `folder_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件夹路径（完整路径，如 "images/2024/01" 或 "videos/promos"）',
                                 `parent_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '父文件夹路径（如果为NULL或空字符串，则表示根目录下的文件夹）',
                                 `user_id` bigint NOT NULL COMMENT '用户ID',
                                 `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
                                 `site_id` bigint NOT NULL COMMENT '站点ID',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_user_folder_path` (`user_id`,`folder_path`,`deleted`),
                                 KEY `idx_user_id` (`user_id`),
                                 KEY `idx_folder_path` (`folder_path`),
                                 KEY `idx_parent_path` (`parent_path`),
                                 KEY `idx_site_id` (`site_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产文件夹表';

-- ----------------------------
-- Records of asset_folders
-- ----------------------------
INSERT INTO `asset_folders` VALUES ('4', '风景', '风景', null, '3', '2025-12-25 11:07:03', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `asset_folders` VALUES ('5', '城市', '城市', null, '3', '2025-12-25 11:07:25', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `asset_folders` VALUES ('6', '新文件夹', '新文件夹', null, '3', '2025-12-25 11:11:35', '2025-12-27 00:05:59', '1', '1');
INSERT INTO `asset_folders` VALUES ('7', 'ui', 'ui', null, '5', '2025-12-25 11:14:36', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `asset_folders` VALUES ('8', 'di', 'di', null, '5', '2025-12-25 11:14:46', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `asset_folders` VALUES ('9', 'ci', 'ui/ci', 'ui', '5', '2025-12-25 11:15:00', '2025-12-27 00:05:59', '0', '1');

-- ----------------------------
-- Table structure for backend_accounts
-- ----------------------------
DROP TABLE IF EXISTS `backend_accounts`;
CREATE TABLE `backend_accounts` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '账号ID',
                                    `email` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '邮箱账号',
                                    `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（加密）',
                                    `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'operator' COMMENT '角色权限：admin-超级管理员，operator-运营人员，viewer-访客',
                                    `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active' COMMENT '状态：active-正常，locked-已锁定',
                                    `last_login` datetime DEFAULT NULL COMMENT '最后登录时间',
                                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
                                    `site_id` bigint NOT NULL COMMENT '站点ID',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_email` (`email`),
                                    KEY `idx_role` (`role`),
                                    KEY `idx_status` (`status`),
                                    KEY `idx_site_id` (`site_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台账号表';

-- ----------------------------
-- Records of backend_accounts
-- ----------------------------
INSERT INTO `backend_accounts` VALUES ('1', 'admin@meitou.com', '$2a$10$h2owbSKohzqb1fBlS7ZCRuImLpDzKn8fQQbk7nATiGBAM0Vo81wnS', 'admin', 'active', '2025-12-24 16:20:51', '2025-12-20 00:34:14', '2025-12-27 00:12:11', '0', '0');
INSERT INTO `backend_accounts` VALUES ('2', 'wuguan@163.com', '$2a$10$sZ5TudkljoS32kJvz0iXY.wI86zicamzNbMlCjCQaIl08Rt2QfJUm', 'operator', 'active', '2025-12-20 10:07:16', '2025-12-20 10:06:01', '2025-12-27 00:12:14', '0', '0');

-- ----------------------------
-- Table structure for generation_records
-- ----------------------------
DROP TABLE IF EXISTS `generation_records`;
CREATE TABLE `generation_records` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
                                      `user_id` bigint NOT NULL COMMENT '用户ID',
                                      `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
                                      `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型：txt2img-文生图，img2img-图生图，txt2video-文生视频，img2video-图生视频，voice-声音克隆',
                                      `model` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模型名称',
                                      `prompt` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提示词',
                                      `cost` int NOT NULL DEFAULT '0' COMMENT '消耗积分',
                                      `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'processing' COMMENT '状态：success-成功，failed-失败，processing-生成中',
                                      `content_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生成内容URL',
                                      `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
                                      `site_id` bigint NOT NULL COMMENT '站点ID',
                                      PRIMARY KEY (`id`),
                                      KEY `idx_user_id` (`user_id`),
                                      KEY `idx_type` (`type`),
                                      KEY `idx_status` (`status`),
                                      KEY `idx_site_id` (`site_id`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生成记录表';

-- ----------------------------
-- Records of generation_records
-- ----------------------------
INSERT INTO `generation_records` VALUES ('8', '3', '用户_2706', 'txt2img', 'gpt-image-1.5', '一只东北虎在雪山', '10', 'success', 'https://file84.grsai.com/file/6ba42865e0cb42e6b2b7024f0b6b94dd.png', '2025-12-25 09:52:21', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `generation_records` VALUES ('9', '3', '用户_2706', 'txt2img', 'gpt-image-1.5', '一只可爱的小猫在草原。', '10', 'success', 'https://file87.grsai.com/file/1456d5e01b6743e7bade9fe10a1ecfc7.png', '2025-12-25 10:45:00', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `generation_records` VALUES ('10', '3', '用户_2706', 'txt2img', 'gpt-image-1.5', '一个东方美女骑着东北虎，在古老的城堡附近', '10', 'success', 'https://file84.grsai.com/file/86bb60233bca4bc3af68f67dc22acc01.png', '2025-12-25 21:11:21', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `generation_records` VALUES ('11', '3', '用户_2706', 'txt2img', 'gpt-image-1.5', '一只哈士奇在演讲。', '10', 'success', 'https://file26.grsai.com/file/c62d727662114e0da6173d82dba724b7.png', '2025-12-25 21:49:02', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `generation_records` VALUES ('20', '3', '用户_2706', 'txt2img', 'gpt-image-1.5', '一只拟人化的橘猫在草原', '10', 'success', 'https://file86.grsai.com/file/8f0336e3105b47dd98958f18ac462119.png', '2025-12-25 22:08:34', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `generation_records` VALUES ('21', '3', '用户_2706', 'txt2img', 'gpt-image-1.5', '一只拟人化的橘猫在草原', '10', 'success', 'https://file88.grsai.com/file/100a533bd3104ebab704fb6794cd3b5d.png', '2025-12-25 22:11:06', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `generation_records` VALUES ('24', '3', '用户_2706', 'txt2img', 'gpt-image-1.5', '一只拟人化的橘猫在草原', '10', 'success', 'https://file85.grsai.com/file/444a0425879e4500aecf89b8de331cf3.png', '2025-12-25 22:19:18', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `generation_records` VALUES ('25', '3', '用户_2706', 'img2img', 'sora-image', '她在教室看书', '15', 'success', 'https://file86.grsai.com/file/c87b444de9ea46b18b9444b5b0f0f563.png', '2025-12-25 22:20:30', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `generation_records` VALUES ('26', '3', '用户_2706', 'img2img', 'gpt-image-1.5', '她在河边看着河里，阳光明媚', '15', 'success', 'https://file84.grsai.com/file/cbb0469034ed443495b97b348f2b7a5a.png', '2025-12-25 22:30:45', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `generation_records` VALUES ('27', '3', '用户_2706', 'img2img', 'gpt-image-1.5', '她在河边看着河里，阳光明媚', '15', 'success', 'https://file84.grsai.com/file/fcfc5510937a4271bfb27dd21eeddc5a.png', '2025-12-25 22:41:11', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `generation_records` VALUES ('28', '3', '用户_2706', 'img2img', 'gpt-image-1.5', '她在河边看着河里，阳光明媚', '15', 'success', 'https://file88.grsai.com/file/f5f2fd8cc0d844269d3ea7e0eb1b5cb9.png', '2025-12-25 22:47:58', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `generation_records` VALUES ('37', '3', '用户_2706', 'txt2video', 'veo3-fast', '未来世界', '20', 'success', 'https://file82.grsai.com/file/e787794c45ad466fb8ca5ba0bd36317c.mp4', '2025-12-26 09:13:20', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `generation_records` VALUES ('39', '3', '用户_2706', 'img2video', 'veo3.1-fast', '她在未来世界', '20', 'success', 'https://file86.grsai.com/file/3f8f8c22a6fe47eab0851208e8eb4bd0.mp4', '2025-12-26 09:29:34', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `generation_records` VALUES ('40', '3', '用户_2706', 'txt2img', 'gpt-image-1.5', '一个可爱的兔子在森林', '10', 'success', 'https://file83.grsai.com/file/28a17f8e66434eb096329f84a969c02e.png', '2025-12-26 14:56:36', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `generation_records` VALUES ('41', '3', '用户_2706', 'txt2img', 'gpt-image-1.5', '一只拟人化的老虎在张家界', '10', 'success', 'https://file86.aitohumanize.com/file/d14da2ba51fc442fbcd0150da7c717bd.png', '2025-12-27 14:43:03', '2025-12-27 14:43:03', '0', '1');
INSERT INTO `generation_records` VALUES ('42', '3', '用户_2706', 'txt2img', 'gpt-image-1.5', '一只拟人化橘猫在桂林', '10', 'success', 'https://file87.aitohumanize.com/file/64ecd6e4d8274b0b9ea4ce286bf9b36d.png', '2025-12-27 16:16:09', '2025-12-27 16:16:09', '0', '1');

-- ----------------------------
-- Table structure for invitation_codes
-- ----------------------------
DROP TABLE IF EXISTS `invitation_codes`;
CREATE TABLE `invitation_codes` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '邀请码ID',
                                    `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '邀请码',
                                    `points` int NOT NULL DEFAULT '0' COMMENT '赠送积分',
                                    `used_count` int NOT NULL DEFAULT '0' COMMENT '已使用次数',
                                    `max_uses` int NOT NULL DEFAULT '1' COMMENT '最大使用次数',
                                    `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active' COMMENT '状态：active-有效，expired-已过期',
                                    `channel` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '使用渠道',
                                    `valid_start_date` date DEFAULT NULL COMMENT '有效期开始时间',
                                    `valid_end_date` date DEFAULT NULL COMMENT '有效期结束时间',
                                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
                                    `site_id` bigint NOT NULL COMMENT '站点ID',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_code` (`code`),
                                    KEY `idx_status` (`status`),
                                    KEY `idx_site_id` (`site_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请码表';

-- ----------------------------
-- Records of invitation_codes
-- ----------------------------
INSERT INTO `invitation_codes` VALUES ('1', 'INV0DATDX', '100', '0', '1', 'active', '默认渠道', '2025-12-20', '2026-12-20', '2025-12-20 09:41:35', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `invitation_codes` VALUES ('2', 'INVDUJ62J', '100', '0', '1', 'active', '默认渠道', '2025-12-20', '2026-12-20', '2025-12-20 15:04:49', '2025-12-27 00:05:59', '0', '1');

-- ----------------------------
-- Table structure for likes
-- ----------------------------
DROP TABLE IF EXISTS `likes`;
CREATE TABLE `likes` (
                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
                         `user_id` bigint NOT NULL COMMENT '用户ID',
                         `content_id` bigint NOT NULL COMMENT '发布内容ID（关联published_contents.id）',
                         `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         `site_id` int NOT NULL COMMENT '站点ID（多租户字段）',
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `uk_user_content` (`user_id`,`content_id`),
                         KEY `idx_user_id` (`user_id`),
                         KEY `idx_content_id` (`content_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞关系表';

-- ----------------------------
-- Records of likes
-- ----------------------------
INSERT INTO `likes` VALUES ('2', '3', '1', '2025-12-27 16:23:27', '1');

-- ----------------------------
-- Table structure for manual_configs
-- ----------------------------
DROP TABLE IF EXISTS `manual_configs`;
CREATE TABLE `manual_configs` (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '手册ID',
                                  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '手册标题',
                                  `url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '手册URL',
                                  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
                                  `site_id` bigint NOT NULL COMMENT '站点ID',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_site_id` (`site_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='手册配置表';

-- ----------------------------
-- Records of manual_configs
-- ----------------------------

-- ----------------------------
-- Table structure for marketing_ads
-- ----------------------------
DROP TABLE IF EXISTS `marketing_ads`;
CREATE TABLE `marketing_ads` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '广告ID',
                                 `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '广告标题',
                                 `image_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '广告图片URL',
                                 `start_date` date NOT NULL COMMENT '开始时间',
                                 `end_date` date NOT NULL COMMENT '结束时间',
                                 `link_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'external' COMMENT '跳转类型：external-外部网页，internal_rich-富文本详情',
                                 `link_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '跳转链接',
                                 `rich_content` text COLLATE utf8mb4_unicode_ci COMMENT '富文本内容',
                                 `summary` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '摘要描述',
                                 `tags` json DEFAULT NULL COMMENT '关联标签（JSON数组）',
                                 `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否激活：0-否，1-是',
                                 `position` int NOT NULL DEFAULT '1' COMMENT '广告位顺序：数字越小排序越靠前。',
                                 `is_full_screen` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否全屏：0-否，1-是',
                                 `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
                                 `site_id` bigint NOT NULL COMMENT '站点ID',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_position` (`position`),
                                 KEY `idx_is_active` (`is_active`),
                                 KEY `idx_site_id` (`site_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='营销广告表';

-- ----------------------------
-- Records of marketing_ads
-- ----------------------------
INSERT INTO `marketing_ads` VALUES ('3', '创意驱动，无限可能！', 'https://aitoutou.oss-cn-shenzhen.aliyuncs.com/images/98278f80c2004e2183a6980a12462120.jpg', '2025-12-14', '2026-02-28', 'external', 'https://www.baidu.com', null, 'Engine Upgrade v3.1.1', null, '1', '1', '1', '2025-12-21 09:34:14', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `marketing_ads` VALUES ('4', '文生图 2.2 震撼发布', 'https://aitoutou.oss-cn-shenzhen.aliyuncs.com/images/f028ded76d5e4b8883232d0ac82a2555.jpg', '2025-12-14', '2026-02-28', 'internal_rich', '', '详情', 'New Model', null, '1', '2', '1', '2025-12-21 10:17:19', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `marketing_ads` VALUES ('5', '视频生成加速 50%', 'https://aitoutou.oss-cn-shenzhen.aliyuncs.com/images/8ffca6b8e9ed4d8ea441e77f6fba7d64.jpg', '2025-12-14', '2026-03-29', 'internal_rich', '', 'ss', 'Efficiency', null, '1', '3', '1', '2025-12-21 10:27:58', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `marketing_ads` VALUES ('6', 'AI 研究院招募中', 'https://aitoutou.oss-cn-shenzhen.aliyuncs.com/images/10a4ea7d21094062a0bba14272e7d7ed.jpg', '2025-12-11', '2026-02-26', 'internal_rich', null, 'cc', 'Community', null, '1', '4', '1', '2025-12-26 10:00:08', '2025-12-27 00:05:59', '0', '1');

-- ----------------------------
-- Table structure for menu_configs
-- ----------------------------
DROP TABLE IF EXISTS `menu_configs`;
CREATE TABLE `menu_configs` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
                                `label` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '菜单标签',
                                `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '菜单代码',
                                `is_visible` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否可见：0-否，1-是',
                                `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
                                `site_id` bigint NOT NULL COMMENT '站点ID',
                                PRIMARY KEY (`id`),
                                KEY `idx_code` (`code`),
                                KEY `idx_site_id` (`site_id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单配置表';

-- ----------------------------
-- Records of menu_configs
-- ----------------------------
INSERT INTO `menu_configs` VALUES ('1', '图视分析', 'vision_analysis', '0', '2025-12-20 00:34:14', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `menu_configs` VALUES ('2', '文生图', 'txt2img', '1', '2025-12-20 00:34:14', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `menu_configs` VALUES ('3', '文生视频', 'txt2video', '1', '2025-12-20 00:34:14', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `menu_configs` VALUES ('4', '图生图', 'img2img', '1', '2025-12-20 00:34:14', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `menu_configs` VALUES ('5', '图生视频', 'img2video', '1', '2025-12-20 00:34:14', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `menu_configs` VALUES ('6', '声音克隆', 'voice_clone', '1', '2025-12-20 00:34:14', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `menu_configs` VALUES ('7', '图视分析', 'vision_analysis', '0', '2025-12-20 00:34:14', '2025-12-27 00:08:41', '0', '2');
INSERT INTO `menu_configs` VALUES ('8', '文生图', 'txt2img', '1', '2025-12-20 00:34:14', '2025-12-27 00:08:44', '0', '2');
INSERT INTO `menu_configs` VALUES ('9', '文生视频', 'txt2video', '1', '2025-12-20 00:34:14', '2025-12-27 00:08:46', '0', '2');
INSERT INTO `menu_configs` VALUES ('10', '图生图', 'img2img', '1', '2025-12-20 00:34:14', '2025-12-27 00:08:48', '0', '2');
INSERT INTO `menu_configs` VALUES ('11', '图生视频', 'img2video', '1', '2025-12-20 00:34:14', '2025-12-27 00:08:51', '0', '2');
INSERT INTO `menu_configs` VALUES ('12', '声音克隆', 'voice_clone', '1', '2025-12-20 00:34:14', '2025-12-27 00:08:53', '0', '2');
INSERT INTO `menu_configs` VALUES ('13', '图视分析', 'vision_analysis', '1', '2025-12-20 00:34:14', '2025-12-27 00:08:56', '0', '3');
INSERT INTO `menu_configs` VALUES ('14', '文生图', 'txt2img', '0', '2025-12-20 00:34:14', '2025-12-27 00:08:58', '0', '3');
INSERT INTO `menu_configs` VALUES ('15', '文生视频', 'txt2video', '1', '2025-12-20 00:34:14', '2025-12-27 00:09:00', '0', '3');
INSERT INTO `menu_configs` VALUES ('16', '图生图', 'img2img', '1', '2025-12-20 00:34:14', '2025-12-27 00:09:02', '0', '3');
INSERT INTO `menu_configs` VALUES ('17', '图生视频', 'img2video', '1', '2025-12-20 00:34:14', '2025-12-27 00:09:04', '0', '3');
INSERT INTO `menu_configs` VALUES ('18', '声音克隆', 'voice_clone', '1', '2025-12-20 00:34:14', '2025-12-27 00:09:07', '0', '3');

-- ----------------------------
-- Table structure for payment_configs
-- ----------------------------
DROP TABLE IF EXISTS `payment_configs`;
CREATE TABLE `payment_configs` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
                                   `payment_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '支付方式：wechat-微信支付，alipay-支付宝支付，bank_transfer-对公转账',
                                   `config_json` text COLLATE utf8mb4_unicode_ci COMMENT '配置信息（JSON格式）',
                                   `is_enabled` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否启用：0-否，1-是',
                                   `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
                                   `site_id` bigint NOT NULL COMMENT '站点ID',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_is_enabled` (`is_enabled`),
                                   KEY `idx_site_id` (`site_id`),
                                   KEY `uk_payment_type` (`payment_type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付配置表';

-- ----------------------------
-- Records of payment_configs
-- ----------------------------
INSERT INTO `payment_configs` VALUES ('1', 'wechat', null, '1', '2025-12-20 00:34:14', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `payment_configs` VALUES ('2', 'alipay', null, '1', '2025-12-20 00:34:14', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `payment_configs` VALUES ('3', 'bank_transfer', null, '0', '2025-12-20 00:34:14', '2025-12-27 00:05:59', '0', '1');
INSERT INTO `payment_configs` VALUES ('4', 'wechat', null, '1', '2025-12-20 00:34:14', '2025-12-20 00:34:14', '0', '2');
INSERT INTO `payment_configs` VALUES ('5', 'alipay', null, '1', '2025-12-20 00:34:14', '2025-12-20 00:34:14', '0', '2');
INSERT INTO `payment_configs` VALUES ('6', 'bank_transfer', null, '0', '2025-12-20 00:34:14', '2025-12-20 00:34:14', '0', '2');
INSERT INTO `payment_configs` VALUES ('7', 'wechat', null, '1', '2025-12-20 00:34:14', '2025-12-20 00:34:14', '0', '3');
INSERT INTO `payment_configs` VALUES ('8', 'alipay', null, '1', '2025-12-20 00:34:14', '2025-12-20 00:34:14', '0', '3');
INSERT INTO `payment_configs` VALUES ('9', 'bank_transfer', null, '0', '2025-12-20 00:34:14', '2025-12-20 00:34:14', '0', '3');

-- ----------------------------
-- Table structure for published_contents
-- ----------------------------
DROP TABLE IF EXISTS `published_contents`;
CREATE TABLE `published_contents` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '发布内容ID',
                                      `user_id` bigint NOT NULL COMMENT '用户ID',
                                      `user_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
                                      `user_avatar_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户头像URL',
                                      `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
                                      `description` text COLLATE utf8mb4_unicode_ci COMMENT '描述',
                                      `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型：image-图片，video-视频',
                                      `generation_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '生成类型：txt2img-文生图，img2img-图生图，txt2video-文生视频，img2video-图生视频',
                                      `content_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容URL',
                                      `thumbnail` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '缩略图URL',
                                      `generation_config` json DEFAULT NULL COMMENT '生成配置参数（JSON格式）',
                                      `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'published' COMMENT '状态：published-已发布，hidden-已隐藏',
                                      `is_pinned` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否置顶：0-否，1-是',
                                      `like_count` int NOT NULL DEFAULT '0' COMMENT '点赞数',
                                      `published_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
                                      `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
                                      `site_id` bigint NOT NULL COMMENT '站点ID（多租户字段）',
                                      PRIMARY KEY (`id`),
                                      KEY `idx_user_id` (`user_id`),
                                      KEY `idx_site_id` (`site_id`),
                                      KEY `idx_type` (`type`),
                                      KEY `idx_generation_type` (`generation_type`),
                                      KEY `idx_status` (`status`),
                                      KEY `idx_published_at` (`published_at`),
                                      KEY `idx_is_pinned` (`is_pinned`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发布内容表';

-- ----------------------------
-- Records of published_contents
-- ----------------------------
INSERT INTO `published_contents` VALUES ('1', '3', '用户_2706', 'https://api.dicebear.com/7.x/avataaars/svg?seed=3', '一只拟人化橘猫在桂林', '很帅', 'image', 'txt2img', 'https://file87.aitohumanize.com/file/64ecd6e4d8274b0b9ea4ce286bf9b36d.png', 'https://file87.aitohumanize.com/file/64ecd6e4d8274b0b9ea4ce286bf9b36d.png', '{\"model\": \"gpt-image-1.5\", \"prompt\": \"一只拟人化橘猫在桂林\", \"resolution\": \"1K\", \"aspectRatio\": \"3:2\"}', 'published', '0', '1', '2025-12-27 16:16:35', '2025-12-27 16:16:35', '2025-12-27 16:16:35', '0', '1');

-- ----------------------------
-- Table structure for recharge_configs
-- ----------------------------
DROP TABLE IF EXISTS `recharge_configs`;
CREATE TABLE `recharge_configs` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
                                    `exchange_rate` int NOT NULL DEFAULT '10' COMMENT '兑换比例（1元 = X算力）',
                                    `min_amount` int NOT NULL DEFAULT '5' COMMENT '最低充值金额（元）',
                                    `options_json` text COLLATE utf8mb4_unicode_ci COMMENT '充值选项列表（JSON格式，存储多个选项，每个选项包含 points 和 price）',
                                    `allow_custom` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用自定义金额：0-否，1-是',
                                    `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用：0-否，1-是',
                                    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
                                    `site_id` bigint NOT NULL COMMENT '站点ID',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_is_enabled` (`is_enabled`),
                                    KEY `idx_site_id` (`site_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值配置表';

-- ----------------------------
-- Records of recharge_configs
-- ----------------------------
INSERT INTO `recharge_configs` VALUES ('1', '100', '10', '[{\"points\":3000,\"price\":30},{\"points\":5000,\"price\":50},{\"points\":10000,\"price\":100},{\"points\":50000,\"price\":500},{\"points\":100000,\"price\":1000}]', '1', '1', '2025-12-26 11:23:05', '2025-12-27 00:06:00', '0', '1');
INSERT INTO `recharge_configs` VALUES ('2', '100', '5', '[{\"points\":300,\"price\":30},{\"points\":500,\"price\":50},{\"points\":1000,\"price\":100},{\"points\":5000,\"price\":500},{\"points\":10000,\"price\":1000}]', '1', '1', '2025-12-26 11:23:05', '2025-12-27 00:07:55', '0', '2');
INSERT INTO `recharge_configs` VALUES ('3', '100', '5', '[{\"points\":3000,\"price\":30},{\"points\":5000,\"price\":50},{\"points\":10000,\"price\":100},{\"points\":50000,\"price\":500},{\"points\":100000,\"price\":1000}]', '1', '1', '2025-12-26 11:23:05', '2025-12-27 00:07:59', '0', '3');

-- ----------------------------
-- Table structure for recharge_orders
-- ----------------------------
DROP TABLE IF EXISTS `recharge_orders`;
CREATE TABLE `recharge_orders` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
                                   `order_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单号（唯一）',
                                   `user_id` bigint NOT NULL COMMENT '用户ID',
                                   `amount` decimal(10,2) NOT NULL COMMENT '充值金额（元）',
                                   `points` int NOT NULL COMMENT '充值算力（积分）',
                                   `payment_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '支付方式：wechat-微信支付，alipay-支付宝支付',
                                   `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '订单状态：pending-待支付，paying-支付中，paid-已支付，cancelled-已取消，refunded-已退款，failed-支付失败',
                                   `third_party_order_no` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '第三方支付订单号',
                                   `callback_info` text COLLATE utf8mb4_unicode_ci COMMENT '支付回调信息（JSON格式）',
                                   `paid_at` datetime DEFAULT NULL COMMENT '支付时间',
                                   `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
                                   `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
                                   `site_id` bigint NOT NULL COMMENT '站点ID',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_order_no` (`order_no`),
                                   KEY `idx_user_id` (`user_id`),
                                   KEY `idx_status` (`status`),
                                   KEY `idx_payment_type` (`payment_type`),
                                   KEY `idx_created_at` (`created_at`),
                                   KEY `idx_site_id` (`site_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值订单表';

-- ----------------------------
-- Records of recharge_orders
-- ----------------------------

-- ----------------------------
-- Table structure for sites
-- ----------------------------
DROP TABLE IF EXISTS `sites`;
CREATE TABLE `sites` (
                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '站点ID（主键）',
                         `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '站点名称（如：医美类、电商类、生活服务类）',
                         `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '站点代码（如：medical、ecommerce、life）',
                         `domain` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '域名（用于识别站点，如：medical.example.com）',
                         `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active' COMMENT '状态：active-启用，disabled-禁用',
                         `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '站点描述',
                         `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                         `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `uk_code` (`code`),
                         UNIQUE KEY `uk_domain` (`domain`),
                         KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站点表（多租户）';

-- ----------------------------
-- Records of sites
-- ----------------------------
INSERT INTO `sites` VALUES ('1', '医美类', 'medical', 'medical.example.com', 'active', '医美类站点', '2025-12-26 17:07:32', '2025-12-27 11:36:00', '0');
INSERT INTO `sites` VALUES ('2', '电商类', 'ecommerce', 'ecommerce.example.com', 'active', '电商类站点', '2025-12-26 17:07:32', '2025-12-27 11:36:07', '0');
INSERT INTO `sites` VALUES ('3', '生活服务类', 'life', 'life.example.com', 'active', '生活服务类站点', '2025-12-26 17:07:32', '2025-12-27 11:36:12', '0');

-- ----------------------------
-- Table structure for user_assets
-- ----------------------------
DROP TABLE IF EXISTS `user_assets`;
CREATE TABLE `user_assets` (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '资产ID',
                               `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
                               `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型：image-图片，video-视频，audio-音频',
                               `folder` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件夹路径（关联asset_folders表的folder_path）',
                               `url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件URL',
                               `thumbnail` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '缩略图URL',
                               `user_id` bigint NOT NULL COMMENT '用户ID',
                               `user_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
                               `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'published' COMMENT '状态：published-展示中，hidden-已下架',
                               `is_pinned` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否置顶：0-否，1-是',
                               `like_count` int NOT NULL DEFAULT '0' COMMENT '点赞数',
                               `upload_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
                               `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
                               `site_id` bigint NOT NULL COMMENT '站点ID',
                               PRIMARY KEY (`id`),
                               KEY `idx_user_id` (`user_id`),
                               KEY `idx_type` (`type`),
                               KEY `idx_status` (`status`),
                               KEY `idx_folder` (`folder`),
                               KEY `idx_site_id` (`site_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户资产表';

-- ----------------------------
-- Records of user_assets
-- ----------------------------
INSERT INTO `user_assets` VALUES ('3', '老虎', 'image', 'ui/ci', 'https://aitoutou.oss-cn-shenzhen.aliyuncs.com/images/ui/ci/405055f4d9074f60ba7c631399d249a1.png', null, '5', '用户_4545', 'published', '0', '0', '2025-12-25 11:27:40', '2025-12-25 11:27:40', '2025-12-27 00:06:00', '0', '1');
INSERT INTO `user_assets` VALUES ('4', 'e0423c9a-5bd2-41ca-9e6e-1f3e6d2e9725', 'image', 'di', 'https://aitoutou.oss-cn-shenzhen.aliyuncs.com/images/di/9e2433dcef46485783b1e9a009388886.png', null, '5', '用户_4545', 'published', '0', '0', '2025-12-25 11:28:17', '2025-12-25 11:28:17', '2025-12-27 00:06:00', '0', '1');
INSERT INTO `user_assets` VALUES ('5', '000', 'video', 'di', 'https://aitoutou.oss-cn-shenzhen.aliyuncs.com/videos/di/f2ac3aa093454eb9a2ed8885246f0296.mp4', null, '5', '用户_4545', 'published', '0', '0', '2025-12-25 11:28:32', '2025-12-25 11:28:32', '2025-12-27 00:06:00', '0', '1');
INSERT INTO `user_assets` VALUES ('6', '001', 'video', 'ui/ci', 'https://aitoutou.oss-cn-shenzhen.aliyuncs.com/videos/ui/ci/fdb232e4912b4f358441ce9ef492af4a.mp4', null, '5', '用户_4545', 'published', '0', '0', '2025-12-25 11:29:44', '2025-12-25 11:29:44', '2025-12-27 00:06:00', '0', '1');
INSERT INTO `user_assets` VALUES ('7', 'bgm', 'audio', 'ui/ci', 'https://aitoutou.oss-cn-shenzhen.aliyuncs.com/audios/ui/ci/940ffa570ef54588bcf42722f850ac42.mp3', null, '5', '用户_4545', 'published', '0', '0', '2025-12-25 11:29:56', '2025-12-25 11:29:56', '2025-12-27 00:06:00', '0', '1');
INSERT INTO `user_assets` VALUES ('8', '22ba0d2bdf98649bfdd0050c37a1a1e6369886346e377d68cffcbee6436acf36', 'image', '城市', 'https://aitoutou.oss-cn-shenzhen.aliyuncs.com/images/城市/193e2ae6b0dc41aa8bb1657c57ae2684.png', null, '3', '用户_2706', 'published', '0', '0', '2025-12-25 18:43:12', '2025-12-25 18:43:12', '2025-12-27 00:06:00', '0', '1');
INSERT INTO `user_assets` VALUES ('9', 'jimeng-2025-07-12-4611-步入这间卧室，仿若踏入了一个被尘世遗忘的仙境。巨大的树干从地面蜿蜒至天花板，繁茂...', 'image', '风景', 'https://aitoutou.oss-cn-shenzhen.aliyuncs.com/images/风景/84e062612fc04a89b2c8d06eeb2cde02.jpeg', null, '3', '用户_2706', 'published', '0', '0', '2025-12-25 18:43:52', '2025-12-25 18:43:52', '2025-12-27 00:06:00', '0', '1');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                         `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
                         `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
                         `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（加密）',
                         `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
                         `wechat` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '微信号',
                         `company` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公司/机构',
                         `role` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色',
                         `balance` int NOT NULL DEFAULT '0' COMMENT '积分余额',
                         `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active' COMMENT '状态：active-正常，suspended-停用',
                         `avatar_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户头像URL',
                         `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                         `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
                         `site_id` bigint NOT NULL COMMENT '站点ID',
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `uk_email` (`email`),
                         KEY `idx_status` (`status`),
                         KEY `idx_site_id` (`site_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES ('1', 'wuguan@163.com', 'wuguan', '$2a$10$mjWtZDmEcPePjuQN36DKAurtpFpL5kY/uxlDjAoMm4ER2d2nHub4G', '18272728282', null, '咸宁恒鲜科技有限公司', null, '100', 'active', null, '2025-12-20 09:36:11', '2025-12-27 00:06:00', '0', '1');
INSERT INTO `users` VALUES ('2', 'wuguan1@163.com', 'wuguan1', '$2a$10$6iBHaO7k65/GvhfbU.1sI.Snqbh4nuC9HJ1aBD1Q7nKawwO20H4rS', '18293939494', null, 'ceshi', null, '0', 'active', null, '2025-12-20 09:37:30', '2025-12-27 00:06:00', '0', '1');
INSERT INTO `users` VALUES ('3', '18271482706@meitou.com', '用户_2706', 'NO_PASSWORD_CODE_LOGIN', '18271482706', null, null, 'user', '100', 'active', null, '2025-12-21 10:57:33', '2025-12-27 00:06:00', '0', '1');
INSERT INTO `users` VALUES ('4', '18727148270@meitou.com', '用户_8270', 'NO_PASSWORD_CODE_LOGIN', '18727148270', null, null, 'user', '0', 'active', null, '2025-12-21 11:29:01', '2025-12-27 00:06:00', '0', '1');
INSERT INTO `users` VALUES ('5', '18241414545@meitou.com', '用户_4545', 'NO_PASSWORD_CODE_LOGIN', '18241414545', null, null, 'user', '0', 'active', null, '2025-12-25 11:14:17', '2025-12-27 00:06:00', '0', '1');
