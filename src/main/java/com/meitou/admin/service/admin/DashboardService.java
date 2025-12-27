package com.meitou.admin.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meitou.admin.entity.User;
import com.meitou.admin.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理端数据概览服务类
 */
@Service
@RequiredArgsConstructor
public class DashboardService {
    
    private final UserMapper userMapper;
    
    /**
     * 获取统计数据
     * 
     * @param siteId 站点ID（可选）
     * @return 统计数据
     */
    public Map<String, Object> getStats(Long siteId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 查询用户总数
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        if (siteId != null) {
            userWrapper.eq(User::getSiteId, siteId);
        }
        long totalUsers = userMapper.selectCount(userWrapper);
        
        // 查询有消耗的用户数（这里简化处理，实际应该查询有生成记录的用户）
        long activeUsers = totalUsers; // 简化处理
        
        // 查询总消耗（这里简化处理，实际应该从生成记录表统计）
        int totalConsumption = 0; // 简化处理
        
        stats.put("totalBalance", 2458200); // 平台积分总余额（示例）
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("totalConsumption", totalConsumption);
        
        return stats;
    }
    
    /**
     * 获取趋势数据
     * 
     * @param siteId 站点ID（可选）
     * @return 趋势数据
     */
    public Map<String, Object> getTrend(Long siteId) {
        // 这里应该查询数据库获取真实的趋势数据
        // 简化处理，返回示例数据
        Map<String, Object> trend = new HashMap<>();
        // 实际应该从数据库查询每日数据
        return trend;
    }
    
    /**
     * 获取排名数据
     * 
     * @param siteId 站点ID（可选）
     * @return 排名数据
     */
    public Map<String, Object> getRanking(Long siteId) {
        // 这里应该查询数据库获取真实的排名数据
        // 简化处理，返回示例数据
        Map<String, Object> ranking = new HashMap<>();
        // 实际应该从数据库查询消耗排名
        return ranking;
    }
}

