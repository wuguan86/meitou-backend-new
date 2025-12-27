package com.meitou.admin.service.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meitou.admin.dto.app.RechargeConfigResponse;
import com.meitou.admin.entity.RechargeConfig;
import com.meitou.admin.mapper.RechargeConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 充值配置服务
 * 处理充值配置相关的业务逻辑
 */
@Service
@RequiredArgsConstructor
public class RechargeConfigService {
    
    private final RechargeConfigMapper rechargeConfigMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 获取当前站点的充值配置
     * 多租户插件会自动过滤当前站点的数据
     * 
     * @return 充值配置响应
     */
    public RechargeConfigResponse getActiveConfig() {
        // 查询当前站点的启用配置
        LambdaQueryWrapper<RechargeConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeConfig::getIsEnabled, true);
        wrapper.eq(RechargeConfig::getDeleted, 0);
        RechargeConfig config = rechargeConfigMapper.selectOne(wrapper);
        
        if (config == null) {
            throw new RuntimeException("未找到充值配置");
        }
        
        return convertToResponse(config);
    }
    
    /**
     * 将实体类转换为响应DTO
     * 
     * @param config 充值配置实体
     * @return 充值配置响应DTO
     */
    private RechargeConfigResponse convertToResponse(RechargeConfig config) {
        RechargeConfigResponse response = new RechargeConfigResponse();
        response.setExchangeRate(config.getExchangeRate());
        response.setMinAmount(config.getMinAmount());
        response.setAllowCustom(config.getAllowCustom());
        
        // 解析JSON选项列表
        try {
            if (config.getOptionsJson() != null && !config.getOptionsJson().trim().isEmpty()) {
                List<RechargeConfigResponse.RechargeOption> options = objectMapper.readValue(
                    config.getOptionsJson(),
                    new TypeReference<List<RechargeConfigResponse.RechargeOption>>() {}
                );
                response.setOptions(options);
            } else {
                response.setOptions(new ArrayList<>());
            }
        } catch (Exception e) {
            // 解析失败，使用空列表
            response.setOptions(new ArrayList<>());
        }
        
        return response;
    }
}

