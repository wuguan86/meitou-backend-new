package com.meitou.admin.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meitou.admin.dto.LoginRequest;
import com.meitou.admin.dto.LoginResponse;
import com.meitou.admin.entity.BackendAccount;
import com.meitou.admin.mapper.BackendAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 管理端认证服务类
 * 处理后台管理登录、登出等认证相关业务
 */
@Service
@RequiredArgsConstructor
public class AuthAdminService {
    
    private final BackendAccountMapper accountMapper; // 账号Mapper
    private final BCryptPasswordEncoder passwordEncoder; // 密码编码器（通过依赖注入）
    
    /**
     * 登录
     * 
     * @param request 登录请求
     * @return 登录响应（包含Token）
     */
    public LoginResponse login(LoginRequest request) {
        // 查询账号
        LambdaQueryWrapper<BackendAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BackendAccount::getEmail, request.getAccount());
        wrapper.eq(BackendAccount::getDeleted, 0);
        BackendAccount account = accountMapper.selectOne(wrapper);
        
        if (account == null) {
            throw new RuntimeException("账号不存在");
        }
        
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        
        // 检查账号状态
        if (!"active".equals(account.getStatus())) {
            throw new RuntimeException("账号已被锁定");
        }
        
        // 更新最后登录时间
        account.setLastLogin(LocalDateTime.now());
        accountMapper.updateById(account);
        
        // 生成简单Token（实际项目中应使用JWT）
        String token = generateSimpleToken(account.getId());
        
        // 构建响应
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setEmail(account.getEmail());
        response.setRole(account.getRole());
        
        return response;
    }
    
    /**
     * 检查登录状态
     * 
     * @param token Token
     * @return 是否有效
     */
    public boolean checkToken(String token) {
        // 简单实现：实际项目中应验证JWT Token
        return token != null && !token.isEmpty();
    }
    
    /**
     * 生成简单Token（实际项目中应使用JWT）
     * 
     * @param accountId 账号ID
     * @return Token
     */
    private String generateSimpleToken(Long accountId) {
        // 简单实现：实际项目中应使用JWT生成Token
        return "token_" + accountId + "_" + System.currentTimeMillis();
    }
}

