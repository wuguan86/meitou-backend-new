package com.meitou.admin.service.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meitou.admin.entity.Like;
import com.meitou.admin.entity.PublishedContent;
import com.meitou.admin.mapper.LikeMapper;
import com.meitou.admin.mapper.PublishedContentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 点赞服务类
 * 处理点赞相关业务逻辑
 */
@Service
@RequiredArgsConstructor
public class LikeService extends ServiceImpl<LikeMapper, Like> {
    
    private final LikeMapper likeMapper;
    private final PublishedContentMapper contentMapper;
    
    /**
     * 切换点赞状态（点赞/取消点赞）
     * 
     * @param userId 用户ID
     * @param contentId 发布内容ID
     * @return 是否已点赞（true-已点赞，false-已取消）
     */
    @Transactional
    public boolean toggleLike(Long userId, Long contentId) {
        // 验证发布内容是否存在
        PublishedContent content = contentMapper.selectById(contentId);
        if (content == null) {
            throw new RuntimeException("发布内容不存在");
        }
        
        // 查询是否已点赞
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId);
        wrapper.eq(Like::getContentId, contentId);
        Like existingLike = likeMapper.selectOne(wrapper);
        
        if (existingLike != null) {
            // 已点赞，取消点赞
            likeMapper.deleteById(existingLike.getId());
            // 更新点赞数
            content.setLikeCount(Math.max(0, content.getLikeCount() - 1));
            contentMapper.updateById(content);
            return false;
        } else {
            // 未点赞，添加点赞
            Like like = new Like();
            like.setUserId(userId);
            like.setContentId(contentId);
            likeMapper.insert(like);
            // 更新点赞数
            content.setLikeCount(content.getLikeCount() + 1);
            contentMapper.updateById(content);
            return true;
        }
    }
    
    /**
     * 检查用户是否已点赞
     * 
     * @param userId 用户ID
     * @param contentId 发布内容ID
     * @return 是否已点赞
     */
    public boolean isLiked(Long userId, Long contentId) {
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId);
        wrapper.eq(Like::getContentId, contentId);
        return likeMapper.selectCount(wrapper) > 0;
    }
}
