package com.simulab.modules.dashboard.service;

import com.simulab.modules.dashboard.dto.DashboardQueryDto;
import com.simulab.modules.dashboard.vo.DashboardOverviewVo;

public interface DashboardService {

    DashboardOverviewVo getOverview(DashboardQueryDto queryDto);
}
