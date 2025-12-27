package com.meitou.admin.controller.admin;

import com.meitou.admin.common.Result;
import com.meitou.admin.entity.UserAsset;
import com.meitou.admin.service.admin.UserAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端资产管理控制器
 */
@RestController
@RequestMapping("/api/admin/assets")
@RequiredArgsConstructor
public class AssetController {
    
    private final UserAssetService assetService;
    
    /**
     * 获取资产列表
     * 管理后台需要查看所有站点的资产，所以不使用多租户过滤
     * 
     * @param siteId 站点ID（可选）
     * @param type 类型（可选）
     * @param search 搜索关键词（可选）
     * @return 资产列表
     */
    @GetMapping
    public Result<List<UserAsset>> getAssets(
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search
    ) {
        List<UserAsset> assets = assetService.getAssets(siteId, type, search);
        return Result.success(assets);
    }
    
    /**
     * 获取资产详情
     * 
     * @param id 资产ID
     * @return 资产信息
     */
    @GetMapping("/{id}")
    public Result<UserAsset> getAsset(@PathVariable Long id) {
        UserAsset asset = assetService.getAssetById(id);
        return Result.success(asset);
    }
    
    /**
     * 更新资产
     * 
     * @param id 资产ID
     * @param asset 资产信息
     * @return 更新后的资产
     */
    @PutMapping("/{id}")
    public Result<UserAsset> updateAsset(@PathVariable Long id, @RequestBody UserAsset asset) {
        UserAsset updated = assetService.updateAsset(id, asset);
        return Result.success("更新成功", updated);
    }
    
    /**
     * 删除资产
     * 
     * @param id 资产ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteAsset(@PathVariable Long id) {
        assetService.deleteAsset(id);
        return Result.success("删除成功");
    }
    
    /**
     * 置顶/取消置顶
     * 
     * @param id 资产ID
     * @return 更新后的资产
     */
    @PutMapping("/{id}/pin")
    public Result<UserAsset> togglePin(@PathVariable Long id) {
        UserAsset asset = assetService.togglePin(id);
        return Result.success("操作成功", asset);
    }
    
    /**
     * 更新状态（上架/下架）
     * 
     * @param id 资产ID
     * @param status 状态
     * @return 更新后的资产
     */
    @PutMapping("/{id}/status")
    public Result<UserAsset> updateStatus(@PathVariable Long id, @RequestParam String status) {
        UserAsset asset = assetService.updateStatus(id, status);
        return Result.success("更新成功", asset);
    }
    
    /**
     * 更新点赞数
     * 
     * @param id 资产ID
     * @param likeCount 点赞数
     * @return 更新后的资产
     */
    @PutMapping("/{id}/like-count")
    public Result<UserAsset> updateLikeCount(@PathVariable Long id, @RequestParam Integer likeCount) {
        UserAsset asset = assetService.updateLikeCount(id, likeCount);
        return Result.success("更新成功", asset);
    }
}

