package com.simulab.modules.dashboard.controller;

import com.simulab.common.api.ApiResponse;
import com.simulab.modules.dashboard.dto.DashboardQueryDto;
import com.simulab.modules.dashboard.service.DashboardService;
import com.simulab.modules.dashboard.vo.DashboardOverviewVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewVo> overview(@ModelAttribute DashboardQueryDto queryDto) {
        return ApiResponse.success(dashboardService.getOverview(queryDto));
    }
}
