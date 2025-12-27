package com.meitou.admin.dto.app;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

/**
 * 图生图请求 DTO
 */
@Data
public class ImageToImageRequest {
    
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
     * 图片宽高比（可选，如：16:9, 9:16, 1:1等）
     */
    private String aspectRatio;
    
    /**
     * 分辨率（可选，如：1K, 2K, 4K）
     */
    private String resolution;
    
    /**
     * 生成数量（可选，默认1，范围1-4）
     */
    @Min(value = 1, message = "生成数量最少为1")
    @Max(value = 4, message = "生成数量最多为4")
    private Integer quantity = 1;
    
    /**
     * 参考图片模式（可选：single-单图，frames-首尾帧，multi-多图）
     */
    private String mode = "single";
    
    /**
     * 第二张参考图片（用于frames或multi模式）
     */
    private String image2;
    
    /**
     * 第三张参考图片（用于multi模式）
     */
    private String image3;
}
