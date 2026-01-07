package com.meitou.admin.service.app;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.CertAlipayRequest;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderResult;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.meitou.admin.config.PaymentProperties;
import com.meitou.admin.exception.BusinessException;
import com.meitou.admin.exception.ErrorCode;
import com.meitou.admin.util.AesEncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 支付服务类
 * 处理微信支付和支付宝支付的统一下单、回调验证等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 创建微信支付订单
     * 
     * @param orderNo 订单号
     * @param amount 金额（元）
     * @param description 商品描述
     * @param configJson 支付配置JSON（可选，如果提供则优先使用，否则使用application.yml中的配置）
     * @return 支付参数（包含二维码URL或支付链接等）
     */
    public Map<String, String> createWechatPayment(String orderNo, String amount, String description, String configJson) {
        try {
            log.info("创建微信支付订单：订单号={}, 金额={}, 描述={}", orderNo, amount, description);
            
            // 解析配置（优先使用传入的configJson，否则使用application.yml中的配置）
            PaymentProperties.WechatPayConfig config = parseWechatConfig(configJson);
            
            // 配置微信支付参数
            WxPayConfig wxPayConfig = new WxPayConfig();
            wxPayConfig.setAppId(config.getAppId());
            wxPayConfig.setMchId(config.getMchId());
            wxPayConfig.setMchKey(config.getMchKey());
            wxPayConfig.setUseSandboxEnv(config.getUseSandbox());
            
            // API V3配置
            if (StringUtils.hasText(config.getApiV3Key())) {
                wxPayConfig.setApiV3Key(config.getApiV3Key());
            }
            if (StringUtils.hasText(config.getCertSerialNo())) {
                wxPayConfig.setCertSerialNo(config.getCertSerialNo());
            }
            if (StringUtils.hasText(config.getCertContent())) {
                wxPayConfig.setPrivateCertContent(config.getCertContent().getBytes(StandardCharsets.UTF_8));
            }
            if (StringUtils.hasText(config.getPrivateKey())) {
                wxPayConfig.setPrivateKeyContent(config.getPrivateKey().getBytes(StandardCharsets.UTF_8));
            }
            
            // 兼容旧版证书路径配置
            if (StringUtils.hasText(config.getCertPath())) {
                try {
                    ClassPathResource resource = new ClassPathResource(config.getCertPath().replace("classpath:", ""));
                    if (resource.exists()) {
                        wxPayConfig.setKeyPath(resource.getFile().getAbsolutePath());
                    }
                } catch (Exception e) {
                    log.warn("加载微信支付证书文件失败，将尝试使用内容配置：{}", e.getMessage());
                }
            }
            
            // 创建微信支付服务
            WxPayService wxPayService = new WxPayServiceImpl();
            wxPayService.setConfig(wxPayConfig);
            
            // 构建统一下单请求
            WxPayUnifiedOrderRequest request = new WxPayUnifiedOrderRequest();
            request.setBody(description); // 商品描述
            request.setOutTradeNo(orderNo); // 商户订单号
            request.setTotalFee(Integer.parseInt(new BigDecimal(amount).multiply(new BigDecimal("100")).toPlainString())); // 总金额（分）
            request.setSpbillCreateIp("127.0.0.1"); // 终端IP
            request.setNotifyUrl(config.getNotifyUrl()); // 支付结果通知回调地址
            request.setTradeType("NATIVE"); // 交易类型：NATIVE-扫码支付
            
            // 调用统一下单接口
            WxPayUnifiedOrderResult result = wxPayService.unifiedOrder(request);
            
            // 构建返回结果
            Map<String, String> paymentParams = new HashMap<>();
            
            // 微信支付扫码支付返回的二维码URL
            String codeUrl = result.getCodeURL();
            
            paymentParams.put("qrCodeUrl", codeUrl != null ? codeUrl : ""); // 二维码URL
            paymentParams.put("orderId", result.getPrepayId() != null ? result.getPrepayId() : ""); // 微信支付订单号
            paymentParams.put("paymentUrl", codeUrl != null ? codeUrl : ""); // 支付链接（扫码支付时与二维码URL相同）
            
            log.info("微信支付订单创建成功：订单号={}, 二维码URL={}", orderNo, codeUrl);
            return paymentParams;
            
        } catch (WxPayException e) {
            log.error("创建微信支付订单失败，错误码：{}, 错误信息：{}", e.getErrCode(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.PAYMENT_ORDER_CREATE_FAILED.getCode(), "创建微信支付订单失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("创建微信支付订单失败", e);
            throw new BusinessException(ErrorCode.PAYMENT_ORDER_CREATE_FAILED.getCode(), "创建微信支付订单失败：" + e.getMessage());
        }
    }
    
    /**
     * 创建支付宝支付订单
     * 
     * @param orderNo 订单号
     * @param amount 金额（元）
     * @param description 商品描述
     * @param configJson 支付配置JSON（可选，如果提供则优先使用，否则使用application.yml中的配置）
     * @return 支付参数（包含支付链接或二维码URL等）
     */
    public Map<String, String> createAlipayPayment(String orderNo, String amount, String description, String configJson) {
        try {
            log.info("创建支付宝支付订单：订单号={}, 金额={}, 描述={}", orderNo, amount, description);
            
            // 解析配置（优先使用传入的configJson，否则使用application.yml中的配置）
            PaymentProperties.AlipayConfig config = parseAlipayConfig(configJson);
            
            AlipayClient alipayClient;
            
            // 检查是否使用证书模式
            if (StringUtils.hasText(config.getAppCertContent()) && 
                StringUtils.hasText(config.getAlipayRootCertContent()) && 
                StringUtils.hasText(config.getAlipayCertContent())) {
                
                log.info("使用证书模式初始化支付宝客户端");
                CertAlipayRequest certParams = new CertAlipayRequest();
                certParams.setServerUrl(config.getGatewayUrl());
                certParams.setAppId(config.getAppId());
                certParams.setPrivateKey(config.getPrivateKey());
                certParams.setFormat(config.getFormat());
                certParams.setCharset(config.getCharset());
                certParams.setSignType(config.getSignType());
                
                certParams.setCertContent(config.getAppCertContent());
                certParams.setRootCertContent(config.getAlipayRootCertContent());
                certParams.setAlipayPublicCertContent(config.getAlipayCertContent());
                
                alipayClient = new DefaultAlipayClient(certParams);
            } else {
                // 普通公钥模式
                log.info("使用公钥模式初始化支付宝客户端");
                alipayClient = new DefaultAlipayClient(
                    config.getGatewayUrl(), // 支付宝网关地址
                    config.getAppId(), // 应用ID
                    config.getPrivateKey(), // 商户私钥
                    config.getFormat(), // 数据格式
                    config.getCharset(), // 字符编码
                    config.getAlipayPublicKey(), // 支付宝公钥
                    config.getSignType() // 签名类型
                );
            }
            
            // 构建请求参数（使用扫码支付）
            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            request.setNotifyUrl(config.getNotifyUrl()); // 异步通知地址
            
            AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
            model.setOutTradeNo(orderNo); // 商户订单号
            model.setTotalAmount(amount); // 订单总金额（元）
            model.setSubject(description); // 订单标题
            request.setBizModel(model);
            
            // 调用支付宝接口
            AlipayTradePrecreateResponse response = alipayClient.execute(request);
            
            if (!response.isSuccess()) {
                log.error("创建支付宝支付订单失败，错误码：{}, 错误信息：{}", response.getCode(), response.getMsg());
                throw new BusinessException(ErrorCode.PAYMENT_ORDER_CREATE_FAILED.getCode(), "创建支付宝支付订单失败：" + response.getMsg());
            }
            
            // 构建返回结果
            Map<String, String> paymentParams = new HashMap<>();
            paymentParams.put("qrCodeUrl", response.getQrCode()); // 二维码URL
            paymentParams.put("orderId", response.getOutTradeNo()); // 商户订单号
            paymentParams.put("paymentUrl", response.getQrCode()); // 支付链接（扫码支付时与二维码URL相同）
            
            log.info("支付宝支付订单创建成功：订单号={}, 二维码URL={}", orderNo, response.getQrCode());
            return paymentParams;
            
        } catch (AlipayApiException e) {
            log.error("创建支付宝支付订单失败，错误码：{}, 错误信息：{}", e.getErrCode(), e.getErrMsg(), e);
            // 避免将详细的技术错误暴露给用户
            throw new BusinessException(ErrorCode.PAYMENT_ORDER_CREATE_FAILED.getCode(), "支付宝下单失败，请稍后重试或联系客服");
        } catch (Exception e) {
            log.error("创建支付宝支付订单失败", e);
            throw new BusinessException(ErrorCode.PAYMENT_ORDER_CREATE_FAILED.getCode(), "支付系统异常，请稍后重试");
        }
    }
    
    /**
     * 验证微信支付回调签名
     * 
     * @param callbackXml 回调数据（XML格式的字符串）
     * @param configJson 支付配置JSON（可选）
     * @return 是否验证通过
     */
    public boolean verifyWechatCallback(String callbackXml, String configJson) {
        try {
            log.info("验证微信支付回调签名，XML长度：{}", callbackXml != null ? callbackXml.length() : 0);
            
            // 解析配置
            PaymentProperties.WechatPayConfig config = parseWechatConfig(configJson);
            
            // 配置微信支付参数
            WxPayConfig wxPayConfig = new WxPayConfig();
            wxPayConfig.setAppId(config.getAppId());
            wxPayConfig.setMchId(config.getMchId());
            wxPayConfig.setMchKey(config.getMchKey());
            wxPayConfig.setUseSandboxEnv(config.getUseSandbox());
            
            // 创建微信支付服务
            WxPayService wxPayService = new WxPayServiceImpl();
            wxPayService.setConfig(wxPayConfig);
            
            // 使用微信支付SDK解析XML并验证签名
            // 微信支付SDK提供了parseOrderNotifyResult方法来解析回调并验证签名
            try {
                // 这里使用微信支付SDK的XML解析功能
                // 由于微信支付SDK版本不同，API可能有所差异
                // 使用反射或者直接解析XML进行签名验证
                
                // 简化实现：提取sign字段，手动验证签名
                // 实际项目中应该使用微信支付SDK提供的验证方法
                String sign = extractXmlValue(callbackXml, "sign");
                if (!StringUtils.hasText(sign)) {
                    log.error("微信支付回调XML中缺少sign字段");
                    return false;
                }
                
                // 移除sign字段，构建待签名字符串
                String signContent = buildWechatSignContent(callbackXml);
                
                // 使用MD5计算签名（微信支付V2使用MD5签名）
                String calculatedSign = calculateWechatSign(signContent, config.getMchKey());
                
                boolean verified = sign.equalsIgnoreCase(calculatedSign);
                if (verified) {
                    log.info("微信支付回调签名验证通过");
                } else {
                    log.error("微信支付回调签名验证失败");
                }
                
                return verified;
                
            } catch (Exception e) {
                log.error("验证微信支付回调签名时发生异常", e);
                // 如果验证失败，返回false
                return false;
            }
            
        } catch (Exception e) {
            log.error("验证微信支付回调签名失败", e);
            return false;
        }
    }
    
    /**
     * 从XML中提取指定字段的值
     */
    private String extractXmlValue(String xml, String fieldName) {
        try {
            String startTag = "<" + fieldName + ">";
            String endTag = "</" + fieldName + ">";
            int start = xml.indexOf(startTag);
            if (start == -1) {
                // 尝试CDATA格式
                startTag = "<" + fieldName + "><![CDATA[";
                endTag = "]]></" + fieldName + ">";
                start = xml.indexOf(startTag);
                if (start != -1) {
                    start += startTag.length();
                    int end = xml.indexOf(endTag, start);
                    if (end != -1) {
                        return xml.substring(start, end);
                    }
                }
                return null;
            }
            start += startTag.length();
            int end = xml.indexOf(endTag, start);
            if (end != -1) {
                String value = xml.substring(start, end);
                // 如果是CDATA，需要去除CDATA标记
                if (value.startsWith("<![CDATA[") && value.endsWith("]]>")) {
                    value = value.substring(9, value.length() - 3);
                }
                return value;
            }
        } catch (Exception e) {
            log.error("提取XML字段值失败：{}", fieldName, e);
        }
        return null;
    }
    
    /**
     * 构建微信支付签名字符串（移除sign字段，按字典序排列，用&连接）
     */
    private String buildWechatSignContent(String xml) {
        // 解析XML为Map
        Map<String, String> params = new TreeMap<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<(\\w+)>(.*?)</\\1>");
        java.util.regex.Matcher matcher = pattern.matcher(xml);
        while (matcher.find()) {
            String key = matcher.group(1);
            if (!"sign".equals(key)) { // 排除sign字段
                String value = matcher.group(2);
                // 去除CDATA标记
                if (value.startsWith("<![CDATA[") && value.endsWith("]]>")) {
                    value = value.substring(9, value.length() - 3);
                }
                if (StringUtils.hasText(value)) {
                    params.put(key, value);
                }
            }
        }
        
        // 构建签名字符串
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        if (sb.length() > 0) {
            sb.append("key="); // 最后加上key=
        }
        return sb.toString();
    }
    
    /**
     * 计算微信支付签名（MD5）
     */
    private String calculateWechatSign(String signContent, String mchKey) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            String content = signContent + mchKey;
            byte[] bytes = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().toUpperCase();
        } catch (Exception e) {
            log.error("计算微信支付签名失败", e);
            return "";
        }
    }
    
    /**
     * 验证支付宝支付回调签名
     * 
     * @param callbackData 回调数据（Map格式）
     * @param configJson 支付配置JSON（可选）
     * @return 是否验证通过
     */
    public boolean verifyAlipayCallback(Map<String, String> callbackData, String configJson) {
        try {
            log.info("验证支付宝支付回调签名：订单号={}", callbackData.get("out_trade_no"));
            
            // 解析配置
            PaymentProperties.AlipayConfig config = parseAlipayConfig(configJson);
            
            // 获取签名
            String sign = callbackData.get("sign");
            if (!StringUtils.hasText(sign)) {
                log.error("支付宝回调数据中缺少签名");
                return false;
            }
            
            // 获取签名类型
            String signType = callbackData.getOrDefault("sign_type", config.getSignType());
            
            // 移除签名和签名类型，构建待签名字符串
            TreeMap<String, String> sortedParams = new TreeMap<>(callbackData);
            sortedParams.remove("sign");
            sortedParams.remove("sign_type");
            
            StringBuilder content = new StringBuilder();
            for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
                if (StringUtils.hasText(entry.getValue())) {
                    content.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }
            }
            if (content.length() > 0) {
                content.deleteCharAt(content.length() - 1); // 移除最后一个&
            }
            
            // 使用支付宝SDK验证签名
            boolean verified = com.alipay.api.internal.util.AlipaySignature.rsaCheckV1(
                callbackData,
                config.getAlipayPublicKey(),
                config.getCharset(),
                signType
            );
            
            if (verified) {
                log.info("支付宝支付回调签名验证通过");
            } else {
                log.error("支付宝支付回调签名验证失败");
            }
            
            return verified;
            
        } catch (Exception e) {
            log.error("验证支付宝支付回调签名失败", e);
            return false;
        }
    }
    
    /**
     * 解析微信支付配置
     * 仅从数据库configJson解析，不使用配置文件兜底
     * @throws BusinessException 如果配置不存在或不完整
     */
    private PaymentProperties.WechatPayConfig parseWechatConfig(String configJson) {
        if (!StringUtils.hasText(configJson)) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFIG_ERROR.getCode(), "微信支付未配置，请在后台管理系统中进行配置");
        }

        PaymentProperties.WechatPayConfig config = new PaymentProperties.WechatPayConfig();
        
        try {
            JsonNode jsonNode = objectMapper.readTree(configJson);
            
            // 必填字段校验
            if (!jsonNode.has("appId") || !StringUtils.hasText(jsonNode.get("appId").asText())) {
                throw new BusinessException(ErrorCode.PAYMENT_CONFIG_ERROR.getCode(), "微信支付配置错误：缺少AppID");
            }
            if (!jsonNode.has("mchId") || !StringUtils.hasText(jsonNode.get("mchId").asText())) {
                throw new BusinessException(ErrorCode.PAYMENT_CONFIG_ERROR.getCode(), "微信支付配置错误：缺少商户号");
            }
            // 回调地址必填校验
            if (!jsonNode.has("notifyUrl") || !StringUtils.hasText(jsonNode.get("notifyUrl").asText())) {
                throw new BusinessException(ErrorCode.PAYMENT_CONFIG_ERROR.getCode(), "微信支付配置错误：缺少回调地址");
            }

            config.setAppId(jsonNode.get("appId").asText());
            config.setMchId(jsonNode.get("mchId").asText());
            
            // 选填字段
            if (jsonNode.has("mchKey")) config.setMchKey(jsonNode.get("mchKey").asText());
            
            // 敏感字段解密
            if (jsonNode.has("apiV3Key")) config.setApiV3Key(tryDecrypt(jsonNode.get("apiV3Key").asText()));
            if (jsonNode.has("certSerialNo")) config.setCertSerialNo(jsonNode.get("certSerialNo").asText());
            if (jsonNode.has("privateKey")) config.setPrivateKey(tryDecrypt(jsonNode.get("privateKey").asText()));
            if (jsonNode.has("certContent")) config.setCertContent(tryDecrypt(jsonNode.get("certContent").asText()));
            
            // 回调地址
            config.setNotifyUrl(jsonNode.get("notifyUrl").asText());
            
            // 其他配置
            if (jsonNode.has("certPath")) config.setCertPath(jsonNode.get("certPath").asText());
            if (jsonNode.has("useSandbox")) config.setUseSandbox(jsonNode.get("useSandbox").asBoolean());
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析微信支付配置失败", e);
            throw new BusinessException(ErrorCode.PAYMENT_CONFIG_ERROR.getCode(), "解析微信支付配置失败：" + e.getMessage());
        }
        
        return config;
    }
    
    /**
     * 解析支付宝支付配置
     * 仅从数据库configJson解析，不使用配置文件兜底
     * @throws BusinessException 如果配置不存在或不完整
     */
    private PaymentProperties.AlipayConfig parseAlipayConfig(String configJson) {
        if (!StringUtils.hasText(configJson)) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFIG_ERROR.getCode(), "支付宝支付未配置，请在后台管理系统中进行配置");
        }

        PaymentProperties.AlipayConfig config = new PaymentProperties.AlipayConfig();
        
        try {
            JsonNode jsonNode = objectMapper.readTree(configJson);
            
            // 必填字段校验
            if (!jsonNode.has("appId") || !StringUtils.hasText(jsonNode.get("appId").asText())) {
                throw new BusinessException(ErrorCode.PAYMENT_CONFIG_ERROR.getCode(), "支付宝配置错误：缺少AppID");
            }
            // 回调地址必填校验
            if (!jsonNode.has("notifyUrl") || !StringUtils.hasText(jsonNode.get("notifyUrl").asText())) {
                throw new BusinessException(ErrorCode.PAYMENT_CONFIG_ERROR.getCode(), "支付宝配置错误：缺少回调地址");
            }
            
            config.setAppId(jsonNode.get("appId").asText());
            
            // 敏感字段解密
            if (jsonNode.has("privateKey")) config.setPrivateKey(tryDecrypt(jsonNode.get("privateKey").asText()));
            if (jsonNode.has("alipayPublicKey")) config.setAlipayPublicKey(tryDecrypt(jsonNode.get("alipayPublicKey").asText()));
            if (jsonNode.has("appCertContent")) config.setAppCertContent(tryDecrypt(jsonNode.get("appCertContent").asText()));
            if (jsonNode.has("alipayRootCertContent")) config.setAlipayRootCertContent(tryDecrypt(jsonNode.get("alipayRootCertContent").asText()));
            if (jsonNode.has("alipayCertContent")) config.setAlipayCertContent(tryDecrypt(jsonNode.get("alipayCertContent").asText()));
            
            // 回调地址
            config.setNotifyUrl(jsonNode.get("notifyUrl").asText());
            
            // 其他配置
            if (jsonNode.has("returnUrl")) config.setReturnUrl(jsonNode.get("returnUrl").asText());
            // 网关地址默认为正式环境，如果配置中有则覆盖
            config.setGatewayUrl(jsonNode.has("gatewayUrl") && StringUtils.hasText(jsonNode.get("gatewayUrl").asText()) 
                ? jsonNode.get("gatewayUrl").asText() 
                : "https://openapi.alipay.com/gateway.do");
                
            if (jsonNode.has("signType")) config.setSignType(jsonNode.get("signType").asText());
            if (jsonNode.has("charset")) config.setCharset(jsonNode.get("charset").asText());
            if (jsonNode.has("format")) config.setFormat(jsonNode.get("format").asText());
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析支付宝支付配置失败", e);
            throw new BusinessException(ErrorCode.PAYMENT_CONFIG_ERROR.getCode(), "解析支付宝支付配置失败：" + e.getMessage());
        }
        
        return config;
    }

    /**
     * 尝试解密敏感信息
     * 如果解密失败（可能本来就是明文），则返回原值
     */
    private String tryDecrypt(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        try {
            // 简单判断是否可能是加密字符串（通常AES加密后Base64编码的字符串不包含空格，长度是4的倍数等，但这里直接试着解密最稳妥）
            String decrypted = AesEncryptUtil.decrypt(value);
            return StringUtils.hasText(decrypted) ? decrypted : value;
        } catch (Exception e) {
            // 解密失败，可能是明文
            return value;
        }
    }
}

