package com.meitou.admin.service.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meitou.admin.common.SiteContext;
import com.meitou.admin.entity.PublishedContent;
import com.meitou.admin.entity.User;
import com.meitou.admin.mapper.PublishedContentMapper;
import com.meitou.admin.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 发布内容服务类
 * 处理发布内容的发布、查询、管理等业务逻辑
 */
@Service
@RequiredArgsConstructor
public class PublishedContentService extends ServiceImpl<PublishedContentMapper, PublishedContent> {
    
    private final PublishedContentMapper contentMapper;
    private final UserMapper userMapper;
    
    /**
     * 发布内容
     * 
     * @param userId 用户ID
     * @param title 标题
     * @param description 描述
     * @param contentUrl 内容URL
     * @param thumbnail 缩略图URL
     * @param type 类型：image-图片，video-视频
     * @param generationType 生成类型：txt2img/img2img/txt2video/img2video
     * @param generationConfig 生成配置参数（JSON字符串）
     * @return 发布的内容
     */
    @Transactional
    public PublishedContent publishContent(Long userId, String title, String description,
                                           String contentUrl, String thumbnail,
                                           String type, String generationType, String generationConfig) {
        // 从SiteContext获取当前站点ID
        Long siteId = SiteContext.getSiteId();
        if (siteId == null) {
            throw new RuntimeException("无法识别站点，请检查请求头或域名配置");
        }
        
        // 查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 创建发布内容对象
        PublishedContent content = new PublishedContent();
        content.setUserId(userId);
        content.setUserName(user.getUsername());
        // 使用用户头像，如果没有则使用默认头像（基于用户ID生成）
        content.setUserAvatarUrl(user.getAvatarUrl() != null ? user.getAvatarUrl() : 
                "https://api.dicebear.com/7.x/avataaars/svg?seed=" + userId);
        content.setTitle(title);
        content.setDescription(description);
        content.setContentUrl(contentUrl);
        
        // 处理缩略图逻辑：如果是视频且缩略图为空或与内容URL相同，尝试自动生成（针对阿里云OSS）
        if ("video".equals(type) && (thumbnail == null || thumbnail.isEmpty() || thumbnail.equals(contentUrl))) {
            if (contentUrl != null && contentUrl.contains("aliyuncs.com")) {
                content.setThumbnail(contentUrl + "?x-oss-process=video/snapshot,t_1000,f_jpg,w_800,h_0,m_fast");
            } else {
                content.setThumbnail(thumbnail);
            }
        } else {
            content.setThumbnail(thumbnail);
        }
        
        content.setType(type);
        content.setGenerationType(generationType);
        content.setGenerationConfig(generationConfig);
        content.setStatus("published"); // 默认状态为展示中
        content.setIsPinned(false); // 默认不置顶
        content.setLikeCount(0); // 默认点赞数为0
        content.setPublishedAt(LocalDateTime.now()); // 设置发布时间
        content.setSiteId(siteId);
        
        // 保存到数据库
        contentMapper.insert(content);
        
        return content;
    }
    
    /**
     * 获取发布内容列表（支持类型筛选）
     * 
     * @param type 类型筛选（可选：image、video，如果为null或"all"则返回所有类型）
     * @return 发布内容列表（按置顶优先、发布时间倒序排列）
     */
    public List<PublishedContent> getPublishedContents(String type) {
        LambdaQueryWrapper<PublishedContent> wrapper = new LambdaQueryWrapper<>();
        
        // 只查询已发布的内容
        wrapper.eq(PublishedContent::getStatus, "published");
        
        // 筛选类型
        if (StringUtils.hasText(type) && !"all".equals(type)) {
            wrapper.eq(PublishedContent::getType, type);
        }
        
        // 按置顶优先、发布时间倒序排列
        wrapper.orderByDesc(PublishedContent::getIsPinned);
        wrapper.orderByDesc(PublishedContent::getPublishedAt);
        
        return contentMapper.selectList(wrapper);
    }
    
    /**
     * 根据ID获取发布内容详情
     * 
     * @param id 发布内容ID
     * @return 发布内容
     */
    public PublishedContent getPublishedContentById(Long id) {
        PublishedContent content = contentMapper.selectById(id);
        if (content == null) {
            throw new RuntimeException("发布内容不存在");
        }
        // 只返回已发布的内容
        if (!"published".equals(content.getStatus())) {
            throw new RuntimeException("发布内容不存在或已下架");
        }
        return content;
    }
    
    /**
     * 切换状态（上架/下架）
     * 
     * @param contentId 发布内容ID
     * @return 更新后的发布内容
     */
    @Transactional
    public PublishedContent toggleStatus(Long contentId) {
        PublishedContent content = contentMapper.selectById(contentId);
        if (content == null) {
            throw new RuntimeException("发布内容不存在");
        }
        
        // 切换状态
        if ("published".equals(content.getStatus())) {
            content.setStatus("hidden");
        } else {
            content.setStatus("published");
        }
        
        contentMapper.updateById(content);
        return content;
    }
    
    /**
     * 切换置顶状态
     * 
     * @param contentId 发布内容ID
     * @return 更新后的发布内容
     */
    @Transactional
    public PublishedContent togglePin(Long contentId) {
        PublishedContent content = contentMapper.selectById(contentId);
        if (content == null) {
            throw new RuntimeException("发布内容不存在");
        }
        
        // 切换置顶状态
        content.setIsPinned(!content.getIsPinned());
        
        contentMapper.updateById(content);
        return content;
    }
    
    /**
     * 删除发布内容（逻辑删除，仅限发布者）
     * 
     * @param userId 用户ID
     * @param contentId 发布内容ID
     */
    @Transactional
    public void deleteContent(Long userId, Long contentId) {
        PublishedContent content = contentMapper.selectById(contentId);
        if (content == null) {
            throw new RuntimeException("发布内容不存在");
        }
        if (!content.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此发布内容");
        }
        
        // 逻辑删除
        contentMapper.deleteById(contentId);
    }
}
