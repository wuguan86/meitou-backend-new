package com.meitou.admin.dto.app;

import lombok.Data;
import java.util.List;

/**
 * 充值配置响应 DTO
 */
@Data
public class RechargeConfigResponse {
    
    /**
     * 兑换比例（1元 = X算力）
     */
    private Integer exchangeRate;
    
    /**
     * 最低充值金额（元）
     */
    private Integer minAmount;
    
    /**
     * 充值选项列表
     */
    private List<RechargeOption> options;
    
    /**
     * 是否启用自定义金额
     */
    private Boolean allowCustom;

    /**
     * 启用的支付方式列表
     */
    private List<String> enabledPaymentMethods;
    
    /**
     * 充值选项内部类
     */
    @Data
    public static class RechargeOption {
        /**
         * 算力点数
         */
        private Integer points;
        
        /**
         * 价格（元）
         */
        private Integer price;
    }
}

