package com.meitou.admin.controller.app;

import com.meitou.admin.common.Result;
import com.meitou.admin.common.SiteContext;
import com.meitou.admin.entity.Site;
import com.meitou.admin.service.SiteCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端站点控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/app/site")
@RequiredArgsConstructor
public class SiteAppController {

    private final SiteCacheService siteCacheService;

    /**
     * 获取当前站点信息
     * 
     * @return 站点信息
     */
    @GetMapping("/current")
    public Result<Site> getCurrentSite() {
        log.info("收到获取当前站点信息的请求");
        Long siteId = SiteContext.getSiteId();
        if (siteId == null) {
            return Result.error("无法识别当前站点");
        }
        
        Site site = siteCacheService.getSiteById(siteId);
        if (site == null) {
            return Result.error("站点不存在");
        }
        
        return Result.success("获取成功", site);
    }
}
