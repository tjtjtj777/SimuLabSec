package com.simulab.modules.dashboard.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.simulab.modules.dashboard.dto.DashboardQueryDto;
import com.simulab.modules.dashboard.vo.DashboardOverviewVo;
import com.simulab.modules.lot.mapper.LotMapper;
import com.simulab.modules.overlayanalysis.entity.SimulationResultSummary;
import com.simulab.modules.overlayanalysis.mapper.SimulationResultSummaryMapper;
import com.simulab.modules.recipeversion.mapper.RecipeVersionMapper;
import com.simulab.modules.simulationtask.entity.SimulationTask;
import com.simulab.modules.simulationtask.mapper.SimulationTaskMapper;
import com.simulab.modules.wafer.mapper.WaferMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private LotMapper lotMapper;
    @Mock
    private WaferMapper waferMapper;
    @Mock
    private SimulationTaskMapper simulationTaskMapper;
    @Mock
    private RecipeVersionMapper recipeVersionMapper;
    @Mock
    private SimulationResultSummaryMapper simulationResultSummaryMapper;

    @Test
    void getOverviewShouldAggregateMetricsFromDatabaseRows() {
        DashboardServiceImpl service = new DashboardServiceImpl(
            lotMapper, waferMapper, simulationTaskMapper, recipeVersionMapper, simulationResultSummaryMapper);

        SimulationTask successTask = new SimulationTask();
        successTask.setStatus("SUCCESS");
        SimulationTask failedTask = new SimulationTask();
        failedTask.setStatus("FAILED");
        SimulationTask runningTask = new SimulationTask();
        runningTask.setStatus("RUNNING");

        SimulationResultSummary row1 = new SimulationResultSummary();
        row1.setMeanOverlay(new BigDecimal("4.000"));
        row1.setMaxOverlay(new BigDecimal("7.500"));
        row1.setPassRate(new BigDecimal("0.9000"));
        SimulationResultSummary row2 = new SimulationResultSummary();
        row2.setMeanOverlay(new BigDecimal("6.000"));
        row2.setMaxOverlay(new BigDecimal("8.000"));
        row2.setPassRate(new BigDecimal("0.8000"));

        when(lotMapper.selectCount(any())).thenReturn(2L);
        when(waferMapper.selectCount(any())).thenReturn(6L);
        when(simulationTaskMapper.selectList(any())).thenReturn(List.of(successTask, failedTask, runningTask));
        when(recipeVersionMapper.selectCount(any())).thenReturn(1L);
        when(simulationResultSummaryMapper.selectList(any())).thenReturn(List.of(row1, row2));

        DashboardOverviewVo vo = service.getOverview(new DashboardQueryDto());

        assertEquals(2L, vo.getTotalLots());
        assertEquals(6L, vo.getTotalWafers());
        assertEquals(1L, vo.getRunningTasks());
        assertEquals(new BigDecimal("50.00"), vo.getSuccessRate());
        assertEquals(new BigDecimal("85.00"), vo.getPassRate());
        assertEquals(new BigDecimal("5.000"), vo.getAvgOverlay());
        assertEquals(new BigDecimal("8.000"), vo.getMaxOverlay());
        assertEquals(1L, vo.getReleasedRecipeCount());
    }
}
