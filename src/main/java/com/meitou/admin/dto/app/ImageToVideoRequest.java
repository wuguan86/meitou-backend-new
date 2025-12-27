package com.meitou.admin.dto.app;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

/**
 * 图生视频请求 DTO
 */
@Data
public class ImageToVideoRequest {
    
    /**
     * 提示词（必填）
     */
    @NotBlank(message = "提示词不能为空")
    private String prompt;
    
    /**
     * 参考图片URL或base64（必填）
     */
    @NotBlank(message = "参考图片不能为空")
    private String image;
    
    /**
     * 模型名称（可选）
     */
    private String model;
    
    /**
     * 视频宽高比（可选，如：16:9, 9:16, 1:1等）
     */
    private String aspectRatio;
    
    /**
     * 视频时长（可选，单位：秒，默认5秒）
     */
    @Min(value = 1, message = "视频时长最少为1秒")
    @Max(value = 60, message = "视频时长最多为60秒")
    private Integer duration = 5;
    
    /**
     * 分辨率（可选，如：1K, 2K, 4K）
     */
    private String resolution;
}

