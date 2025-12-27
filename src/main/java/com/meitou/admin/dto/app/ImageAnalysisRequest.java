package com.meitou.admin.dto.app;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 图片分析请求 DTO
 */
@Data
public class ImageAnalysisRequest {
    
    /**
     * 图片URL或base64（必填）
     */
    @NotBlank(message = "图片不能为空")
    private String image;
    
    /**
     * 分析方向（可选）
     */
    private String direction;
    
    /**
     * 模型名称（可选）
     */
    private String model;
}

