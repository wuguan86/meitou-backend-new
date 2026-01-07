package com.meitou.admin.service.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meitou.admin.common.SiteContext;
import com.meitou.admin.dto.app.VideoAnalysisRequest;
import com.meitou.admin.entity.*;
import com.meitou.admin.exception.BusinessException;
import com.meitou.admin.exception.ErrorCode;
import com.meitou.admin.mapper.AnalysisRecordMapper;
import com.meitou.admin.mapper.UserMapper;
import com.meitou.admin.mapper.UserTransactionMapper;
import com.meitou.admin.service.admin.ApiPlatformService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class VideoAnalysisService {

    private final ApiPlatformService apiPlatformService;
    private final UserMapper userMapper;
    private final UserTransactionMapper userTransactionMapper;
    private final AnalysisRecordMapper analysisRecordMapper;
    private final TransactionTemplate transactionTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS) // Longer timeout for video analysis
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private void runWithSiteContext(Long siteId, Runnable runnable) {
        Long originalSiteId = SiteContext.getSiteId();
        try {
            SiteContext.setSiteId(siteId);
            runnable.run();
        } finally {
            if (originalSiteId == null) {
                SiteContext.clear();
            } else {
                SiteContext.setSiteId(originalSiteId);
            }
        }
    }

    public VideoAnalysisService(ApiPlatformService apiPlatformService,
                                UserMapper userMapper,
                                UserTransactionMapper userTransactionMapper,
                                AnalysisRecordMapper analysisRecordMapper,
                                TransactionTemplate transactionTemplate) {
        this.apiPlatformService = apiPlatformService;
        this.userMapper = userMapper;
        this.userTransactionMapper = userTransactionMapper;
        this.analysisRecordMapper = analysisRecordMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public SseEmitter analyzeVideo(VideoAnalysisRequest request, Long userId) {
        // 1. Get User
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 2. Find Platform
        ApiPlatform platform = apiPlatformService.getPlatformByTypeAndModel("video_analysis", request.getModel(), SiteContext.getSiteId());
        if (platform == null) {
            throw new BusinessException(ErrorCode.GENERATION_PLATFORM_NOT_CONFIGURED.getCode(), "视频分析平台未配置");
        }

        // 3. Find Interface
        List<ApiInterface> interfaces = apiPlatformService.getInterfacesByPlatformId(platform.getId());
        ApiInterface apiInterface = interfaces.stream()
                .filter(i -> i.getResponseMode() != null && !"Result".equals(i.getResponseMode()))
                .findFirst()
                .orElse(null);
        if (apiInterface == null) {
            throw new BusinessException(ErrorCode.GENERATION_INTERFACE_NOT_CONFIGURED.getCode(), "视频分析接口未配置");
        }

        // 4. Get Config (Chart Profile & Cost)
        String chartProfile = "";
        int cost = 100; // Default
        String actualModel = request.getModel();

        if (platform.getSupportedModels() != null) {
            try {
                // Check if supportedModels is JSON array
                if (platform.getSupportedModels().trim().startsWith("[")) {
                    JsonNode models = objectMapper.readTree(platform.getSupportedModels());
                    for (JsonNode m : models) {
                        boolean isMatch = false;
                        if (m.has("name") && m.get("name").asText().equals(request.getModel())) {
                            isMatch = true;
                        } else if (m.has("id") && m.get("id").asText().equals(request.getModel())) {
                            isMatch = true;
                        } else if (m.has("value") && m.get("value").asText().equals(request.getModel())) {
                            isMatch = true;
                        }

                        if (isMatch) {
                            if (m.has("name")) {
                                actualModel = m.get("name").asText();
                            }
                            if (m.has("chartProfile")) {
                                chartProfile = m.get("chartProfile").asText();
                            }
                            if (m.has("defaultCost")) {
                                cost = m.get("defaultCost").asInt();
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to parse supported models", e);
            }
        }

        // 5. Deduct Credits
        int finalCost = cost;
        String finalModel = actualModel;
        User currentUser = userMapper.selectById(userId);
        if (currentUser.getBalance() < finalCost) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        AnalysisRecord analysisRecord = transactionTemplate.execute(status -> {
            // Deduct points
            int newBalance = currentUser.getBalance() - finalCost;
            currentUser.setBalance(newBalance);
            userMapper.updateById(currentUser);

            // Save Analysis Record (Pending)
            AnalysisRecord record = new AnalysisRecord();
            record.setUserId(userId);
            record.setType("video");
            record.setContent(request.getVideo());
            record.setStatus(0); // Pending
            record.setSiteId(SiteContext.getSiteId());
            analysisRecordMapper.insert(record);

            // Record transaction
            UserTransaction transaction = new UserTransaction();
            transaction.setUserId(userId);
            transaction.setType("CONSUME");
            transaction.setAmount(-finalCost);
            transaction.setBalanceAfter(newBalance);
            transaction.setDescription("视频分析-" + finalModel);
            transaction.setReferenceId(record.getId());
            transaction.setSiteId(SiteContext.getSiteId());
            userTransactionMapper.insert(transaction);

            return record;
        });

        // 6. Call API and Stream
        SseEmitter emitter = new SseEmitter(300000L); // 5 mins timeout for video
        Long recordSiteId = analysisRecord.getSiteId();

        try {
            // Build Body
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", actualModel);
            root.put("stream", true);

            ArrayNode messages = root.putArray("messages");

            // System Message
            if (chartProfile != null && !chartProfile.isEmpty()) {
                ObjectNode systemMsg = messages.addObject();
                systemMsg.put("role", "system");
                systemMsg.put("content", chartProfile);
            }

            // User Message
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            ArrayNode content = userMsg.putArray("content");

            // Text
            ObjectNode textContent = content.addObject();
            textContent.put("type", "text");
            textContent.put("text", request.getDirection() != null && !request.getDirection().isEmpty() ? request.getDirection() : "Analyze this video");

            // Video - Using image_url type as many OpenAI-compatible adapters for multimodal models
            // (like Gemini) expect visual content (images or video) via the image_url field.
            ObjectNode videoContentObj = content.addObject();
            videoContentObj.put("type", "image_url");
            ObjectNode videoUrlObj = videoContentObj.putObject("image_url");
            videoUrlObj.put("url", request.getVideo());

            // Build Request
            String jsonBody = objectMapper.writeValueAsString(root);
            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

            Request.Builder reqBuilder = new Request.Builder()
                    .url(apiInterface.getUrl())
                    .post(body);

            // Add Headers
            if (platform.getApiKey() != null && !platform.getApiKey().isEmpty()) {
                reqBuilder.addHeader("Authorization", "Bearer " + platform.getApiKey());
            }

            // Custom headers from interface config
            if (apiInterface.getHeaders() != null && !apiInterface.getHeaders().isEmpty()) {
                try {
                    JsonNode headersNode = objectMapper.readTree(apiInterface.getHeaders());
                    if (headersNode.isArray()) {
                        for (JsonNode h : headersNode) {
                            if (h.has("key") && h.has("value")) {
                                reqBuilder.addHeader(h.get("key").asText(), h.get("value").asText());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to parse headers", e);
                }
            }

            okHttpClient.newCall(reqBuilder.build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runWithSiteContext(recordSiteId, () -> {
                        analysisRecord.setStatus(2);
                        analysisRecord.setErrorMsg(e.getMessage());
                        analysisRecordMapper.updateById(analysisRecord);
                    });

                    try {
                        emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                        emitter.complete();
                    } catch (IOException ex) {
                        log.error("Error sending SSE failure", ex);
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    StringBuilder fullResponse = new StringBuilder();
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful()) {
                            String errorBody = responseBody != null ? responseBody.string() : "";
                            log.error("API Error: {} - {}", response.code(), errorBody);

                            runWithSiteContext(recordSiteId, () -> {
                                analysisRecord.setStatus(2);
                                analysisRecord.setErrorMsg("API Error: " + response.code());
                                analysisRecordMapper.updateById(analysisRecord);
                            });

                            emitter.send(SseEmitter.event().name("error").data("API Error: " + response.code()));
                            emitter.complete();
                            return;
                        }

                        if (responseBody == null) {
                            emitter.complete();
                            return;
                        }

                        okio.BufferedSource source = responseBody.source();
                        boolean isFinished = false;
                        StringBuilder lineBuffer = new StringBuilder();
                        byte[] buffer = new byte[8192];

                        while (!source.exhausted()) {
                            int read = source.read(buffer);
                            if (read == -1) break;

                            String chunk = new String(buffer, 0, read);
                            lineBuffer.append(chunk);

                            int newlineIndex;
                            while ((newlineIndex = lineBuffer.indexOf("\n")) != -1) {
                                String line = lineBuffer.substring(0, newlineIndex).trim();
                                lineBuffer.delete(0, newlineIndex + 1);

                                if (!line.isEmpty() && line.startsWith("data: ")) {
                                    String data = line.substring(6).trim();
                                    if ("[DONE]".equals(data)) {
                                        isFinished = true;
                                        runWithSiteContext(recordSiteId, () -> {
                                            analysisRecord.setStatus(1);
                                            analysisRecord.setResult(fullResponse.toString());
                                            analysisRecordMapper.updateById(analysisRecord);
                                        });
                                        emitter.complete();
                                        return;
                                    }
                                    try {
                                        JsonNode node = objectMapper.readTree(data);
                                        if (node.has("choices") && node.get("choices").isArray() && node.get("choices").size() > 0) {
                                            JsonNode choice = node.get("choices").get(0);
                                            if (choice.has("delta") && choice.get("delta").has("content")) {
                                                String content = choice.get("delta").get("content").asText();
                                                fullResponse.append(content);
                                            }
                                        }
                                        emitter.send(SseEmitter.event().data(data));
                                    } catch (Exception e) {
                                        // Ignore parse errors
                                    }
                                }
                            }
                        }

                        if (lineBuffer.length() > 0) {
                             String line = lineBuffer.toString().trim();
                             if (!line.isEmpty() && line.startsWith("data: ")) {
                                 String data = line.substring(6).trim();
                                 if (!"[DONE]".equals(data)) {
                                     try {
                                         JsonNode node = objectMapper.readTree(data);
                                         if (node.has("choices") && node.get("choices").isArray() && node.get("choices").size() > 0) {
                                             JsonNode choice = node.get("choices").get(0);
                                             if (choice.has("delta") && choice.get("delta").has("content")) {
                                                 String content = choice.get("delta").get("content").asText();
                                                 fullResponse.append(content);
                                             }
                                         }
                                         emitter.send(SseEmitter.event().data(data));
                                     } catch (Exception e) {
                                         // Ignore
                                     }
                                 }
                             }
                        }

                        if (!isFinished && fullResponse.length() > 0) {
                            runWithSiteContext(recordSiteId, () -> {
                                analysisRecord.setStatus(1);
                                analysisRecord.setResult(fullResponse.toString());
                                analysisRecordMapper.updateById(analysisRecord);
                            });
                        }

                        emitter.complete();
                    } catch (Exception e) {
                        runWithSiteContext(recordSiteId, () -> {
                            analysisRecord.setStatus(2);
                            analysisRecord.setErrorMsg(e.getMessage());
                            analysisRecordMapper.updateById(analysisRecord);
                        });

                        try {
                            emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                            emitter.complete();
                        } catch (IOException ex) {
                            // ignore
                        }
                    }
                }
            });

        } catch (Exception e) {
            runWithSiteContext(recordSiteId, () -> {
                analysisRecord.setStatus(2);
                analysisRecord.setErrorMsg(e.getMessage());
                analysisRecordMapper.updateById(analysisRecord);
            });
            emitter.completeWithError(e);
        }

        return emitter;
    }
}
