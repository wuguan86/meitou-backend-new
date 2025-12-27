package com.meitou.admin.controller.admin;

import com.meitou.admin.annotation.SiteScope;
import com.meitou.admin.common.Result;
import com.meitou.admin.entity.GenerationRecord;
import com.meitou.admin.service.admin.GenerationRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端生成记录控制器
 */
@RestController
@RequestMapping("/api/admin/generation-records")
@RequiredArgsConstructor
public class GenerationRecordController {
    
    private final GenerationRecordService recordService;
    
    /**
     * 获取生成记录列表（按站点ID）
     * 
     * @param siteId 站点ID（必传）：1=医美类，2=电商类，3=生活服务类
     * @return 记录列表
     */
    @GetMapping
    @SiteScope // 使用 AOP 自动处理 SiteContext
    public Result<List<GenerationRecord>> getRecords(@RequestParam(required = true) Long siteId) {
        // SiteContext 已由 @SiteScope 注解自动设置
        List<GenerationRecord> records = recordService.getRecordsBySiteId(siteId);
        return Result.success(records);
    }
    
    /**
     * 获取生成记录详情
     * 
     * @param id 记录ID
     * @return 记录信息
     */
    @GetMapping("/{id}")
    public Result<GenerationRecord> getRecord(@PathVariable Long id) {
        GenerationRecord record = recordService.getRecordById(id);
        return Result.success(record);
    }
    
    /**
     * 创建生成记录
     * 
     * @param siteId 站点ID（必传）：1=医美类，2=电商类，3=生活服务类
     * @param record 记录信息
     * @return 创建后的记录
     */
    @PostMapping
    @SiteScope // 使用 AOP 自动处理 SiteContext
    public Result<GenerationRecord> createRecord(
            @RequestParam(required = true) Long siteId,
            @RequestBody GenerationRecord record) {
        // SiteContext 已由 @SiteScope 注解自动设置
        // 设置站点ID
        record.setSiteId(siteId);
        
        GenerationRecord created = recordService.createRecord(record);
        return Result.success("创建成功", created);
    }
    
    /**
     * 更新生成记录
     * 
     * @param id 记录ID
     * @param siteId 站点ID（必传）：1=医美类，2=电商类，3=生活服务类
     * @param record 记录信息
     * @return 更新后的记录
     */
    @PutMapping("/{id}")
    @SiteScope // 使用 AOP 自动处理 SiteContext
    public Result<GenerationRecord> updateRecord(
            @PathVariable Long id,
            @RequestParam(required = true) Long siteId,
            @RequestBody GenerationRecord record) {
        // SiteContext 已由 @SiteScope 注解自动设置
        GenerationRecord updated = recordService.updateRecord(id, record, siteId);
        return Result.success("更新成功", updated);
    }
    
    /**
     * 删除生成记录
     * 
     * @param id 记录ID
     * @param siteId 站点ID（必传）：1=医美类，2=电商类，3=生活服务类
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @SiteScope // 使用 AOP 自动处理 SiteContext
    public Result<Void> deleteRecord(
            @PathVariable Long id,
            @RequestParam(required = true) Long siteId) {
        // SiteContext 已由 @SiteScope 注解自动设置
        recordService.deleteRecord(id, siteId);
        return Result.success("删除成功");
    }
}

