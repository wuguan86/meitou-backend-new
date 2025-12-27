package com.meitou.admin.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meitou.admin.entity.GenerationRecord;
import com.meitou.admin.mapper.GenerationRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理端生成记录服务类
 */
@Service
@RequiredArgsConstructor
public class GenerationRecordService extends ServiceImpl<GenerationRecordMapper, GenerationRecord> {
    
    private final GenerationRecordMapper recordMapper;
    
    /**
     * 获取生成记录列表（按站点ID）
     * 注意：调用此方法前，需要先设置 SiteContext.setSiteId(siteId)，
     * 这样多租户插件会自动添加 site_id 过滤条件
     * 
     * @param siteId 站点ID
     * @return 记录列表
     */
    public List<GenerationRecord> getRecordsBySiteId(Long siteId) {
        // 不在这里添加 siteId 条件，因为多租户插件会自动添加
        // 如果在这里添加，会导致 SQL 中出现重复的 site_id 条件
        LambdaQueryWrapper<GenerationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(GenerationRecord::getCreatedAt);
        return recordMapper.selectList(wrapper);
    }
    
    /**
     * 根据ID获取记录
     * 
     * @param id 记录ID
     * @return 记录
     */
    public GenerationRecord getRecordById(Long id) {
        GenerationRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("记录不存在");
        }
        return record;
    }
    
    /**
     * 创建生成记录
     * 注意：调用此方法前，需要先设置 SiteContext.setSiteId(siteId)，
     * 这样多租户插件会自动处理 site_id 字段
     * 
     * @param record 记录信息
     * @return 创建后的记录
     */
    public GenerationRecord createRecord(GenerationRecord record) {
        recordMapper.insert(record);
        return record;
    }
    
    /**
     * 更新生成记录
     * 注意：调用此方法前，需要先设置 SiteContext.setSiteId(siteId)，
     * 这样多租户插件会自动添加 site_id 过滤条件
     * 
     * @param id 记录ID
     * @param record 记录信息
     * @param siteId 站点ID
     * @return 更新后的记录
     */
    public GenerationRecord updateRecord(Long id, GenerationRecord record, Long siteId) {
        // 先查询记录是否存在，并且属于指定的站点
        GenerationRecord existing = recordMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("记录不存在");
        }
        
        // 验证站点ID是否匹配
        if (!siteId.equals(existing.getSiteId())) {
            throw new RuntimeException("记录不属于指定站点");
        }
        
        // 设置ID和站点ID
        record.setId(id);
        record.setSiteId(siteId);
        
        // 更新记录
        recordMapper.updateById(record);
        return record;
    }
    
    /**
     * 删除生成记录（逻辑删除）
     * 注意：调用此方法前，需要先设置 SiteContext.setSiteId(siteId)，
     * 这样多租户插件会自动添加 site_id 过滤条件
     * 
     * @param id 记录ID
     * @param siteId 站点ID
     */
    public void deleteRecord(Long id, Long siteId) {
        // 先查询记录是否存在，并且属于指定的站点
        GenerationRecord existing = recordMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("记录不存在");
        }
        
        // 验证站点ID是否匹配
        if (!siteId.equals(existing.getSiteId())) {
            throw new RuntimeException("记录不属于指定站点");
        }
        
        // 逻辑删除（MyBatis-Plus会自动处理）
        recordMapper.deleteById(id);
    }
}

