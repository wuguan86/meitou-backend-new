package com.meitou.admin.service.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meitou.admin.common.Constants;
import com.meitou.admin.common.SiteContext;
import com.meitou.admin.dto.app.CodeLoginRequest;
import com.meitou.admin.dto.app.UserLoginResponse;
import com.meitou.admin.entity.InvitationCode;
import com.meitou.admin.entity.User;
import com.meitou.admin.mapper.InvitationCodeMapper;
import com.meitou.admin.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/**
 * 用户端认证服务
 * 处理用户验证码登录、注册等业务逻辑
 */
@Service
@RequiredArgsConstructor
public class AuthAppService {
    
    private final UserMapper userMapper;
    private final InvitationCodeMapper invitationCodeMapper;
    private final SmsCodeService smsCodeService;
    
    /**
     * 验证码登录
     * 如果用户不存在，则自动注册；如果存在，则直接登录
     * 
     * @param request 登录请求
     * @return 登录响应
     */
    @Transactional
    public UserLoginResponse loginByCode(CodeLoginRequest request) {
        // 验证验证码
        if (!smsCodeService.verifyCode(request.getPhone(), request.getCode())) {
            throw new RuntimeException("验证码错误或已过期");
        }
        
        // 查询用户是否存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, request.getPhone());
        wrapper.eq(User::getDeleted, 0);
        User user = userMapper.selectOne(wrapper);
        
        // 如果用户不存在，自动注册
        if (user == null) {
            user = createNewUser(request.getPhone(), request.getInvitationCode());
        } else {
            // 检查用户状态
            if (!Constants.USER_STATUS_ACTIVE.equals(user.getStatus())) {
                throw new RuntimeException("账号已被停用");
            }
        }
        
        // 生成Token（实际项目中应使用JWT）
        String token = generateToken(user.getId());
        
        // 构建响应
        UserLoginResponse response = new UserLoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setBalance(user.getBalance() != null ? user.getBalance() : 0);
        response.setSiteId(user.getSiteId()); // 设置站点ID
        
        return response;
    }
    
    /**
     * 创建新用户
     * 
     * @param phone 手机号
     * @param invitationCode 邀请码（可选）
     * @return 新创建的用户
     */
    private User createNewUser(String phone, String invitationCode) {
        // 从SiteContext获取当前站点ID
        Long siteId = SiteContext.getSiteId();
        if (siteId == null) {
            throw new RuntimeException("无法识别站点，请检查请求头或域名配置");
        }
        
        User user = new User();
        user.setPhone(phone);
        user.setUsername("用户_" + phone.substring(7)); // 默认用户名为手机号后4位
        // 邮箱设置为手机号+@meitou.com的格式（因为数据库要求NOT NULL且UNIQUE）
        user.setEmail(phone + "@meitou.com");
        // 密码设置为默认值（数据库要求NOT NULL，验证码登录无需密码）
        // 使用一个默认的占位符，实际验证码登录不需要验证密码
        user.setPassword("NO_PASSWORD_CODE_LOGIN");
        user.setRole("user"); // 默认角色
        user.setBalance(0); // 默认积分为0
        user.setStatus(Constants.USER_STATUS_ACTIVE); // 默认状态为正常
        user.setSiteId(siteId); // 设置站点ID
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(0);
        
        // 如果有邀请码，处理邀请码逻辑
        if (invitationCode != null && !invitationCode.trim().isEmpty()) {
            handleInvitationCode(invitationCode, user);
        }
        
        // 插入用户
        userMapper.insert(user);
        
        return user;
    }
    
    /**
     * 处理邀请码逻辑
     * 
     * @param code 邀请码
     * @param user 新用户
     */
    private void handleInvitationCode(String code, User user) {
        // 查询邀请码
        LambdaQueryWrapper<InvitationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvitationCode::getCode, code);
        wrapper.eq(InvitationCode::getStatus, "active"); // 状态为激活
        InvitationCode invitationCode = invitationCodeMapper.selectOne(wrapper);
        
        if (invitationCode != null) {
            // 增加邀请码使用次数
            invitationCode.setUsedCount(invitationCode.getUsedCount() + 1);
            invitationCodeMapper.updateById(invitationCode);
            
            // 可以根据邀请码配置给新用户赠送积分等
            // 这里可以根据业务需求扩展
        }
    }
    
    /**
     * 获取当前用户信息
     * 
     * @param userId 用户ID
     * @return 用户登录响应
     */
    public UserLoginResponse getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }
        
        // 检查用户状态
        if (!Constants.USER_STATUS_ACTIVE.equals(user.getStatus())) {
            throw new RuntimeException("账号已被停用");
        }
        
        // 构建响应
        UserLoginResponse response = new UserLoginResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setBalance(user.getBalance() != null ? user.getBalance() : 0);
        response.setSiteId(user.getSiteId()); // 设置站点ID
        
        return response;
    }
    
    /**
     * 生成Token（实际项目中应使用JWT）
     * 
     * @param userId 用户ID
     * @return Token
     */
    private String generateToken(Long userId) {
        // 简单实现：实际项目中应使用JWT生成Token
        return "app_token_" + userId + "_" + System.currentTimeMillis();
    }
}

