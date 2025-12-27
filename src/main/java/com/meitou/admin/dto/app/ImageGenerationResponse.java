package com.meitou.admin.dto.app;

import lombok.Data;
import java.util.List;

/**
 * 图片生成响应 DTO
 */
@Data
public class ImageGenerationResponse {
    
    /**
     * 生成的图片URL列表
     */
    private List<String> imageUrls;
    
    /**
     * 任务ID（如果API是异步的）
     */
    private String taskId;
    
    /**
     * 生成状态：success-成功，processing-处理中，failed-失败
     */
    private String status;
    
    /**
     * 错误消息（如果失败）
     */
    private String errorMessage;
}
