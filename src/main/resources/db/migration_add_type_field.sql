-- 数据库迁移脚本：为api_platforms表添加type字段
-- 执行时间：2024年
-- 说明：为API平台表添加类型字段，用于区分不同类型的API接口

-- 检查字段是否已存在，如果不存在则添加
SET @dbname = DATABASE();
SET @tablename = 'api_platforms';
SET @columnname = 'type';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT 1', -- 字段已存在，不执行任何操作
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' VARCHAR(50) DEFAULT NULL COMMENT ''API类型：image_analysis-图片分析，video_analysis-视频分析，txt2img-文生图，img2img-图生图，txt2video-文生视频，img2video-图生视频，voice_clone-声音克隆''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 为现有数据设置默认类型（可选）
-- 根据接口特征推断类型：如果有Stream类型的接口，则设置为txt2video，否则设置为txt2img
-- 注意：这个更新语句需要根据实际情况调整，因为需要查询api_interfaces表
-- UPDATE api_platforms ap
-- SET ap.type = (
--   CASE 
--     WHEN EXISTS (
--       SELECT 1 FROM api_interfaces ai 
--       WHERE ai.platform_id = ap.id 
--       AND ai.response_mode = 'Stream' 
--       AND ai.deleted = 0
--     ) THEN 'txt2video'
--     WHEN EXISTS (
--       SELECT 1 FROM api_interfaces ai 
--       WHERE ai.platform_id = ap.id 
--       AND ai.deleted = 0
--     ) THEN 'txt2img'
--     ELSE 'txt2img'
--   END
-- )
-- WHERE ap.type IS NULL AND ap.deleted = 0;

