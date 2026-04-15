package com.simulab.modules.dashboard.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardOverviewVo {

    private Long totalLots;
    private Long totalWafers;
    private Long runningTasks;
    private BigDecimal successRate;
    private BigDecimal passRate;
    private BigDecimal avgOverlay;
    private BigDecimal maxOverlay;
    private Long releasedRecipeCount;
}
