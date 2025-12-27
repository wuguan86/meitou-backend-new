package com.meitou.admin.controller.app;

import com.meitou.admin.common.Result;
import com.meitou.admin.dto.app.ImageGenerationResponse;
import com.meitou.admin.dto.app.ImageToImageRequest;
import com.meitou.admin.dto.app.PlatformModelResponse;
import com.meitou.admin.dto.app.TextToImageRequest;
import com.meitou.admin.dto.app.TextToVideoRequest;
import com.meitou.admin.dto.app.ImageToVideoRequest;
import com.meitou.admin.dto.app.VideoGenerationResponse;
import com.meitou.admin.entity.ApiPlatform;
import com.meitou.admin.service.admin.ApiPlatformService;
import com.meitou.admin.service.app.GenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户端图片生成控制器
 * 处理文生图和图生图接口
 */
@RestController
@RequestMapping("/api/app/generation")
@RequiredArgsConstructor
public class GenerationController {
    
    private final GenerationService generationService;
    private final ApiPlatformService apiPlatformService;
    
    /**
     * 文生图接口
     * 
     * @param request 文生图请求
     * @param token 用户Token（从请求头获取）
     * @return 生成响应
     */
    @PostMapping("/text-to-image")
    public Result<ImageGenerationResponse> textToImage(
            @Valid @RequestBody TextToImageRequest request,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        try {
            // 从token获取用户ID
            Long userId = getUserIdFromToken(token);
            if (userId == null) {
                return Result.error(401, "未登录或token无效");
            }
            
            // 调用服务生成图片
            ImageGenerationResponse response = generationService.generateTextToImage(request, userId);
            return Result.success("生成成功", response);
            
        } catch (Exception e) {
            return Result.error("生成失败：" + e.getMessage());
        }
    }
    
    /**
     * 图生图接口
     * 
     * @param request 图生图请求
     * @param token 用户Token（从请求头获取）
     * @return 生成响应
     */
    @PostMapping("/image-to-image")
    public Result<ImageGenerationResponse> imageToImage(
            @Valid @RequestBody ImageToImageRequest request,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        try {
            // 从token获取用户ID
            Long userId = getUserIdFromToken(token);
            if (userId == null) {
                return Result.error(401, "未登录或token无效");
            }
            
            // 调用服务生成图片
            ImageGenerationResponse response = generationService.generateImageToImage(request, userId);
            return Result.success("生成成功", response);
            
        } catch (Exception e) {
            return Result.error("生成失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取文生图平台的模型列表
     * 根据type字段查找类型为txt2img的平台
     * 
     * @return 平台模型列表
     */
    @GetMapping("/text-to-image/models")
    public Result<List<PlatformModelResponse>> getTextToImageModels() {
        try {
            // 根据类型获取所有启用的文生图平台（type=txt2img）
            List<ApiPlatform> platforms = apiPlatformService.getPlatformsByTypeWithDecryptedKey("txt2img", null);
            
            List<PlatformModelResponse> responses = new ArrayList<>();
            for (ApiPlatform platform : platforms) {
                // 检查是否有支持的模型
                if (platform.getSupportedModels() != null && !platform.getSupportedModels().trim().isEmpty()) {
                    PlatformModelResponse response = new PlatformModelResponse();
                    response.setPlatformId(platform.getId());
                    response.setPlatformName(platform.getName());
                    
                    // 解析支持的模型（以#号分割）
                    String[] modelIds = platform.getSupportedModels().split("#");
                    List<PlatformModelResponse.ModelInfo> models = Arrays.stream(modelIds)
                            .filter(id -> id != null && !id.trim().isEmpty())
                            .map(id -> {
                                PlatformModelResponse.ModelInfo modelInfo = new PlatformModelResponse.ModelInfo();
                                modelInfo.setId(id.trim());
                                // 默认使用模型ID作为显示名称，可以后续扩展
                                modelInfo.setName(id.trim());
                                return modelInfo;
                            })
                            .collect(Collectors.toList());
                    
                    response.setModels(models);
                    responses.add(response);
                }
            }
            
            return Result.success(responses);
        } catch (Exception e) {
            return Result.error("获取模型列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取图生图平台的模型列表
     * 根据type字段查找类型为img2img的平台
     * 
     * @return 平台模型列表
     */
    @GetMapping("/image-to-image/models")
    public Result<List<PlatformModelResponse>> getImageToImageModels() {
        try {
            // 根据类型获取所有启用的图生图平台（type=img2img）
            List<ApiPlatform> platforms = apiPlatformService.getPlatformsByTypeWithDecryptedKey("img2img", null);
            
            List<PlatformModelResponse> responses = new ArrayList<>();
            for (ApiPlatform platform : platforms) {
                // 检查是否有支持的模型
                if (platform.getSupportedModels() != null && !platform.getSupportedModels().trim().isEmpty()) {
                    PlatformModelResponse response = new PlatformModelResponse();
                    response.setPlatformId(platform.getId());
                    response.setPlatformName(platform.getName());
                    
                    // 解析支持的模型（以#号分割）
                    String[] modelIds = platform.getSupportedModels().split("#");
                    List<PlatformModelResponse.ModelInfo> models = Arrays.stream(modelIds)
                            .filter(id -> id != null && !id.trim().isEmpty())
                            .map(id -> {
                                PlatformModelResponse.ModelInfo modelInfo = new PlatformModelResponse.ModelInfo();
                                modelInfo.setId(id.trim());
                                // 默认使用模型ID作为显示名称，可以后续扩展
                                modelInfo.setName(id.trim());
                                return modelInfo;
                            })
                            .collect(Collectors.toList());
                    
                    response.setModels(models);
                    responses.add(response);
                }
            }
            
            return Result.success(responses);
        } catch (Exception e) {
            return Result.error("获取模型列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 文生视频接口
     * 
     * @param request 文生视频请求
     * @param token 用户Token（从请求头获取）
     * @return 生成响应
     */
    @PostMapping("/text-to-video")
    public Result<VideoGenerationResponse> textToVideo(
            @Valid @RequestBody TextToVideoRequest request,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        try {
            // 从token获取用户ID
            Long userId = getUserIdFromToken(token);
            if (userId == null) {
                return Result.error(401, "未登录或token无效");
            }
            
            // 调用服务生成视频
            VideoGenerationResponse response = generationService.generateTextToVideo(request, userId);
            return Result.success("生成成功", response);
            
        } catch (Exception e) {
            return Result.error("生成失败：" + e.getMessage());
        }
    }
    
    /**
     * 图生视频接口
     * 
     * @param request 图生视频请求
     * @param token 用户Token（从请求头获取）
     * @return 生成响应
     */
    @PostMapping("/image-to-video")
    public Result<VideoGenerationResponse> imageToVideo(
            @Valid @RequestBody ImageToVideoRequest request,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        try {
            // 从token获取用户ID
            Long userId = getUserIdFromToken(token);
            if (userId == null) {
                return Result.error(401, "未登录或token无效");
            }
            
            // 调用服务生成视频
            VideoGenerationResponse response = generationService.generateImageToVideo(request, userId);
            return Result.success("生成成功", response);
            
        } catch (Exception e) {
            return Result.error("生成失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取文生视频平台的模型列表
     * 根据type字段查找类型为txt2video的平台
     * 
     * @return 平台模型列表
     */
    @GetMapping("/text-to-video/models")
    public Result<List<PlatformModelResponse>> getTextToVideoModels() {
        try {
            // 根据类型获取所有启用的文生视频平台（type=txt2video）
            List<ApiPlatform> platforms = apiPlatformService.getPlatformsByTypeWithDecryptedKey("txt2video", null);
            
            List<PlatformModelResponse> responses = new ArrayList<>();
            for (ApiPlatform platform : platforms) {
                // 检查是否有支持的模型
                if (platform.getSupportedModels() != null && !platform.getSupportedModels().trim().isEmpty()) {
                    PlatformModelResponse response = new PlatformModelResponse();
                    response.setPlatformId(platform.getId());
                    response.setPlatformName(platform.getName());
                    
                    // 解析支持的模型（以#号分割）
                    String[] modelIds = platform.getSupportedModels().split("#");
                    List<PlatformModelResponse.ModelInfo> models = Arrays.stream(modelIds)
                            .filter(id -> id != null && !id.trim().isEmpty())
                            .map(id -> {
                                PlatformModelResponse.ModelInfo modelInfo = new PlatformModelResponse.ModelInfo();
                                modelInfo.setId(id.trim());
                                modelInfo.setName(id.trim());
                                return modelInfo;
                            })
                            .collect(Collectors.toList());
                    
                    response.setModels(models);
                    responses.add(response);
                }
            }
            
            return Result.success(responses);
        } catch (Exception e) {
            return Result.error("获取模型列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取图生视频平台的模型列表
     * 根据type字段查找类型为img2video的平台
     * 
     * @return 平台模型列表
     */
    @GetMapping("/image-to-video/models")
    public Result<List<PlatformModelResponse>> getImageToVideoModels() {
        try {
            // 根据类型获取所有启用的图生视频平台（type=img2video）
            List<ApiPlatform> platforms = apiPlatformService.getPlatformsByTypeWithDecryptedKey("img2video", null);
            
            List<PlatformModelResponse> responses = new ArrayList<>();
            for (ApiPlatform platform : platforms) {
                // 检查是否有支持的模型
                if (platform.getSupportedModels() != null && !platform.getSupportedModels().trim().isEmpty()) {
                    PlatformModelResponse response = new PlatformModelResponse();
                    response.setPlatformId(platform.getId());
                    response.setPlatformName(platform.getName());
                    
                    // 解析支持的模型（以#号分割）
                    String[] modelIds = platform.getSupportedModels().split("#");
                    List<PlatformModelResponse.ModelInfo> models = Arrays.stream(modelIds)
                            .filter(id -> id != null && !id.trim().isEmpty())
                            .map(id -> {
                                PlatformModelResponse.ModelInfo modelInfo = new PlatformModelResponse.ModelInfo();
                                modelInfo.setId(id.trim());
                                modelInfo.setName(id.trim());
                                return modelInfo;
                            })
                            .collect(Collectors.toList());
                    
                    response.setModels(models);
                    responses.add(response);
                }
            }
            
            return Result.success(responses);
        } catch (Exception e) {
            return Result.error("获取模型列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 从token中解析用户ID
     * token格式：app_token_{userId}_{timestamp} 或 Bearer app_token_{userId}_{timestamp}
     * 
     * @param token 用户Token
     * @return 用户ID，如果token无效则返回null
     */
    private Long getUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        // 移除Bearer前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // 解析token格式：app_token_{userId}_{timestamp}
        if (token.startsWith("app_token_")) {
            String[] parts = token.substring(10).split("_");
            if (parts.length > 0) {
                try {
                    return Long.parseLong(parts[0]);
                } catch (NumberFormatException e) {
                    // token格式错误
                    return null;
                }
            }
        }
        
        return null;
    }
}
