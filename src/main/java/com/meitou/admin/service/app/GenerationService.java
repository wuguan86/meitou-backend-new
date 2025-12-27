package com.meitou.admin.service.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meitou.admin.dto.app.ImageGenerationResponse;
import com.meitou.admin.dto.app.ImageToImageRequest;
import com.meitou.admin.dto.app.TextToImageRequest;
import com.meitou.admin.dto.app.TextToVideoRequest;
import com.meitou.admin.dto.app.ImageToVideoRequest;
import com.meitou.admin.dto.app.VideoGenerationResponse;
import com.meitou.admin.entity.ApiInterface;
import com.meitou.admin.entity.ApiPlatform;
import com.meitou.admin.common.SiteContext;
import com.meitou.admin.entity.GenerationRecord;
import com.meitou.admin.entity.User;
import com.meitou.admin.mapper.GenerationRecordMapper;
import com.meitou.admin.mapper.UserMapper;
import com.meitou.admin.service.admin.ApiPlatformService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 图片生成服务类
 * 处理文生图和图生图的业务逻辑，调用GRSAI API
 */
@Slf4j
@Service
public class GenerationService {
    
    private final ApiPlatformService apiPlatformService;
    private final GenerationRecordMapper generationRecordMapper;
    private final UserMapper userMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 构造函数，初始化RestTemplate并配置超时时间
     */
    public GenerationService(ApiPlatformService apiPlatformService, 
                             GenerationRecordMapper generationRecordMapper,
                             UserMapper userMapper) {
        this.apiPlatformService = apiPlatformService;
        this.generationRecordMapper = generationRecordMapper;
        this.userMapper = userMapper;
        
        // 配置RestTemplate的超时时间
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 连接超时：10秒
        factory.setReadTimeout(120000); // 读取超时：120秒（流式响应可能需要较长时间，图片生成通常需要30-60秒）
        this.restTemplate = new RestTemplate(factory);
    }
    
    /**
     * 文生图
     * 
     * @param request 文生图请求
     * @param userId 用户ID
     * @return 生成响应
     */
    @Transactional
    public ImageGenerationResponse generateTextToImage(TextToImageRequest request, Long userId) {
        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 根据类型查找对应的API平台（type=txt2img）
        ApiPlatform platform = findPlatformByType("txt2img", null);
        if (platform == null) {
            throw new RuntimeException("文生图平台未配置，请在后台API接口管理中配置类型为'文生图'的平台");
        }
        
        // 检查apiKey是否有效（解密后不为null）
        if (platform.getApiKey() == null || platform.getApiKey().isEmpty()) {
            throw new RuntimeException("API密钥未配置或解密失败，请重新设置API密钥");
        }
        
        // 获取文生图接口配置
        ApiInterface txt2imgInterface = findTextToImageInterface(platform.getId());
        if (txt2imgInterface == null) {
            throw new RuntimeException("文生图接口未配置");
        }
        
        try {
            // 构建请求参数
            Map<String, Object> apiRequest = buildTextToImageRequest(request, platform);
            
            // 调用API
            String responseJson = callApi(txt2imgInterface, platform, apiRequest);
            
            // 解析响应（传递responseMode以支持不同格式）
            List<String> imageUrls = parseImageUrls(responseJson, txt2imgInterface.getResponseMode());
            
            // 保存生成记录
            GenerationRecord record = new GenerationRecord();
            record.setUserId(userId);
            record.setUsername(user.getUsername());
            record.setType("txt2img");
            record.setModel(request.getModel());
            record.setPrompt(request.getPrompt());
            record.setStatus("success");
            record.setSiteId(SiteContext.getSiteId());
            record.setContentUrl(String.join(",", imageUrls));
            record.setCost(calculateCost(request.getQuantity(), "txt2img"));
            generationRecordMapper.insert(record);
            
            // 构建响应
            ImageGenerationResponse response = new ImageGenerationResponse();
            response.setImageUrls(imageUrls);
            response.setStatus("success");
            
            return response;
            
        } catch (Exception e) {
            log.error("文生图失败：{}", e.getMessage(), e);
            
            // 保存失败记录
            try {
                GenerationRecord record = new GenerationRecord();
                record.setUserId(userId);
                record.setUsername(user.getUsername());
                record.setType("txt2img");
                record.setModel(request.getModel());
                record.setPrompt(request.getPrompt());
                record.setStatus("failed");
                record.setSiteId(SiteContext.getSiteId());
                generationRecordMapper.insert(record);
            } catch (Exception ex) {
                log.error("保存失败记录时出错：{}", ex.getMessage());
            }
            
            throw new RuntimeException("文生图失败：" + e.getMessage());
        }
    }
    
    /**
     * 图生图
     * 
     * @param request 图生图请求
     * @param userId 用户ID
     * @return 生成响应
     */
    @Transactional
    public ImageGenerationResponse generateImageToImage(ImageToImageRequest request, Long userId) {
        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 根据类型查找对应的API平台（type=img2img）
        ApiPlatform platform = findPlatformByType("img2img", null);
        if (platform == null) {
            throw new RuntimeException("图生图平台未配置，请在后台API接口管理中配置类型为'图生图'的平台");
        }
        
        // 检查apiKey是否有效（解密后不为null）
        if (platform.getApiKey() == null || platform.getApiKey().isEmpty()) {
            throw new RuntimeException("API密钥未配置或解密失败，请重新设置API密钥");
        }
        
        // 获取图生图接口配置
        ApiInterface img2imgInterface = findImageToImageInterface(platform.getId());
        if (img2imgInterface == null) {
            throw new RuntimeException("图生图接口未配置");
        }
        
        try {
            // 构建请求参数
            Map<String, Object> apiRequest = buildImageToImageRequest(request, platform);
            
            // 应用参数映射（如果接口配置了参数映射）
            apiRequest = applyParameterMapping(apiRequest, img2imgInterface);
            
            // 调用API
            String responseJson = callApi(img2imgInterface, platform, apiRequest);
            
            // 解析响应（传递responseMode以支持不同格式）
            List<String> imageUrls = parseImageUrls(responseJson, img2imgInterface.getResponseMode());
            
            // 保存生成记录
            GenerationRecord record = new GenerationRecord();
            record.setUserId(userId);
            record.setUsername(user.getUsername());
            record.setType("img2img");
            record.setModel(request.getModel());
            record.setPrompt(request.getPrompt());
            record.setStatus("success");
            record.setSiteId(SiteContext.getSiteId());
            record.setContentUrl(String.join(",", imageUrls));
            record.setCost(calculateCost(request.getQuantity(), "img2img"));
            generationRecordMapper.insert(record);
            
            // 构建响应
            ImageGenerationResponse response = new ImageGenerationResponse();
            response.setImageUrls(imageUrls);
            response.setStatus("success");
            
            return response;
            
        } catch (Exception e) {
            log.error("图生图失败：{}", e.getMessage(), e);
            
            // 保存失败记录
            try {
                GenerationRecord record = new GenerationRecord();
                record.setUserId(userId);
                record.setUsername(user.getUsername());
                record.setType("img2img");
                record.setModel(request.getModel());
                record.setPrompt(request.getPrompt());
                record.setStatus("failed");
                record.setSiteId(SiteContext.getSiteId());
                generationRecordMapper.insert(record);
            } catch (Exception ex) {
                log.error("保存失败记录时出错：{}", ex.getMessage());
            }
            
            throw new RuntimeException("图生图失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据类型查找API平台（apiKey已解密）
     * 
     * @param type API类型：image_analysis, video_analysis, txt2img, img2img, txt2video, img2video, voice_clone
     * @return 找到的第一个平台，如果没有则返回null
     */
    private ApiPlatform findPlatformByType(String type, Long siteId) {
        // 使用根据类型查询的方法，确保apiKey已解密且平台已启用
        List<ApiPlatform> platforms = apiPlatformService.getPlatformsByTypeWithDecryptedKey(type, siteId);
        // 返回第一个平台（如果有多个平台，可以后续扩展为负载均衡或优先级选择）
        return platforms.isEmpty() ? null : platforms.get(0);
    }
    
    /**
     * 查找文生图接口
     */
    private ApiInterface findTextToImageInterface(Long platformId) {
        List<ApiInterface> interfaces = apiPlatformService.getInterfacesByPlatformId(platformId);
        // 查找responseMode为JSON或Stream的接口（非Result类型）
        // 注意：Stream类型也支持，因为API可能返回SSE格式的流式数据
        return interfaces.stream()
                .filter(i -> i.getResponseMode() != null && !"Result".equals(i.getResponseMode()))
                .findFirst()
                .orElse(interfaces.isEmpty() ? null : interfaces.get(0)); // 如果没有找到，返回第一个
    }
    
    /**
     * 查找图生图接口
     */
    private ApiInterface findImageToImageInterface(Long platformId) {
        List<ApiInterface> interfaces = apiPlatformService.getInterfacesByPlatformId(platformId);
        // 查找responseMode为JSON的接口
        return interfaces.stream()
                .filter(i -> i.getResponseMode() != null && "JSON".equals(i.getResponseMode()))
                .findFirst()
                .orElse(interfaces.isEmpty() ? null : interfaces.get(0));
    }
    
    /**
     * 构建文生图请求参数
     */
    private Map<String, Object> buildTextToImageRequest(TextToImageRequest request, ApiPlatform platform) {
        Map<String, Object> params = new HashMap<>();
        
        // 基础参数
        params.put("prompt", request.getPrompt());
        
        // 处理宽高比
        if (request.getAspectRatio() != null && !request.getAspectRatio().isEmpty() && !"Auto".equals(request.getAspectRatio())) {
            params.put("aspect_ratio", request.getAspectRatio());
        }
        
        // 处理分辨率（转换为宽高）
        if (request.getResolution() != null && !request.getResolution().isEmpty()) {
            Map<String, Integer> size = parseResolution(request.getResolution(), request.getAspectRatio());
            params.put("width", size.get("width"));
            params.put("height", size.get("height"));
        }
        
        // 数量
        if (request.getQuantity() != null && request.getQuantity() > 1) {
            params.put("n", request.getQuantity());
        }
        
        // 模型（如果有）
        if (request.getModel() != null && !request.getModel().isEmpty()) {
            params.put("model", request.getModel());
        }
        
        return params;
    }
    
    /**
     * 构建图生图请求参数
     */
    private Map<String, Object> buildImageToImageRequest(ImageToImageRequest request, ApiPlatform platform) {
        Map<String, Object> params = new HashMap<>();
        
        // 基础参数
        params.put("prompt", request.getPrompt());
        
        // 参考图片URL数组（根据API文档，参数名是urls，支持多张图片）
        List<String> imageUrls = new ArrayList<>();
        
        // 添加第一张参考图片（必填）
        if (request.getImage() != null && !request.getImage().trim().isEmpty()) {
            imageUrls.add(request.getImage());
            log.debug("图生图参考图片1已设置: {}", request.getImage().length() > 100 
                ? request.getImage().substring(0, 100) + "..." 
                : request.getImage());
        } else {
            log.warn("图生图参考图片参数为空！");
            throw new RuntimeException("参考图片不能为空");
        }
        
        // 处理多图模式：根据模式添加额外的参考图片
        if ("frames".equals(request.getMode()) && request.getImage2() != null) {
            // 首尾帧模式：添加第二张图片
            imageUrls.add(request.getImage2());
            log.debug("图生图参考图片2（尾帧）已设置");
        } else if ("multi".equals(request.getMode())) {
            // 多图参考模式：添加所有图片
            if (request.getImage2() != null) {
                imageUrls.add(request.getImage2());
            }
            if (request.getImage3() != null) {
                imageUrls.add(request.getImage3());
            }
            log.debug("图生图多图参考模式，共{}张图片", imageUrls.size());
        }
        
        // 设置urls参数（API文档要求）
        params.put("urls", imageUrls);
        
        // 处理宽高比：映射为size参数（API文档要求）
        if (request.getAspectRatio() != null && !request.getAspectRatio().isEmpty() && !"Auto".equals(request.getAspectRatio())) {
            // 将宽高比转换为API要求的格式：1:1, 3:2, 2:3, auto
            String size = request.getAspectRatio();
            // 如果已经是API要求的格式，直接使用；否则需要转换
            if (!size.equals("auto") && !size.equals("1:1") && !size.equals("3:2") && !size.equals("2:3")) {
                // 尝试转换常见格式
                if (size.equals("16:9") || size.equals("9:16") || size.equals("4:3") || size.equals("3:4")) {
                    // 这些格式API不支持，使用auto
                    size = "auto";
                } else {
                    size = "auto";
                }
            }
            params.put("size", size);
        } else {
            // 默认使用auto
            params.put("size", "auto");
        }
        
        // 处理数量：映射为variants参数（API文档要求，1或2）
        if (request.getQuantity() != null && request.getQuantity() >= 1) {
            int variants = Math.min(request.getQuantity(), 2); // API最多支持2
            params.put("variants", variants);
        } else {
            params.put("variants", 1); // 默认1
        }
        
        // 模型（必填）
        if (request.getModel() != null && !request.getModel().isEmpty()) {
            params.put("model", request.getModel());
        } else {
            // 如果没有指定模型，使用默认模型
            params.put("model", "sora-image");
        }
        
        // 关闭进度回复，直接返回最终结果（建议设置）
        params.put("shutProgress", false);
        
        return params;
    }
    
    /**
     * 解析分辨率
     */
    private Map<String, Integer> parseResolution(String resolution, String aspectRatio) {
        Map<String, Integer> size = new HashMap<>();
        
        // 根据分辨率设置基础尺寸
        int baseSize = 1024; // 默认1K
        if ("2K".equals(resolution)) {
            baseSize = 2048;
        } else if ("4K".equals(resolution)) {
            baseSize = 4096;
        }
        
        // 根据宽高比调整
        if (aspectRatio != null && !aspectRatio.isEmpty() && !"Auto".equals(aspectRatio)) {
            String[] parts = aspectRatio.split(":");
            if (parts.length == 2) {
                try {
                    double ratio = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
                    if (ratio > 1) {
                        // 横向
                        size.put("width", baseSize);
                        size.put("height", (int)(baseSize / ratio));
                    } else {
                        // 纵向
                        size.put("width", (int)(baseSize * ratio));
                        size.put("height", baseSize);
                    }
                } catch (NumberFormatException e) {
                    size.put("width", baseSize);
                    size.put("height", baseSize);
                }
            } else {
                size.put("width", baseSize);
                size.put("height", baseSize);
            }
        } else {
            size.put("width", baseSize);
            size.put("height", baseSize);
        }
        
        return size;
    }
    
    /**
     * 应用参数映射（根据接口配置的parametersJson映射参数名）
     * 如果接口配置了参数映射，则将内部参数名映射为API接口期望的参数名
     * 
     * @param params 原始参数Map
     * @param apiInterface API接口配置
     * @return 映射后的参数Map
     */
    private Map<String, Object> applyParameterMapping(Map<String, Object> params, ApiInterface apiInterface) {
        // 如果接口没有配置参数映射，直接返回原参数
        if (apiInterface.getParametersJson() == null || apiInterface.getParametersJson().trim().isEmpty()) {
            return params;
        }
        
        try {
            // 解析参数映射配置（JSON格式：{"image": "reference_image", "prompt": "text"}）
            JsonNode mappingNode = objectMapper.readTree(apiInterface.getParametersJson());
            Map<String, Object> mappedParams = new HashMap<>();
            
            // 遍历原始参数，应用映射
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String internalKey = entry.getKey();
                Object value = entry.getValue();
                
                // 如果配置中有映射，使用映射后的键名；否则使用原键名
                if (mappingNode.has(internalKey)) {
                    String mappedKey = mappingNode.get(internalKey).asText();
                    mappedParams.put(mappedKey, value);
                    log.debug("参数映射: {} -> {}", internalKey, mappedKey);
                } else {
                    // 没有映射配置，保持原键名
                    mappedParams.put(internalKey, value);
                }
            }
            
            return mappedParams;
        } catch (Exception e) {
            log.warn("解析参数映射失败，使用原始参数：{}", e.getMessage());
            return params; // 如果解析失败，返回原始参数
        }
    }
    
    /**
     * 调用API平台接口
     */
    private String callApi(ApiInterface apiInterface, ApiPlatform platform, Map<String, Object> requestParams) {
        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 添加Authorization头（从接口配置的headers中解析）
            if (apiInterface.getHeaders() != null && !apiInterface.getHeaders().isEmpty()) {
                try {
                    JsonNode headersNode = objectMapper.readTree(apiInterface.getHeaders());
                    headersNode.fields().forEachRemaining(entry -> {
                        String key = entry.getKey();
                        String value = entry.getValue().asText();
                        // 替换{apiKey}占位符
                        if (value.contains("{apiKey}") && platform.getApiKey() != null) {
                            value = value.replace("{apiKey}", platform.getApiKey());
                        }
                        headers.set(key, value);
                    });
                } catch (Exception e) {
                    log.warn("解析headers失败，使用默认配置：{}", e.getMessage());
                }
            }
            
            // 如果有API Key，默认添加Authorization头
            if (platform.getApiKey() != null && !platform.getApiKey().isEmpty()) {
                // 记录API密钥的前几位用于调试（不记录完整密钥）
                String apiKeyPreview = platform.getApiKey().length() > 10 
                    ? platform.getApiKey().substring(0, 10) + "***" 
                    : "***";
                log.debug("使用API Key: {} (长度: {})", apiKeyPreview, platform.getApiKey().length());
                headers.set("Authorization", "Bearer " + platform.getApiKey());
            } else if (platform.getApiKey() == null || platform.getApiKey().isEmpty()) {
                log.warn("API密钥为空，平台: {}", platform.getName());
            }
            
            // 构建请求体
            String requestBody = objectMapper.writeValueAsString(requestParams);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            log.info("调用API平台接口: {} {} (平台: {})", apiInterface.getMethod(), apiInterface.getUrl(), platform.getName());
            log.debug("请求参数: {}", requestBody);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    apiInterface.getUrl(),
                    HttpMethod.valueOf(apiInterface.getMethod()),
                    entity,
                    String.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("API调用失败，状态码：" + response.getStatusCode());
            }
            
            log.debug("API响应: {}", response.getBody());
            return response.getBody();
            
        } catch (Exception e) {
            log.error("调用API平台接口失败：{}", e.getMessage(), e);
            throw new RuntimeException("调用API平台接口失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 解析图片URL列表
     * 
     * @param responseJson 响应JSON字符串
     * @param responseMode 响应模式：JSON、Stream、Result
     */
    private List<String> parseImageUrls(String responseJson, String responseMode) {
        List<String> imageUrls = new ArrayList<>();
        
        try {
            // 自动检测响应格式：如果响应以 "data: " 开头，则视为SSE格式
            boolean isSseFormat = responseJson != null && responseJson.trim().startsWith("data:");
            
            // 如果是Stream模式或自动检测到SSE格式，解析SSE格式的流式数据
            if ("Stream".equals(responseMode) || isSseFormat) {
                return parseSseResponse(responseJson);
            }
            
            // 普通JSON格式
            JsonNode root = objectMapper.readTree(responseJson);
            
            // 首先检查响应是否包含错误信息
            // 格式1: { "code": -1, "msg": "错误消息" }
            if (root.has("code")) {
                int code = root.get("code").asInt();
                if (code != 0 && code != 200) {
                    // 非成功状态码，提取错误消息
                    String errorMsg = "API调用失败";
                    if (root.has("msg")) {
                        errorMsg = root.get("msg").asText();
                    } else if (root.has("message")) {
                        errorMsg = root.get("message").asText();
                    } else if (root.has("error")) {
                        errorMsg = root.get("error").asText();
                    }
                    
                    // 针对常见错误提供更友好的提示
                    String friendlyMsg = errorMsg;
                    if (errorMsg != null && errorMsg.toLowerCase().contains("apikey")) {
                        friendlyMsg = "API密钥错误，请检查后台API接口管理中的API密钥配置是否正确";
                    } else if (errorMsg != null && errorMsg.toLowerCase().contains("unauthorized")) {
                        friendlyMsg = "API认证失败，请检查API密钥是否有效";
                    } else if (errorMsg != null && errorMsg.toLowerCase().contains("forbidden")) {
                        friendlyMsg = "API访问被拒绝，请检查API密钥权限";
                    }
                    
                    log.error("API返回错误响应: code={}, msg={}", code, errorMsg);
                    throw new RuntimeException(friendlyMsg);
                }
            }
            // 格式2: { "error": "错误消息" }
            else if (root.has("error")) {
                String errorMsg = root.get("error").asText();
                throw new RuntimeException("API返回错误：" + errorMsg);
            }
            // 格式3: { "status": "error", "message": "错误消息" }
            else if (root.has("status")) {
                String status = root.get("status").asText();
                if ("error".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status)) {
                    String errorMsg = "API调用失败";
                    if (root.has("message")) {
                        errorMsg = root.get("message").asText();
                    } else if (root.has("msg")) {
                        errorMsg = root.get("msg").asText();
                    }
                    throw new RuntimeException("API返回错误：" + errorMsg);
                }
            }
            
            // 尝试多种常见的响应格式
            // 格式1: { "results": [{ "url": "url1", "width": 1024, "height": 1024 }, ...] } (新API格式)
            if (root.has("results") && root.get("results").isArray()) {
                for (JsonNode result : root.get("results")) {
                    if (result.has("url")) {
                        imageUrls.add(result.get("url").asText());
                    }
                }
            }
            // 格式2: { "url": "url1" } (旧格式，兼容)
            else if (root.has("url") && !root.has("results")) {
                imageUrls.add(root.get("url").asText());
            }
            // 格式3: { "images": ["url1", "url2"] }
            else if (root.has("images") && root.get("images").isArray()) {
                for (JsonNode img : root.get("images")) {
                    if (img.isTextual()) {
                        imageUrls.add(img.asText());
                    } else if (img.has("url")) {
                        imageUrls.add(img.get("url").asText());
                    }
                }
            }
            // 格式4: { "data": [{ "url": "url1" }, { "url": "url2" }] }
            else if (root.has("data") && root.get("data").isArray()) {
                for (JsonNode item : root.get("data")) {
                    if (item.has("url")) {
                        imageUrls.add(item.get("url").asText());
                    }
                }
            }
            // 格式5: { "result": { "images": ["url1", "url2"] } }
            else if (root.has("result")) {
                JsonNode result = root.get("result");
                if (result.has("images") && result.get("images").isArray()) {
                    for (JsonNode img : result.get("images")) {
                        if (img.isTextual()) {
                            imageUrls.add(img.asText());
                        }
                    }
                }
            }
            // 格式6: 直接是base64数据 { "image": "data:image/..." }
            else if (root.has("image")) {
                imageUrls.add(root.get("image").asText());
            }
            
        } catch (RuntimeException e) {
            // 重新抛出运行时异常（包括我们检查到的错误）
            throw e;
        } catch (Exception e) {
            log.error("解析API响应失败：{}", e.getMessage(), e);
            throw new RuntimeException("解析API响应失败：" + e.getMessage());
        }
        
        if (imageUrls.isEmpty()) {
            throw new RuntimeException("未找到生成的图片URL，请检查API响应格式是否正确");
        }
        
        return imageUrls;
    }
    
    /**
     * 解析SSE格式的流式响应
     * SSE格式：每行以 "data: " 开头，后面跟着JSON对象
     * 
     * @param sseResponse SSE格式的响应字符串
     * @return 图片URL列表
     */
    private List<String> parseSseResponse(String sseResponse) {
        List<String> imageUrls = new ArrayList<>();
        JsonNode lastDataNode = null;
        String lastStatus = null;
        
        try {
            // 按行分割SSE数据
            String[] lines = sseResponse.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                // 跳过空行
                if (line.isEmpty()) {
                    continue;
                }
                
                // 检查是否是data行
                if (line.startsWith("data: ")) {
                    String jsonData = line.substring(6); // 移除 "data: " 前缀
                    
                    try {
                        // 解析JSON数据
                        JsonNode dataNode = objectMapper.readTree(jsonData);
                        lastDataNode = dataNode;
                        
                        // 检查状态字段
                        if (dataNode.has("status")) {
                            lastStatus = dataNode.get("status").asText();
                            
                            // 如果状态是completed、success或succeeded，尝试提取图片URL
                            if ("completed".equals(lastStatus) || "success".equals(lastStatus) || "succeeded".equals(lastStatus)) {
                                // 尝试从results字段提取图片URL
                                if (dataNode.has("results") && dataNode.get("results").isArray()) {
                                    for (JsonNode result : dataNode.get("results")) {
                                        if (result.has("url")) {
                                            imageUrls.add(result.get("url").asText());
                                        } else if (result.isTextual()) {
                                            imageUrls.add(result.asText());
                                        }
                                    }
                                }
                                // 或者直接从url字段提取
                                else if (dataNode.has("url") && !dataNode.get("url").asText().isEmpty()) {
                                    imageUrls.add(dataNode.get("url").asText());
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 忽略解析失败的行，继续处理下一行
                        log.debug("解析SSE数据行失败：{}", e.getMessage());
                    }
                }
            }
            
            // 如果没有找到图片URL，但最后一个状态是completed/success/succeeded，尝试从最后一个数据节点提取
            if (imageUrls.isEmpty() && lastDataNode != null && 
                ("completed".equals(lastStatus) || "success".equals(lastStatus) || "succeeded".equals(lastStatus))) {
                if (lastDataNode.has("results") && lastDataNode.get("results").isArray()) {
                    for (JsonNode result : lastDataNode.get("results")) {
                        if (result.has("url")) {
                            imageUrls.add(result.get("url").asText());
                        } else if (result.isTextual()) {
                            imageUrls.add(result.asText());
                        }
                    }
                } else if (lastDataNode.has("url") && !lastDataNode.get("url").asText().isEmpty()) {
                    imageUrls.add(lastDataNode.get("url").asText());
                }
            }
            
            // 如果还是没有找到，检查是否有错误信息
            if (imageUrls.isEmpty() && lastDataNode != null) {
                if (lastDataNode.has("error") && !lastDataNode.get("error").asText().isEmpty()) {
                    throw new RuntimeException("API返回错误：" + lastDataNode.get("error").asText());
                }
                if (lastDataNode.has("failure_reason") && !lastDataNode.get("failure_reason").asText().isEmpty()) {
                    throw new RuntimeException("API返回失败原因：" + lastDataNode.get("failure_reason").asText());
                }
                // 如果状态是running，尝试从当前数据中提取URL（有些API会在running状态时也返回部分结果）
                if ("running".equals(lastStatus) && lastDataNode != null) {
                    // 尝试从results字段提取（即使状态是running，也可能有部分结果）
                    if (lastDataNode.has("results") && lastDataNode.get("results").isArray()) {
                        for (JsonNode result : lastDataNode.get("results")) {
                            if (result.has("url") && !result.get("url").asText().isEmpty()) {
                                imageUrls.add(result.get("url").asText());
                            } else if (result.isTextual() && !result.asText().isEmpty()) {
                                imageUrls.add(result.asText());
                            }
                        }
                    }
                    // 如果还是没有找到URL，抛出异常
                    if (imageUrls.isEmpty()) {
                        throw new RuntimeException("图片生成任务仍在处理中（状态：running），请稍后查询结果。如需实时获取结果，建议使用轮询或Webhook方式");
                    }
                }
            }
            
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析SSE响应失败：{}", e.getMessage(), e);
            throw new RuntimeException("解析SSE响应失败：" + e.getMessage());
        }
        
        if (imageUrls.isEmpty()) {
            throw new RuntimeException("未找到生成的图片URL，响应状态：" + lastStatus);
        }
        
        return imageUrls;
    }
    
    /**
     * 文生视频
     * 
     * @param request 文生视频请求
     * @param userId 用户ID
     * @return 生成响应
     */
    @Transactional
    public VideoGenerationResponse generateTextToVideo(TextToVideoRequest request, Long userId) {
        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 根据类型查找对应的API平台（type=txt2video）
        ApiPlatform platform = findPlatformByType("txt2video", null);
        if (platform == null) {
            throw new RuntimeException("文生视频平台未配置，请在后台API接口管理中配置类型为'文生视频'的平台");
        }
        
        // 检查apiKey是否有效
        if (platform.getApiKey() == null || platform.getApiKey().isEmpty()) {
            throw new RuntimeException("API密钥未配置或解密失败，请重新设置API密钥");
        }
        
        // 获取文生视频接口配置
        ApiInterface txt2videoInterface = findTextToImageInterface(platform.getId()); // 复用查找接口的方法
        if (txt2videoInterface == null) {
            throw new RuntimeException("文生视频接口未配置");
        }
        
        try {
            // 构建请求参数
            Map<String, Object> apiRequest = buildTextToVideoRequest(request, platform);
            
            // 调用API
            String responseJson = callApi(txt2videoInterface, platform, apiRequest);
            
            // 解析响应（支持多种响应格式）
            VideoResponseInfo videoInfo = parseVideoResponse(responseJson, txt2videoInterface.getResponseMode());
            
            // 构建响应
            VideoGenerationResponse response = new VideoGenerationResponse();
            
            // 如果webHook为"-1"，会立即返回一个id用于轮询
            if ("-1".equals(request.getWebHook()) && videoInfo.getTaskId() != null) {
                // 返回任务ID，用于后续轮询
                response.setTaskId(videoInfo.getTaskId());
                response.setStatus("processing");
                
                // 保存处理中记录
                GenerationRecord record = new GenerationRecord();
                record.setUserId(userId);
                record.setUsername(user.getUsername());
                record.setType("txt2video");
                record.setModel(request.getModel());
                record.setPrompt(request.getPrompt());
                record.setStatus("processing");
                record.setSiteId(SiteContext.getSiteId());
                generationRecordMapper.insert(record);
                
                return response;
            }
            
            // 检查状态
            String status = videoInfo.getStatus();
            if ("running".equals(status)) {
                // 任务进行中，返回任务ID
                response.setTaskId(videoInfo.getTaskId());
                response.setStatus("processing");
                
                // 保存处理中记录
                GenerationRecord record = new GenerationRecord();
                record.setUserId(userId);
                record.setUsername(user.getUsername());
                record.setType("txt2video");
                record.setModel(request.getModel());
                record.setPrompt(request.getPrompt());
                record.setStatus("processing");
                record.setSiteId(SiteContext.getSiteId());
                generationRecordMapper.insert(record);
            } else if ("succeeded".equals(status) || "success".equals(status)) {
                // 任务成功，保存视频URL
                String videoUrl = videoInfo.getVideoUrl();
                if (videoUrl == null || videoUrl.isEmpty()) {
                    throw new RuntimeException("视频生成成功但未返回视频URL");
                }
                
                // 保存成功记录
                GenerationRecord record = new GenerationRecord();
                record.setUserId(userId);
                record.setUsername(user.getUsername());
                record.setType("txt2video");
                record.setModel(request.getModel());
                record.setPrompt(request.getPrompt());
                record.setStatus("success");
                record.setSiteId(SiteContext.getSiteId());
                record.setContentUrl(videoUrl);
                record.setCost(calculateCost(1, "txt2video"));
                generationRecordMapper.insert(record);
                
                response.setVideoUrl(videoUrl);
                response.setStatus("success");
            } else if ("failed".equals(status)) {
                // 任务失败
                String errorMsg = videoInfo.getErrorMessage();
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = videoInfo.getFailureReason();
                }
                
                // 检查是否是模型已下架的错误，如果是则提供更友好的提示
                String friendlyErrorMsg = errorMsg;
                if (errorMsg != null && (errorMsg.contains("下架") || errorMsg.contains("deprecated") || errorMsg.contains("veo3"))) {
                    // 如果错误信息包含模型下架相关的关键词，提供更友好的提示
                    if (errorMsg.contains("veo3") && !errorMsg.contains("veo3.1")) {
                        friendlyErrorMsg = "您选择的模型（" + request.getModel() + "）已被官方下架，请使用 veo3.1-fast 或 veo3.1-pro 模型。原错误信息：" + errorMsg;
                    }
                }
                
                // 保存失败记录
                GenerationRecord record = new GenerationRecord();
                record.setUserId(userId);
                record.setUsername(user.getUsername());
                record.setType("txt2video");
                record.setModel(request.getModel());
                record.setPrompt(request.getPrompt());
                record.setStatus("failed");
                record.setSiteId(SiteContext.getSiteId());
                generationRecordMapper.insert(record);
                
                // 抛出异常，使用特定的错误消息前缀以便在catch块中识别
                throw new RuntimeException("VIDEO_GENERATION_FAILED:" + (friendlyErrorMsg != null ? friendlyErrorMsg : "未知错误"));
            } else {
                throw new RuntimeException("未知的视频生成状态：" + status);
            }
            
            return response;
            
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            // 检查是否是业务异常（已经保存了失败记录的异常）
            // 通过检查异常消息是否以"VIDEO_GENERATION_FAILED:"开头来判断
            boolean alreadySaved = errorMessage != null && errorMessage.startsWith("VIDEO_GENERATION_FAILED:");
            
            if (alreadySaved) {
                // 业务异常已保存记录，只需记录日志，并提取原始错误消息
                String originalError = errorMessage.substring("VIDEO_GENERATION_FAILED:".length());
                log.error("文生视频失败：{}", originalError);
                throw new RuntimeException("视频生成失败：" + originalError);
            } else {
                // 其他异常（如API调用失败等），记录错误日志并保存失败记录
                log.error("文生视频失败：{}", errorMessage, e);
                
                // 保存失败记录
                try {
                    GenerationRecord record = new GenerationRecord();
                    record.setUserId(userId);
                    record.setUsername(user.getUsername());
                    record.setType("txt2video");
                    record.setModel(request.getModel());
                    record.setPrompt(request.getPrompt());
                    record.setStatus("failed");
                    record.setSiteId(SiteContext.getSiteId());
                    generationRecordMapper.insert(record);
                } catch (Exception ex) {
                    log.error("保存失败记录时出错：{}", ex.getMessage());
                }
                
                throw new RuntimeException("文生视频失败：" + errorMessage);
            }
        }
    }
    
    /**
     * 图生视频
     * 
     * @param request 图生视频请求
     * @param userId 用户ID
     * @return 生成响应
     */
    @Transactional
    public VideoGenerationResponse generateImageToVideo(ImageToVideoRequest request, Long userId) {
        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 根据类型查找对应的API平台（type=img2video）
        ApiPlatform platform = findPlatformByType("img2video", null);
        if (platform == null) {
            throw new RuntimeException("图生视频平台未配置，请在后台API接口管理中配置类型为'图生视频'的平台");
        }
        
        // 检查apiKey是否有效
        if (platform.getApiKey() == null || platform.getApiKey().isEmpty()) {
            throw new RuntimeException("API密钥未配置或解密失败，请重新设置API密钥");
        }
        
        // 获取图生视频接口配置
        ApiInterface img2videoInterface = findImageToImageInterface(platform.getId()); // 复用查找接口的方法
        if (img2videoInterface == null) {
            throw new RuntimeException("图生视频接口未配置");
        }
        
        try {
            // 构建请求参数
            Map<String, Object> apiRequest = buildImageToVideoRequest(request, platform);
            
            // 调用API
            String responseJson = callApi(img2videoInterface, platform, apiRequest);
            
            // 解析响应（使用新的解析方法，兼容多种响应格式）
            VideoResponseInfo videoInfo = parseVideoResponse(responseJson, img2videoInterface.getResponseMode());
            
            // 构建响应
            VideoGenerationResponse response = new VideoGenerationResponse();
            
            // 先检查状态，再检查URL
            String status = videoInfo.getStatus();
            if ("running".equals(status)) {
                // 任务进行中，返回任务ID
                response.setTaskId(videoInfo.getTaskId());
                response.setStatus("processing");
                
                // 保存处理中记录
                GenerationRecord record = new GenerationRecord();
                record.setUserId(userId);
                record.setUsername(user.getUsername());
                record.setType("img2video");
                record.setModel(request.getModel());
                record.setPrompt(request.getPrompt());
                record.setStatus("processing");
                record.setSiteId(SiteContext.getSiteId());
                generationRecordMapper.insert(record);
            } else if ("succeeded".equals(status) || "success".equals(status)) {
                // 任务成功，保存视频URL
                String videoUrl = videoInfo.getVideoUrl();
                if (videoUrl == null || videoUrl.isEmpty()) {
                    throw new RuntimeException("视频生成成功但未返回视频URL");
                }
                
                // 保存成功记录
                GenerationRecord record = new GenerationRecord();
                record.setUserId(userId);
                record.setUsername(user.getUsername());
                record.setType("img2video");
                record.setModel(request.getModel());
                record.setPrompt(request.getPrompt());
                record.setStatus("success");
                record.setSiteId(SiteContext.getSiteId());
                record.setContentUrl(videoUrl);
                record.setCost(calculateCost(1, "img2video"));
                generationRecordMapper.insert(record);
                
                response.setVideoUrl(videoUrl);
                response.setStatus("success");
            } else if ("failed".equals(status)) {
                // 任务失败
                String errorMsg = videoInfo.getErrorMessage();
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = videoInfo.getFailureReason();
                }
                
                // 检查是否是模型已下架的错误，如果是则提供更友好的提示
                String friendlyErrorMsg = errorMsg;
                if (errorMsg != null && (errorMsg.contains("下架") || errorMsg.contains("deprecated") || errorMsg.contains("veo3"))) {
                    // 如果错误信息包含模型下架相关的关键词，提供更友好的提示
                    if (errorMsg.contains("veo3") && !errorMsg.contains("veo3.1")) {
                        friendlyErrorMsg = "您选择的模型（" + request.getModel() + "）已被官方下架，请使用 veo3.1-fast 或 veo3.1-pro 模型。原错误信息：" + errorMsg;
                    }
                }
                
                // 保存失败记录
                GenerationRecord record = new GenerationRecord();
                record.setUserId(userId);
                record.setUsername(user.getUsername());
                record.setType("img2video");
                record.setModel(request.getModel());
                record.setPrompt(request.getPrompt());
                record.setStatus("failed");
                record.setSiteId(SiteContext.getSiteId());
                generationRecordMapper.insert(record);
                
                // 抛出异常，使用特定的错误消息前缀以便在catch块中识别
                throw new RuntimeException("VIDEO_GENERATION_FAILED:" + (friendlyErrorMsg != null ? friendlyErrorMsg : "未知错误"));
            } else {
                throw new RuntimeException("未知的视频生成状态：" + status);
            }
            
            return response;
            
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            // 检查是否是业务异常（已经保存了失败记录的异常）
            // 通过检查异常消息是否以"VIDEO_GENERATION_FAILED:"开头来判断
            boolean alreadySaved = errorMessage != null && errorMessage.startsWith("VIDEO_GENERATION_FAILED:");
            
            if (alreadySaved) {
                // 业务异常已保存记录，只需记录日志，并提取原始错误消息
                String originalError = errorMessage.substring("VIDEO_GENERATION_FAILED:".length());
                log.error("图生视频失败：{}", originalError);
                throw new RuntimeException("视频生成失败：" + originalError);
            } else {
                // 其他异常（如API调用失败等），记录错误日志并保存失败记录
                log.error("图生视频失败：{}", errorMessage, e);
                
                // 保存失败记录
                try {
                    GenerationRecord record = new GenerationRecord();
                    record.setUserId(userId);
                    record.setUsername(user.getUsername());
                    record.setType("img2video");
                    record.setModel(request.getModel());
                    record.setPrompt(request.getPrompt());
                    record.setStatus("failed");
                    record.setSiteId(SiteContext.getSiteId());
                    generationRecordMapper.insert(record);
                } catch (Exception ex) {
                    log.error("保存失败记录时出错：{}", ex.getMessage());
                }
                
                throw new RuntimeException("图生视频失败：" + errorMessage);
            }
        }
    }
    
    /**
     * 构建文生视频请求参数
     * 根据API文档构建符合要求的参数
     */
    private Map<String, Object> buildTextToVideoRequest(TextToVideoRequest request, ApiPlatform platform) {
        Map<String, Object> params = new HashMap<>();
        
        // 必填参数：提示词（只支持英文）
        params.put("prompt", request.getPrompt());
        
        // 必填参数：模型
        // 支持模型: veo3.1-fast, veo3.1-pro, veo3-fast, veo3-pro
        // 注意：veo3-fast和veo3-pro已被官方下架，需要自动替换为veo3.1模型
        String model = request.getModel();
        if (model != null && !model.isEmpty()) {
            // 检查是否是已下架的veo3模型，如果是则自动替换为veo3.1模型
            if ("veo3-fast".equals(model)) {
                log.warn("检测到已下架的模型 veo3-fast，自动替换为 veo3.1-fast");
                model = "veo3.1-fast";
            } else if ("veo3-pro".equals(model)) {
                log.warn("检测到已下架的模型 veo3-pro，自动替换为 veo3.1-pro");
                model = "veo3.1-pro";
            }
            params.put("model", model);
        } else {
            // 如果没有指定模型，使用默认模型
            params.put("model", "veo3.1-fast");
        }
        
        // 选填参数：首帧图片URL
        // 支持模型: veo3-fast, veo3-pro, veo3.1-fast, veo3.1-pro
        if (request.getFirstFrameUrl() != null && !request.getFirstFrameUrl().trim().isEmpty()) {
            params.put("firstFrameUrl", request.getFirstFrameUrl());
        }
        
        // 选填参数：尾帧图片URL（需搭配firstFrameUrl使用）
        // 支持模型: veo3.1-fast, veo3.1-pro
        if (request.getLastFrameUrl() != null && !request.getLastFrameUrl().trim().isEmpty()) {
            // 检查是否同时设置了firstFrameUrl
            if (request.getFirstFrameUrl() == null || request.getFirstFrameUrl().trim().isEmpty()) {
                log.warn("尾帧图片URL需要搭配首帧图片URL使用，忽略lastFrameUrl参数");
            } else {
                params.put("lastFrameUrl", request.getLastFrameUrl());
            }
        }
        
        // 选填参数：参考图片URL数组（最多支持三张图片，不可搭配首尾帧使用）
        // 支持尺寸: 16:9
        // 支持模型: veo3.1-fast
        if (request.getUrls() != null && !request.getUrls().isEmpty()) {
            // 检查是否同时设置了首尾帧（如果设置了首尾帧，则不能使用urls）
            if ((request.getFirstFrameUrl() != null && !request.getFirstFrameUrl().trim().isEmpty()) ||
                (request.getLastFrameUrl() != null && !request.getLastFrameUrl().trim().isEmpty())) {
                log.warn("参考图片URL数组不可搭配首尾帧使用，忽略urls参数");
            } else {
                // 限制最多3张图片
                List<String> urls = request.getUrls();
                if (urls.size() > 3) {
                    log.warn("参考图片URL数组最多支持3张，已截取前3张");
                    urls = urls.subList(0, 3);
                }
                params.put("urls", urls);
            }
        }
        
        // 选填参数：视频宽高比（默认16:9）
        // 支持的比例: 16:9, 9:16
        if (request.getAspectRatio() != null && !request.getAspectRatio().isEmpty() && !"Auto".equals(request.getAspectRatio())) {
            // 验证宽高比是否支持
            if ("16:9".equals(request.getAspectRatio()) || "9:16".equals(request.getAspectRatio())) {
                params.put("aspectRatio", request.getAspectRatio());
            } else {
                log.warn("不支持的宽高比: {}，使用默认值16:9", request.getAspectRatio());
                params.put("aspectRatio", "16:9");
            }
        } else {
            // 默认值16:9
            params.put("aspectRatio", "16:9");
        }
        
        // 选填参数：回调接口URL
        // 如果填了webHook，进度与结果则以Post请求回调地址的方式进行回复
        // 如果webHook为"-1"，会立即返回一个id用于轮询
        if (request.getWebHook() != null && !request.getWebHook().trim().isEmpty()) {
            params.put("webHook", request.getWebHook());
        }
        
        // 选填参数：关闭进度回复，直接回复最终结果（默认false）
        // 建议搭配webHook使用
        if (request.getShutProgress() != null) {
            params.put("shutProgress", request.getShutProgress());
        } else {
            params.put("shutProgress", false);
        }
        
        return params;
    }
    
    /**
     * 构建图生视频请求参数
     */
    private Map<String, Object> buildImageToVideoRequest(ImageToVideoRequest request, ApiPlatform platform) {
        Map<String, Object> params = new HashMap<>();
        
        // 基础参数
        params.put("prompt", request.getPrompt());
        params.put("image", request.getImage()); // 参考图片
        
        // 处理宽高比
        if (request.getAspectRatio() != null && !request.getAspectRatio().isEmpty() && !"Auto".equals(request.getAspectRatio())) {
            params.put("aspect_ratio", request.getAspectRatio());
        }
        
        // 处理分辨率
        if (request.getResolution() != null && !request.getResolution().isEmpty()) {
            Map<String, Integer> size = parseResolution(request.getResolution(), request.getAspectRatio());
            params.put("width", size.get("width"));
            params.put("height", size.get("height"));
        }
        
        // 时长
        if (request.getDuration() != null) {
            params.put("duration", request.getDuration());
        }
        
        // 模型（添加自动替换逻辑，将已下架的veo3模型替换为veo3.1模型）
        String model = request.getModel();
        if (model != null && !model.isEmpty()) {
            // 检查是否是已下架的veo3模型，如果是则自动替换为veo3.1模型
            if ("veo3-fast".equals(model)) {
                log.warn("检测到已下架的模型 veo3-fast，自动替换为 veo3.1-fast");
                model = "veo3.1-fast";
            } else if ("veo3-pro".equals(model)) {
                log.warn("检测到已下架的模型 veo3-pro，自动替换为 veo3.1-pro");
                model = "veo3.1-pro";
            }
            params.put("model", model);
        } else {
            // 如果没有指定模型，使用默认模型
            params.put("model", "veo3.1-fast");
        }
        
        return params;
    }
    
    /**
     * 视频响应信息内部类
     */
    private static class VideoResponseInfo {
        private String taskId; // 任务ID
        private String videoUrl; // 视频URL
        private String status; // 状态：running, succeeded, failed
        private Integer progress; // 进度：0~100
        private String failureReason; // 失败原因
        private String errorMessage; // 错误信息
        
        // Getters and Setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        
        public String getVideoUrl() { return videoUrl; }
        public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Integer getProgress() { return progress; }
        public void setProgress(Integer progress) { this.progress = progress; }
        
        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    /**
     * 解析视频响应（支持多种响应格式）
     * 
     * @param responseJson 响应JSON字符串
     * @param responseMode 响应模式：JSON、Stream
     * @return 视频响应信息
     */
    private VideoResponseInfo parseVideoResponse(String responseJson, String responseMode) {
        VideoResponseInfo info = new VideoResponseInfo();
        
        try {
            // 如果是Stream模式，解析SSE格式
            if ("Stream".equals(responseMode) || (responseJson != null && responseJson.trim().startsWith("data:"))) {
                return parseSseVideoResponse(responseJson);
            }
            
            // 普通JSON格式
            JsonNode root = objectMapper.readTree(responseJson);
            
            // 格式1: webHook="-1"时立即返回id（用于轮询）
            // {
            //   "code": 0,
            //   "msg": "success",
            //   "data": {
            //     "id": "id"
            //   }
            // }
            if (root.has("code") && root.has("data")) {
                int code = root.get("code").asInt();
                if (code == 0 && root.get("data").has("id")) {
                    info.setTaskId(root.get("data").get("id").asText());
                    info.setStatus("running"); // 刚创建的任务，状态为running
                    return info;
                }
            }
            
            // 格式2: 流式响应和webHook响应的参数
            // {
            //   "id": "xxxxx",
            //   "url": "https://example.com/example.mp4",
            //   "progress": 100,
            //   "status": "succeeded",
            //   "failure_reason": "",
            //   "error": ""
            // }
            if (root.has("id")) {
                info.setTaskId(root.get("id").asText());
            }
            
            if (root.has("url")) {
                info.setVideoUrl(root.get("url").asText());
            }
            
            if (root.has("status")) {
                String status = root.get("status").asText();
                info.setStatus(status);
            }
            
            if (root.has("progress")) {
                info.setProgress(root.get("progress").asInt());
            }
            
            if (root.has("failure_reason")) {
                String failureReason = root.get("failure_reason").asText();
                info.setFailureReason(fixEncoding(failureReason));
            }
            
            if (root.has("error")) {
                String error = root.get("error").asText();
                info.setErrorMessage(fixEncoding(error));
            }
            
            // 如果没有status但有url，认为成功
            if (info.getStatus() == null && info.getVideoUrl() != null && !info.getVideoUrl().isEmpty()) {
                info.setStatus("succeeded");
            }
            
            // 兼容旧格式：直接返回video_url或videoUrl
            if (info.getVideoUrl() == null || info.getVideoUrl().isEmpty()) {
                if (root.has("video_url")) {
                    info.setVideoUrl(root.get("video_url").asText());
                    info.setStatus("succeeded");
                } else if (root.has("videoUrl")) {
                    info.setVideoUrl(root.get("videoUrl").asText());
                    info.setStatus("succeeded");
                } else if (root.has("data") && root.get("data").has("url")) {
                    info.setVideoUrl(root.get("data").get("url").asText());
                    info.setStatus("succeeded");
                }
            }
            
            // 检查是否有错误信息
            if (root.has("code")) {
                int code = root.get("code").asInt();
                if (code != 0 && code != 200) {
                    String errorMsg = "API调用失败";
                    if (root.has("msg")) {
                        errorMsg = root.get("msg").asText();
                    } else if (root.has("message")) {
                        errorMsg = root.get("message").asText();
                    }
                    info.setStatus("failed");
                    info.setErrorMessage(errorMsg);
                    throw new RuntimeException(errorMsg);
                }
            }
            
            // 如果既没有taskId也没有videoUrl，也没有错误信息，抛出异常
            if ((info.getTaskId() == null || info.getTaskId().isEmpty()) && 
                (info.getVideoUrl() == null || info.getVideoUrl().isEmpty()) &&
                (info.getStatus() == null || !"failed".equals(info.getStatus()))) {
                throw new RuntimeException("未找到视频URL或任务ID");
            }
            
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析视频响应失败：{}", e.getMessage(), e);
            throw new RuntimeException("解析视频响应失败：" + e.getMessage());
        }
        
        return info;
    }
    
    /**
     * 解析SSE格式的视频响应
     * 
     * @param sseResponse SSE格式的响应字符串
     * @return 视频响应信息
     */
    private VideoResponseInfo parseSseVideoResponse(String sseResponse) {
        VideoResponseInfo info = new VideoResponseInfo();
        
        try {
            // 按行分割SSE数据
            String[] lines = sseResponse.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                
                // 检查是否是data行
                if (line.startsWith("data: ")) {
                    String jsonData = line.substring(6); // 移除 "data: " 前缀
                    
                    try {
                        // 解析JSON数据
                        JsonNode dataNode = objectMapper.readTree(jsonData);
                        
                        // 提取信息（每次循环都更新info，保留最新的数据）
                        if (dataNode.has("id")) {
                            info.setTaskId(dataNode.get("id").asText());
                        }
                        if (dataNode.has("url")) {
                            info.setVideoUrl(dataNode.get("url").asText());
                        }
                        if (dataNode.has("status")) {
                            info.setStatus(dataNode.get("status").asText());
                        }
                        if (dataNode.has("progress")) {
                            info.setProgress(dataNode.get("progress").asInt());
                        }
                        if (dataNode.has("failure_reason")) {
                            String failureReason = dataNode.get("failure_reason").asText();
                            info.setFailureReason(fixEncoding(failureReason));
                        }
                        if (dataNode.has("error")) {
                            String error = dataNode.get("error").asText();
                            info.setErrorMessage(fixEncoding(error));
                        }
                        
                    } catch (Exception e) {
                        log.debug("解析SSE数据行失败：{}", e.getMessage());
                    }
                }
            }
            
            // 注意：这里不抛出异常，即使状态为failed也返回info对象
            // 让调用方（generateTextToVideo方法）统一处理失败逻辑
            // 这样可以保存失败记录并返回友好的错误信息
            
        } catch (RuntimeException e) {
            // 重新抛出运行时异常（但不包括failed状态的异常）
            throw e;
        } catch (Exception e) {
            log.error("解析SSE视频响应失败：{}", e.getMessage(), e);
            throw new RuntimeException("解析SSE视频响应失败：" + e.getMessage());
        }
        
        return info;
    }
    
    /**
     * 尝试修复字符串编码问题
     * 如果字符串看起来是UTF-8编码被错误解析为ISO-8859-1，尝试修复
     * 
     * @param str 可能编码错误的字符串
     * @return 修复后的字符串
     */
    private String fixEncoding(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        try {
            // 检查字符串是否包含乱码字符（典型的UTF-8被错误解析为ISO-8859-1的特征）
            // 如果字符串包含类似 "å®" 这样的字符，很可能是编码问题
            if (str.contains("å") || str.contains("æ") || str.contains("é")) {
                // 尝试将字符串重新编码：先按ISO-8859-1编码，再按UTF-8解码
                byte[] bytes = str.getBytes("ISO-8859-1");
                String fixed = new String(bytes, "UTF-8");
                // 验证修复后的字符串是否有效（不包含乱码）
                if (fixed.matches(".*[\\u4e00-\\u9fa5]+.*") || !fixed.contains("")) {
                    return fixed;
                }
            }
        } catch (Exception e) {
            // 如果修复失败，返回原字符串
            log.debug("修复编码失败，使用原字符串: {}", e.getMessage());
        }
        
        return str;
    }
    
    /**
     * 计算消耗积分
     */
    private Integer calculateCost(Integer quantity, String type) {
        // 简单计算：文生图每个10积分，图生图每个15积分，视频每个20积分
        int baseCost;
        if ("txt2img".equals(type)) {
            baseCost = 10;
        } else if ("img2img".equals(type)) {
            baseCost = 15;
        } else if ("txt2video".equals(type) || "img2video".equals(type)) {
            baseCost = 20;
        } else {
            baseCost = 10; // 默认
        }
        return baseCost * (quantity != null ? quantity : 1);
    }
}
