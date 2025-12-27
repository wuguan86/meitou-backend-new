package com.meitou.admin.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meitou.admin.entity.PaymentConfig;
import com.meitou.admin.mapper.PaymentConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理端支付配置服务类
 */
@Service
@RequiredArgsConstructor
public class PaymentConfigService extends ServiceImpl<PaymentConfigMapper, PaymentConfig> {
    
    private final PaymentConfigMapper paymentConfigMapper;
    
    /**
     * 获取支付配置列表（按站点ID）
     * 
     * @param siteId 站点ID
     * @return 支付配置列表
     */
    public List<PaymentConfig> getPaymentConfigs(Long siteId) {
        LambdaQueryWrapper<PaymentConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentConfig::getSiteId, siteId);
        wrapper.eq(PaymentConfig::getDeleted, 0);
        wrapper.orderByAsc(PaymentConfig::getId);
        return paymentConfigMapper.selectList(wrapper);
    }
    
    /**
     * 根据支付类型和站点ID获取支付配置
     * 
     * @param paymentType 支付类型：wechat-微信支付，alipay-支付宝支付，bank_transfer-对公转账
     * @param siteId 站点ID
     * @return 支付配置
     */
    public PaymentConfig getPaymentConfigByType(String paymentType, Long siteId) {
        LambdaQueryWrapper<PaymentConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentConfig::getPaymentType, paymentType);
        wrapper.eq(PaymentConfig::getSiteId, siteId);
        wrapper.eq(PaymentConfig::getDeleted, 0);
        return paymentConfigMapper.selectOne(wrapper);
    }
    
    /**
     * 更新支付配置的启用状态
     * 
     * @param paymentType 支付类型
     * @param siteId 站点ID
     * @param isEnabled 是否启用
     * @return 更新后的支付配置
     */
    public PaymentConfig updatePaymentConfigStatus(String paymentType, Long siteId, Boolean isEnabled) {
        PaymentConfig config = getPaymentConfigByType(paymentType, siteId);
        if (config == null) {
            throw new RuntimeException("支付配置不存在：paymentType=" + paymentType + ", siteId=" + siteId);
        }
        config.setIsEnabled(isEnabled);
        paymentConfigMapper.updateById(config);
        return config;
    }
    
    /**
     * 创建或更新支付配置（如果不存在则创建，存在则更新）
     * 
     * @param paymentType 支付类型
     * @param siteId 站点ID
     * @param config 支付配置信息
     * @return 创建或更新后的支付配置
     */
    public PaymentConfig createOrUpdatePaymentConfig(String paymentType, Long siteId, PaymentConfig config) {
        PaymentConfig existing = getPaymentConfigByType(paymentType, siteId);
        if (existing == null) {
            // 创建新配置
            PaymentConfig newConfig = new PaymentConfig();
            newConfig.setPaymentType(paymentType);
            newConfig.setSiteId(siteId);
            newConfig.setConfigJson(config.getConfigJson());
            newConfig.setIsEnabled(config.getIsEnabled() != null ? config.getIsEnabled() : false);
            paymentConfigMapper.insert(newConfig);
            return newConfig;
        } else {
            // 更新现有配置
            if (config.getConfigJson() != null) {
                existing.setConfigJson(config.getConfigJson());
            }
            if (config.getIsEnabled() != null) {
                existing.setIsEnabled(config.getIsEnabled());
            }
            paymentConfigMapper.updateById(existing);
            return existing;
        }
    }
    
    /**
     * 更新支付配置
     * 
     * @param paymentType 支付类型
     * @param siteId 站点ID
     * @param config 支付配置信息
     * @return 更新后的支付配置
     */
    public PaymentConfig updatePaymentConfig(String paymentType, Long siteId, PaymentConfig config) {
        PaymentConfig existing = getPaymentConfigByType(paymentType, siteId);
        if (existing == null) {
            throw new RuntimeException("支付配置不存在：paymentType=" + paymentType + ", siteId=" + siteId);
        }
        
        if (config.getConfigJson() != null) {
            existing.setConfigJson(config.getConfigJson());
        }
        if (config.getIsEnabled() != null) {
            existing.setIsEnabled(config.getIsEnabled());
        }
        
        paymentConfigMapper.updateById(existing);
        return existing;
    }
    
    /**
     * 删除支付配置
     * 
     * @param paymentType 支付类型
     * @param siteId 站点ID
     */
    public void deletePaymentConfig(String paymentType, Long siteId) {
        PaymentConfig config = getPaymentConfigByType(paymentType, siteId);
        if (config == null) {
            throw new RuntimeException("支付配置不存在：paymentType=" + paymentType + ", siteId=" + siteId);
        }
        paymentConfigMapper.deleteById(config.getId());
    }
}

