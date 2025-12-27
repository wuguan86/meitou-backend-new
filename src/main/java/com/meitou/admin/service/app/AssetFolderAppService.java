package com.meitou.admin.service.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meitou.admin.entity.AssetFolder;
import com.meitou.admin.mapper.AssetFolderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户端文件夹服务类
 * 处理文件夹的创建、查询、删除等业务逻辑
 */
@Service
@RequiredArgsConstructor
public class AssetFolderAppService extends ServiceImpl<AssetFolderMapper, AssetFolder> {
    
    private final AssetFolderMapper folderMapper;
    
    /**
     * 创建文件夹
     * 
     * @param userId 用户ID
     * @param name 文件夹名称
     * @param parentPath 父文件夹路径（可选，如果为null或空字符串，则表示根目录）
     * @return 创建的文件夹
     */
    @Transactional
    public AssetFolder createFolder(Long userId, String name, String parentPath) {
        // 验证文件夹名称
        if (!StringUtils.hasText(name)) {
            throw new RuntimeException("文件夹名称不能为空");
        }
        
        // 清理父路径（去除首尾空格和斜杠）
        String cleanedParentPath = null;
        if (StringUtils.hasText(parentPath)) {
            cleanedParentPath = parentPath.trim();
            if (cleanedParentPath.isEmpty()) {
                cleanedParentPath = null;
            }
        }
        
        // 构建完整路径
        String folderPath = cleanedParentPath != null ? 
            cleanedParentPath + "/" + name : name;
        
        // 检查文件夹是否已存在
        LambdaQueryWrapper<AssetFolder> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(AssetFolder::getUserId, userId);
        checkWrapper.eq(AssetFolder::getFolderPath, folderPath);
        checkWrapper.eq(AssetFolder::getDeleted, 0);
        AssetFolder existing = folderMapper.selectOne(checkWrapper);
        
        if (existing != null) {
            throw new RuntimeException("文件夹已存在");
        }
        
        // 创建文件夹对象
        AssetFolder folder = new AssetFolder();
        folder.setName(name);
        folder.setFolderPath(folderPath);
        folder.setParentPath(cleanedParentPath);
        folder.setUserId(userId);
        
        // 保存到数据库
        folderMapper.insert(folder);
        
        return folder;
    }
    
    /**
     * 获取用户的文件夹列表
     * 
     * @param userId 用户ID
     * @param parentPath 父文件夹路径（可选，如果为null或空字符串，则返回根目录下的文件夹）
     * @return 文件夹列表
     */
    public List<AssetFolder> getFolders(Long userId, String parentPath) {
        LambdaQueryWrapper<AssetFolder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetFolder::getUserId, userId);
        
        // 筛选父文件夹
        if (parentPath == null || parentPath.trim().isEmpty()) {
            wrapper.and(w -> w.isNull(AssetFolder::getParentPath).or().eq(AssetFolder::getParentPath, ""));
        } else {
            wrapper.eq(AssetFolder::getParentPath, parentPath.trim());
        }
        
        // 按名称排序
        wrapper.orderByAsc(AssetFolder::getName);
        
        return folderMapper.selectList(wrapper);
    }
    
    /**
     * 获取用户的所有文件夹列表（用于下拉选择等场景）
     * 
     * @param userId 用户ID
     * @return 所有文件夹列表
     */
    public List<AssetFolder> getAllFolders(Long userId) {
        LambdaQueryWrapper<AssetFolder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetFolder::getUserId, userId);
        wrapper.orderByAsc(AssetFolder::getFolderPath);
        return folderMapper.selectList(wrapper);
    }
    
    /**
     * 根据路径获取文件夹
     * 
     * @param userId 用户ID
     * @param folderPath 文件夹路径
     * @return 文件夹，如果不存在则返回null
     */
    public AssetFolder getFolderByPath(Long userId, String folderPath) {
        LambdaQueryWrapper<AssetFolder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetFolder::getUserId, userId);
        wrapper.eq(AssetFolder::getFolderPath, folderPath);
        wrapper.eq(AssetFolder::getDeleted, 0);
        return folderMapper.selectOne(wrapper);
    }
    
    /**
     * 更新文件夹名称
     * 
     * @param userId 用户ID
     * @param folderId 文件夹ID
     * @param newName 新名称
     * @return 更新后的文件夹
     */
    @Transactional
    public AssetFolder updateFolderName(Long userId, Long folderId, String newName) {
        // 验证文件夹名称
        if (!StringUtils.hasText(newName)) {
            throw new RuntimeException("文件夹名称不能为空");
        }
        
        // 查询文件夹并验证所有权
        AssetFolder folder = folderMapper.selectById(folderId);
        if (folder == null) {
            throw new RuntimeException("文件夹不存在");
        }
        if (!folder.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此文件夹");
        }
        
        // 构建新的完整路径
        String newFolderPath = folder.getParentPath() != null ? 
            folder.getParentPath() + "/" + newName : newName;
        
        // 检查新路径是否已存在
        LambdaQueryWrapper<AssetFolder> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(AssetFolder::getUserId, userId);
        checkWrapper.eq(AssetFolder::getFolderPath, newFolderPath);
        checkWrapper.ne(AssetFolder::getId, folderId);
        checkWrapper.eq(AssetFolder::getDeleted, 0);
        AssetFolder existing = folderMapper.selectOne(checkWrapper);
        
        if (existing != null) {
            throw new RuntimeException("文件夹名称已存在");
        }
        
        // 更新文件夹名称和路径
        folder.setName(newName);
        folder.setFolderPath(newFolderPath);
        
        folderMapper.updateById(folder);
        
        return folder;
    }
    
    /**
     * 删除文件夹（逻辑删除）
     * 
     * @param userId 用户ID
     * @param folderId 文件夹ID
     */
    @Transactional
    public void deleteFolder(Long userId, Long folderId) {
        // 查询文件夹并验证所有权
        AssetFolder folder = folderMapper.selectById(folderId);
        if (folder == null) {
            throw new RuntimeException("文件夹不存在");
        }
        if (!folder.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此文件夹");
        }
        
        // 逻辑删除
        folderMapper.deleteById(folderId);
    }
}
