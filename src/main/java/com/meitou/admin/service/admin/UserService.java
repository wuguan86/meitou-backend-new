package com.meitou.admin.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meitou.admin.entity.User;
import com.meitou.admin.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 管理端用户服务类
 * 处理用户相关的业务逻辑
 */
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {
    
    private final UserMapper userMapper; // 用户Mapper
    private final BCryptPasswordEncoder passwordEncoder; // 密码编码器（通过依赖注入）
    
    /**
     * 获取用户列表（支持站点ID和搜索）
     * 管理后台需要查看所有站点的用户，所以不使用多租户过滤
     * 
     * @param siteId 站点ID（可选）
     * @param search 搜索关键词
     * @return 用户列表
     */
    public List<User> getUsers(Long siteId, String search) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (siteId != null) {
            wrapper.eq(User::getSiteId, siteId);
        }
        if (StringUtils.hasText(search)) {
            wrapper.and(w -> w.like(User::getUsername, search)
                    .or().like(User::getEmail, search)
                    .or().like(User::getPhone, search));
        }
        wrapper.orderByDesc(User::getCreatedAt);
        return userMapper.selectList(wrapper);
    }
    
    /**
     * 根据ID获取用户
     * 
     * @param id 用户ID
     * @return 用户
     */
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }
    
    /**
     * 创建用户
     * 
     * @param user 用户信息
     * @return 创建的用户
     */
    public User createUser(User user) {
        // 检查邮箱是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, user.getEmail());
        wrapper.eq(User::getDeleted, 0);
        User existing = userMapper.selectOne(wrapper);
        if (existing != null) {
            throw new RuntimeException("邮箱已存在");
        }
        
        // 加密密码
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        // 设置默认值
        if (user.getBalance() == null) {
            user.setBalance(0);
        }
        if (user.getStatus() == null) {
            user.setStatus("active");
        }
        
        userMapper.insert(user);
        return user;
    }
    
    /**
     * 更新用户
     * 
     * @param id 用户ID
     * @param user 用户信息
     * @return 更新后的用户
     */
    public User updateUser(Long id, User user) {
        User existing = getUserById(id);
        
        // 更新字段
        if (user.getEmail() != null) {
            // 检查邮箱是否被其他用户使用
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getEmail, user.getEmail());
            wrapper.ne(User::getId, id);
            wrapper.eq(User::getDeleted, 0);
            User other = userMapper.selectOne(wrapper);
            if (other != null) {
                throw new RuntimeException("邮箱已被其他用户使用");
            }
            existing.setEmail(user.getEmail());
        }
        if (user.getUsername() != null) {
            existing.setUsername(user.getUsername());
        }
        if (user.getPhone() != null) {
            existing.setPhone(user.getPhone());
        }
        if (user.getWechat() != null) {
            existing.setWechat(user.getWechat());
        }
        if (user.getCompany() != null) {
            existing.setCompany(user.getCompany());
        }
        if (user.getRole() != null) {
            existing.setRole(user.getRole());
        }
        if (user.getStatus() != null) {
            existing.setStatus(user.getStatus());
        }
        if (user.getSiteId() != null) {
            existing.setSiteId(user.getSiteId());
        }
        // 密码更新（如果提供）
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        userMapper.updateById(existing);
        return existing;
    }
    
    /**
     * 删除用户（逻辑删除）
     * 
     * @param id 用户ID
     */
    public void deleteUser(Long id) {
        getUserById(id); // 检查用户是否存在
        userMapper.deleteById(id);
    }
    
    /**
     * 赠送积分
     * 
     * @param id 用户ID
     * @param points 积分数量
     * @return 更新后的用户
     */
    public User giftPoints(Long id, Integer points) {
        User user = getUserById(id);
        user.setBalance(user.getBalance() + points);
        userMapper.updateById(user);
        return user;
    }
}

