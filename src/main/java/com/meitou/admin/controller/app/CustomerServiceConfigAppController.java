package com.meitou.admin.controller.app;

import com.meitou.admin.annotation.SiteScope;
import com.meitou.admin.common.Result;
import com.meitou.admin.entity.CustomerServiceConfig;
import com.meitou.admin.service.admin.CustomerServiceConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/app/customer-service")
@RequiredArgsConstructor
public class CustomerServiceConfigAppController {

    private final CustomerServiceConfigService configService;

    @GetMapping("/config")
    @SiteScope
    public Result<CustomerServiceConfig> getConfig(@RequestParam(required = true) Long siteId) {
        return Result.success(configService.getConfigBySiteId(siteId));
    }
}
