package com.meitou.admin.controller.admin;

import com.meitou.admin.annotation.SiteScope;
import com.meitou.admin.common.Result;
import com.meitou.admin.entity.InvitationCode;
import com.meitou.admin.service.admin.InvitationCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 管理端邀请码管理控制器
 */
@RestController
@RequestMapping("/api/admin/invitations")
@RequiredArgsConstructor
public class InvitationController {
    
    private final InvitationCodeService codeService;
    
    /**
     * 获取邀请码列表（按站点ID）
     * 
     * @param siteId 站点ID（可选，如果不提供则返回所有站点的邀请码）
     * @return 邀请码列表
     */
    @GetMapping
    @SiteScope(required = false) // 使用 AOP 自动处理 SiteContext，siteId 不是必填
    public Result<List<InvitationCode>> getCodes(@RequestParam(required = false) Long siteId) {
        List<InvitationCode> codes;
        if (siteId != null) {
            // SiteContext 已由 @SiteScope 注解自动设置
            codes = codeService.getCodesBySiteId(siteId);
        } else {
            // 如果没有指定 siteId，清除 SiteContext，让多租户插件不添加过滤条件
            // 但 invitation_codes 表需要多租户过滤，所以应该总是指定 siteId
            codes = codeService.getCodes();
        }
        return Result.success(codes);
    }
    
    /**
     * 生成邀请码
     * 
     * @param request 生成请求
     * @return 生成的邀请码列表
     */
    @PostMapping("/generate")
    public Result<List<InvitationCode>> generateCodes(@RequestBody Map<String, Object> request) {
        Integer count = (Integer) request.get("count");
        Integer points = (Integer) request.get("points");
        Integer maxUses = (Integer) request.get("maxUses");
        Long siteId = request.get("siteId") != null ? Long.valueOf(request.get("siteId").toString()) : null;
        String channel = (String) request.get("channel");
        String validStartDateStr = (String) request.get("validStartDate");
        String validEndDateStr = (String) request.get("validEndDate");
        
        LocalDate validStartDate = validStartDateStr != null ? LocalDate.parse(validStartDateStr) : null;
        LocalDate validEndDate = validEndDateStr != null ? LocalDate.parse(validEndDateStr) : null;
        
        if (siteId == null) {
            return Result.error("站点ID不能为空");
        }
        
        List<InvitationCode> codes = codeService.generateCodes(
                count, points, maxUses, siteId, channel, validStartDate, validEndDate
        );
        return Result.success("生成成功", codes);
    }
    
    /**
     * 更新邀请码
     * 
     * @param id 邀请码ID
     * @param code 邀请码信息
     * @return 更新后的邀请码
     */
    @PutMapping("/{id}")
    public Result<InvitationCode> updateCode(@PathVariable Long id, @RequestBody InvitationCode code) {
        InvitationCode updated = codeService.updateCode(id, code);
        return Result.success("更新成功", updated);
    }
}

