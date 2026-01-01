package com.meitou.admin.service.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meitou.admin.common.SiteContext;
import com.meitou.admin.dto.app.*;
import com.meitou.admin.entity.PaymentConfig;
import com.meitou.admin.entity.RechargeOrder;
import com.meitou.admin.entity.User;
import com.meitou.admin.exception.BusinessException;
import com.meitou.admin.exception.ErrorCode;
import com.meitou.admin.mapper.PaymentConfigMapper;
import com.meitou.admin.mapper.RechargeOrderMapper;
import com.meitou.admin.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 充值服务
 * 处理充值订单相关的业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RechargeService {
    
    private final RechargeOrderMapper rechargeOrderMapper;
    private final UserMapper userMapper;
    private final PaymentConfigMapper paymentConfigMapper;
    private final RechargeConfigService rechargeConfigService;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 创建充值订单
     * 
     * @param userId 用户ID
     * @param request 创建订单请求
     * @return 订单响应
     */
    @Transactional
    public RechargeOrderResponse createOrder(Long userId, RechargeOrderRequest request) {
        // 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 获取充值配置（多租户插件会自动过滤当前站点）
        RechargeConfigResponse config = rechargeConfigService.getActiveConfig();
        
        // 验证金额（使用配置的最低金额）
        if (request.getAmount().compareTo(BigDecimal.valueOf(config.getMinAmount())) < 0) {
            throw new BusinessException(ErrorCode.RECHARGE_AMOUNT_INVALID.getCode(), "充值金额不能低于" + config.getMinAmount() + "元");
        }
        
        // 计算算力（根据配置的兑换比例）
        int points = request.getAmount().intValue() * config.getExchangeRate();
        
        // 生成唯一订单号
        String orderNo = generateOrderNo(userId);
        
        // 创建订单
        RechargeOrder order = new RechargeOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setAmount(request.getAmount());
        order.setPoints(points);
        order.setPaymentType(request.getPaymentType());
        order.setStatus("pending"); // 待支付
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setDeleted(0);
        
        rechargeOrderMapper.insert(order);
        
        // 获取支付配置
        PaymentConfig paymentConfig = getPaymentConfig(request.getPaymentType());
        if (paymentConfig == null || !paymentConfig.getIsEnabled()) {
            throw new BusinessException(ErrorCode.PAYMENT_METHOD_DISABLED);
        }
        
        // 调用支付接口获取支付参数
        Map<String, String> paymentParams;
        try {
            if ("wechat".equals(request.getPaymentType())) {
                paymentParams = paymentService.createWechatPayment(
                    orderNo,
                    request.getAmount().toString(),
                    "算力充值",
                    paymentConfig.getConfigJson()
                );
            } else if ("alipay".equals(request.getPaymentType())) {
                paymentParams = paymentService.createAlipayPayment(
                    orderNo,
                    request.getAmount().toString(),
                    "算力充值",
                    paymentConfig.getConfigJson()
                );
            } else {
                throw new BusinessException(ErrorCode.PAYMENT_METHOD_NOT_SUPPORTED);
            }
        } catch (Exception e) {
            log.error("创建支付订单失败", e);
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            }
            throw new BusinessException(ErrorCode.PAYMENT_ORDER_CREATE_FAILED.getCode(), "创建支付订单失败：" + e.getMessage());
        }
        
        // 更新订单状态为支付中
        order.setStatus("paying");
        order.setThirdPartyOrderNo(paymentParams.get("orderId"));
        rechargeOrderMapper.updateById(order);
        
        // 构建响应
        RechargeOrderResponse response = new RechargeOrderResponse();
        response.setOrderNo(orderNo);
        response.setAmount(request.getAmount());
        response.setPoints(points);
        response.setPaymentType(request.getPaymentType());
        response.setStatus(order.getStatus());
        try {
            response.setPaymentParams(objectMapper.writeValueAsString(paymentParams));
        } catch (Exception e) {
            log.error("序列化支付参数失败", e);
        }
        response.setCreatedAt(order.getCreatedAt());
        
        return response;
    }
    
    /**
     * 处理支付回调
     * 
     * @param paymentType 支付方式
     * @param callbackData 回调数据
     * @return 是否处理成功
     */
    @Transactional
    public boolean handlePaymentCallback(String paymentType, Map<String, String> callbackData) {
        // 保存原始站点ID（虽然回调通常没有上下文，但为了安全起见）
        Long originalSiteId = SiteContext.getSiteId();
        
        try {
            // 获取订单号
            String orderNo = callbackData.get("out_trade_no");
            if (orderNo == null) {
                log.error("支付回调缺少订单号");
                return false;
            }
            
            // 查询订单（使用忽略多租户过滤的方法，因为回调没有上下文）
            // 原来的 selectOne 会加上 site_id = 0 的条件，导致查不到订单
            RechargeOrder order = rechargeOrderMapper.selectByOrderNo(orderNo);
            
            if (order == null) {
                log.error("订单不存在：{}", orderNo);
                return false;
            }
            
            // 关键：设置当前线程的 SiteContext
            // 否则后续的 updateById 和用户余额更新会因为多租户过滤而失败
            if (order.getSiteId() != null) {
                SiteContext.setSiteId(order.getSiteId());
            }
            
            // 如果订单已经是已支付状态，直接返回成功（幂等处理）
            if ("paid".equals(order.getStatus())) {
                log.info("订单已支付，跳过处理：{}", orderNo);
                return true;
            }
            
            // 获取支付配置
            PaymentConfig paymentConfig = getPaymentConfig(paymentType);
            if (paymentConfig == null) {
                log.error("支付配置不存在：{}", paymentType);
                return false;
            }
            
            // 验证回调签名
            boolean verified = false;
            if ("wechat".equals(paymentType)) {
                // 微信支付回调需要将Map转换为XML字符串进行验证
                // 注意：这里简化处理，实际应该使用XML格式进行验证
                String callbackXml = convertMapToXml(callbackData);
                verified = paymentService.verifyWechatCallback(callbackXml, paymentConfig.getConfigJson());
            } else if ("alipay".equals(paymentType)) {
                verified = paymentService.verifyAlipayCallback(callbackData, paymentConfig.getConfigJson());
            }
            
            if (!verified) {
                log.error("支付回调签名验证失败：{}", orderNo);
                return false;
            }
            
            // 检查支付状态
            String paymentStatus = callbackData.get("trade_status") != null 
                ? callbackData.get("trade_status") 
                : callbackData.get("result_code");
            
            if (!"SUCCESS".equals(paymentStatus) && !"TRADE_SUCCESS".equals(paymentStatus)) {
                log.warn("支付未成功：订单号={}, 状态={}", orderNo, paymentStatus);
                order.setStatus("failed");
                rechargeOrderMapper.updateById(order);
                return false;
            }
            
            // 更新订单状态
            order.setStatus("paid");
            order.setThirdPartyOrderNo(callbackData.get("transaction_id") != null 
                ? callbackData.get("transaction_id") 
                : callbackData.get("trade_no"));
            order.setPaidAt(LocalDateTime.now());
            order.setCompletedAt(LocalDateTime.now());
            try {
                order.setCallbackInfo(objectMapper.writeValueAsString(callbackData));
            } catch (Exception e) {
                log.error("序列化回调信息失败", e);
            }
            rechargeOrderMapper.updateById(order);
            
            // 更新用户余额（原子操作）
            User user = userMapper.selectById(order.getUserId());
            if (user != null) {
                user.setBalance((user.getBalance() != null ? user.getBalance() : 0) + order.getPoints());
                userMapper.updateById(user);
                log.info("用户余额更新成功：用户ID={}, 增加算力={}, 当前余额={}", 
                    user.getId(), order.getPoints(), user.getBalance());
            }
            
            return true;
        } catch (Exception e) {
            log.error("处理支付回调失败", e);
            return false;
        } finally {
            // 恢复上下文
            if (originalSiteId != null) {
                SiteContext.setSiteId(originalSiteId);
            } else {
                SiteContext.clear();
            }
        }
    }
    
    /**
     * 查询订单
     * 
     * @param orderNo 订单号
     * @param userId 用户ID（用于验证订单归属）
     * @return 订单详情
     */
    public OrderQueryResponse queryOrder(String orderNo, Long userId) {
        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrder::getOrderNo, orderNo);
        wrapper.eq(RechargeOrder::getUserId, userId);
        wrapper.eq(RechargeOrder::getDeleted, 0);
        RechargeOrder order = rechargeOrderMapper.selectOne(wrapper);
        
        if (order == null) {
            throw new BusinessException(ErrorCode.RECORD_NOT_FOUND.getCode(), "订单不存在");
        }
        
        OrderQueryResponse response = new OrderQueryResponse();
        response.setOrderNo(order.getOrderNo());
        response.setAmount(order.getAmount());
        response.setPoints(order.getPoints());
        response.setPaymentType(order.getPaymentType());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setPaidAt(order.getPaidAt());
        response.setCompletedAt(order.getCompletedAt());
        
        return response;
    }
    
    /**
     * 取消订单
     * 
     * @param orderNo 订单号
     * @param userId 用户ID
     */
    @Transactional
    public void cancelOrder(String orderNo, Long userId) {
        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrder::getOrderNo, orderNo);
        wrapper.eq(RechargeOrder::getUserId, userId);
        wrapper.eq(RechargeOrder::getDeleted, 0);
        RechargeOrder order = rechargeOrderMapper.selectOne(wrapper);
        
        if (order == null) {
            throw new BusinessException(ErrorCode.RECORD_NOT_FOUND.getCode(), "订单不存在");
        }
        
        // 仅允许取消待支付订单
        if (!"pending".equals(order.getStatus()) && !"paying".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID.getCode(), "只能取消待支付或支付中的订单");
        }
        
        order.setStatus("cancelled");
        rechargeOrderMapper.updateById(order);
    }
    
    /**
     * 获取用户订单列表
     * 
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 订单列表
     */
    public Page<OrderQueryResponse> getUserOrders(Long userId, int page, int size) {
        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrder::getUserId, userId);
        wrapper.eq(RechargeOrder::getDeleted, 0);
        wrapper.orderByDesc(RechargeOrder::getCreatedAt);
        
        Page<RechargeOrder> orderPage = new Page<>(page, size);
        Page<RechargeOrder> result = rechargeOrderMapper.selectPage(orderPage, wrapper);
        
        // 转换为响应DTO
        Page<OrderQueryResponse> responsePage = new Page<>(page, size, result.getTotal());
        List<OrderQueryResponse> responseList = result.getRecords().stream().map(order -> {
            OrderQueryResponse response = new OrderQueryResponse();
            response.setOrderNo(order.getOrderNo());
            response.setAmount(order.getAmount());
            response.setPoints(order.getPoints());
            response.setPaymentType(order.getPaymentType());
            response.setStatus(order.getStatus());
            response.setCreatedAt(order.getCreatedAt());
            response.setPaidAt(order.getPaidAt());
            response.setCompletedAt(order.getCompletedAt());
            return response;
        }).toList();
        
        responsePage.setRecords(responseList);
        return responsePage;
    }
    
    /**
     * 生成唯一订单号
     * 格式：R{时间戳}{随机数}{用户ID后4位}
     * 
     * @param userId 用户ID
     * @return 订单号
     */
    private String generateOrderNo(Long userId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String userIdSuffix = String.format("%04d", userId % 10000);
        return "R" + timestamp + random + userIdSuffix;
    }
    
    /**
     * 获取支付配置
     * 
     * @param paymentType 支付方式
     * @return 支付配置
     */
    private PaymentConfig getPaymentConfig(String paymentType) {
        LambdaQueryWrapper<PaymentConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentConfig::getPaymentType, paymentType);
        wrapper.eq(PaymentConfig::getDeleted, 0);
        return paymentConfigMapper.selectOne(wrapper);
    }
    
    /**
     * 将Map转换为XML字符串（用于微信支付回调验证）
     * 
     * @param data Map数据
     * @return XML字符串
     */
    private String convertMapToXml(Map<String, String> data) {
        StringBuilder xml = new StringBuilder("<xml>");
        for (Map.Entry<String, String> entry : data.entrySet()) {
            xml.append("<").append(entry.getKey()).append(">");
            xml.append("<![CDATA[").append(entry.getValue()).append("]]>");
            xml.append("</").append(entry.getKey()).append(">");
        }
        xml.append("</xml>");
        return xml.toString();
    }
    
    /**
     * 处理微信支付回调（XML格式）
     * 
     * @param callbackXml 回调XML字符串
     * @return 是否处理成功
     */
    @Transactional
    public boolean handleWechatPaymentCallback(String callbackXml) {
        try {
            // 解析XML为Map
            Map<String, String> callbackData = parseXmlToMap(callbackXml);
            return handlePaymentCallback("wechat", callbackData);
        } catch (Exception e) {
            log.error("处理微信支付回调失败", e);
            return false;
        }
    }
    
    /**
     * 解析XML字符串为Map（简单实现，实际项目中应使用XML解析库）
     * 
     * @param xml XML字符串
     * @return Map数据
     */
    private Map<String, String> parseXmlToMap(String xml) {
        Map<String, String> result = new HashMap<>();
        // 简单实现：使用正则表达式提取键值对
        // 注意：这是一个简化实现，实际项目中应使用DOM或SAX解析器
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<(\\w+)><!\\[CDATA\\[(.*?)\\]\\]></\\1>");
        java.util.regex.Matcher matcher = pattern.matcher(xml);
        while (matcher.find()) {
            result.put(matcher.group(1), matcher.group(2));
        }
        // 如果没有CDATA，尝试普通格式
        if (result.isEmpty()) {
            pattern = java.util.regex.Pattern.compile("<(\\w+)>(.*?)</\\1>");
            matcher = pattern.matcher(xml);
            while (matcher.find()) {
                result.put(matcher.group(1), matcher.group(2));
            }
        }
        return result;
    }
}

