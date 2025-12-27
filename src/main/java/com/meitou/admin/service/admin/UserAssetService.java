package com.meitou.admin.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meitou.admin.entity.UserAsset;
import com.meitou.admin.mapper.UserAssetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 管理端用户资产服务类
 */
@Service
@RequiredArgsConstructor
public class UserAssetService extends ServiceImpl<UserAssetMapper, UserAsset> {
    
    private final UserAssetMapper assetMapper;
    
    /**
     * 获取资产列表（支持站点ID、类型、搜索）
     * 管理后台需要查看所有站点的资产，所以不使用多租户过滤
     * 
     * @param siteId 站点ID（可选）
     * @param type 类型
     * @param search 搜索关键词
     * @return 资产列表
     */
    public List<UserAsset> getAssets(Long siteId, String type, String search) {
        LambdaQueryWrapper<UserAsset> wrapper = new LambdaQueryWrapper<>();
        
        if (siteId != null) {
            wrapper.eq(UserAsset::getSiteId, siteId);
        }
        if (StringUtils.hasText(type) && !"all".equals(type)) {
            wrapper.eq(UserAsset::getType, type);
        }
        if (StringUtils.hasText(search)) {
            wrapper.and(w -> w.like(UserAsset::getTitle, search)
                    .or().like(UserAsset::getUserName, search));
        }
        
        wrapper.orderByDesc(UserAsset::getIsPinned);
        wrapper.orderByDesc(UserAsset::getCreatedAt);
        
        return assetMapper.selectList(wrapper);
    }
    
    /**
     * 根据ID获取资产
     * 
     * @param id 资产ID
     * @return 资产
     */
    public UserAsset getAssetById(Long id) {
        UserAsset asset = assetMapper.selectById(id);
        if (asset == null) {
            throw new RuntimeException("资产不存在");
        }
        return asset;
    }
    
    /**
     * 更新资产
     * 
     * @param id 资产ID
     * @param asset 资产信息
     * @return 更新后的资产
     */
    public UserAsset updateAsset(Long id, UserAsset asset) {
        UserAsset existing = getAssetById(id);
        
        if (asset.getTitle() != null) {
            existing.setTitle(asset.getTitle());
        }
        if (asset.getStatus() != null) {
            existing.setStatus(asset.getStatus());
        }
        
        assetMapper.updateById(existing);
        return existing;
    }
    
    /**
     * 删除资产（逻辑删除）
     * 
     * @param id 资产ID
     */
    public void deleteAsset(Long id) {
        getAssetById(id);
        assetMapper.deleteById(id);
    }
    
    /**
     * 置顶/取消置顶
     * 
     * @param id 资产ID
     * @return 更新后的资产
     */
    public UserAsset togglePin(Long id) {
        UserAsset asset = getAssetById(id);
        asset.setIsPinned(!asset.getIsPinned());
        assetMapper.updateById(asset);
        return asset;
    }
    
    /**
     * 更新状态（上架/下架）
     * 
     * @param id 资产ID
     * @param status 状态
     * @return 更新后的资产
     */
    public UserAsset updateStatus(Long id, String status) {
        UserAsset asset = getAssetById(id);
        asset.setStatus(status);
        assetMapper.updateById(asset);
        return asset;
    }
    
    /**
     * 更新点赞数
     * 
     * @param id 资产ID
     * @param likeCount 点赞数
     * @return 更新后的资产
     */
    public UserAsset updateLikeCount(Long id, Integer likeCount) {
        UserAsset asset = getAssetById(id);
        asset.setLikeCount(likeCount);
        assetMapper.updateById(asset);
        return asset;
    }
}

