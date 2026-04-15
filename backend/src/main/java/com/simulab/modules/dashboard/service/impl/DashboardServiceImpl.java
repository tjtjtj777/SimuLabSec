package com.simulab.modules.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.simulab.modules.dashboard.dto.DashboardQueryDto;
import com.simulab.modules.dashboard.service.DashboardService;
import com.simulab.modules.dashboard.vo.DashboardOverviewVo;
import com.simulab.modules.lot.entity.Lot;
import com.simulab.modules.lot.mapper.LotMapper;
import com.simulab.modules.overlayanalysis.entity.SimulationResultSummary;
import com.simulab.modules.overlayanalysis.mapper.SimulationResultSummaryMapper;
import com.simulab.modules.recipeversion.entity.RecipeVersion;
import com.simulab.modules.recipeversion.mapper.RecipeVersionMapper;
import com.simulab.modules.simulationtask.entity.SimulationTask;
import com.simulab.modules.simulationtask.mapper.SimulationTaskMapper;
import com.simulab.modules.wafer.entity.Wafer;
import com.simulab.modules.wafer.mapper.WaferMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final LotMapper lotMapper;
    private final WaferMapper waferMapper;
    private final SimulationTaskMapper simulationTaskMapper;
    private final RecipeVersionMapper recipeVersionMapper;
    private final SimulationResultSummaryMapper simulationResultSummaryMapper;

    public DashboardServiceImpl(
        LotMapper lotMapper,
        WaferMapper waferMapper,
        SimulationTaskMapper simulationTaskMapper,
        RecipeVersionMapper recipeVersionMapper,
        SimulationResultSummaryMapper simulationResultSummaryMapper
    ) {
        this.lotMapper = lotMapper;
        this.waferMapper = waferMapper;
        this.simulationTaskMapper = simulationTaskMapper;
        this.recipeVersionMapper = recipeVersionMapper;
        this.simulationResultSummaryMapper = simulationResultSummaryMapper;
    }

    @Override
    public DashboardOverviewVo getOverview(DashboardQueryDto queryDto) {
        long totalLots = lotMapper.selectCount(new LambdaQueryWrapper<Lot>().eq(Lot::getDeleted, 0));
        long totalWafers = waferMapper.selectCount(new LambdaQueryWrapper<Wafer>().eq(Wafer::getDeleted, 0));
        List<SimulationTask> tasks = simulationTaskMapper.selectList(new LambdaQueryWrapper<SimulationTask>()
            .eq(SimulationTask::getDeleted, 0)
            .eq(StringUtils.hasText(queryDto.getScenarioType()), SimulationTask::getScenarioType, queryDto.getScenarioType()));
        long runningTasks = tasks.stream().filter(task ->
            "RUNNING".equalsIgnoreCase(task.getStatus()) || "PENDING_DATA".equalsIgnoreCase(task.getStatus())
                || "QUEUED".equalsIgnoreCase(task.getStatus())).count();
        long finishedTasks = tasks.stream().filter(task ->
            "SUCCESS".equalsIgnoreCase(task.getStatus()) || "FAILED".equalsIgnoreCase(task.getStatus())
                || "CANCELLED".equalsIgnoreCase(task.getStatus())).count();
        long successTasks = tasks.stream().filter(task -> "SUCCESS".equalsIgnoreCase(task.getStatus())).count();
        BigDecimal successRate = finishedTasks == 0 ? BigDecimal.ZERO
            : BigDecimal.valueOf(successTasks)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(finishedTasks), 2, RoundingMode.HALF_UP);

        List<SimulationResultSummary> summaryRows = simulationResultSummaryMapper.selectList(
            new LambdaQueryWrapper<SimulationResultSummary>()
                .orderByDesc(SimulationResultSummary::getCreatedAt)
        );

        List<BigDecimal> meanOverlayList = summaryRows.stream()
            .map(SimulationResultSummary::getMeanOverlay)
            .filter(value -> value != null)
            .toList();
        BigDecimal avgOverlay = meanOverlayList.isEmpty() ? BigDecimal.ZERO : meanOverlayList.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(meanOverlayList.size()), 3, RoundingMode.HALF_UP);
        BigDecimal maxOverlay = summaryRows.stream()
            .map(SimulationResultSummary::getMaxOverlay)
            .filter(value -> value != null)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
        List<BigDecimal> passRateList = summaryRows.stream()
            .map(SimulationResultSummary::getPassRate)
            .filter(value -> value != null)
            .toList();
        BigDecimal passRate = passRateList.isEmpty() ? BigDecimal.ZERO : passRateList.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(passRateList.size()), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
        long releasedRecipeCount = recipeVersionMapper.selectCount(new LambdaQueryWrapper<RecipeVersion>()
            .eq(RecipeVersion::getDeleted, 0)
            .eq(RecipeVersion::getStatus, "RELEASED"));

        return DashboardOverviewVo.builder()
            .totalLots(totalLots)
            .totalWafers(totalWafers)
            .runningTasks(runningTasks)
            .successRate(successRate)
            .passRate(passRate)
            .avgOverlay(avgOverlay)
            .maxOverlay(maxOverlay)
            .releasedRecipeCount(releasedRecipeCount)
            .build();
    }
}
