package com.simulab.modules.overlayanalysis.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulab.common.exception.BusinessException;
import com.simulab.common.security.SecurityUser;
import com.simulab.modules.measurement.mapper.MeasurementRunMapper;
import com.simulab.modules.overlayanalysis.dto.OverlayQueryDto;
import com.simulab.modules.overlayanalysis.dto.TrendQueryDto;
import com.simulab.modules.overlayanalysis.dto.WaferHeatmapBatchQueryDto;
import com.simulab.modules.overlayanalysis.dto.WaferHeatmapQueryDto;
import com.simulab.modules.overlayanalysis.entity.OverlayMeasurementPoint;
import com.simulab.modules.overlayanalysis.entity.SimulationResultSummary;
import com.simulab.modules.overlayanalysis.mapper.OverlayMeasurementPointMapper;
import com.simulab.modules.overlayanalysis.mapper.SimulationResultSummaryMapper;
import com.simulab.modules.overlayanalysis.vo.OverlayHistogramBinVo;
import com.simulab.modules.overlayanalysis.vo.OverlayScatterPointVo;
import com.simulab.modules.overlayanalysis.vo.OverlayTrendPointVo;
import com.simulab.modules.overlayanalysis.vo.WaferHeatmapPointVo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class OverlayAnalysisServiceImplTest {

    @Mock
    private OverlayMeasurementPointMapper overlayMeasurementPointMapper;
    @Mock
    private SimulationResultSummaryMapper simulationResultSummaryMapper;
    @Mock
    private MeasurementRunMapper measurementRunMapper;

    @BeforeEach
    void setupAuth() {
        SecurityUser securityUser = SecurityUser.builder().userId(1001L).username("u1").build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities()));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void buildWaferHeatmapShouldMapRequestedMetric() {
        OverlayAnalysisServiceImpl service = new OverlayAnalysisServiceImpl(
            overlayMeasurementPointMapper, simulationResultSummaryMapper);
        OverlayMeasurementPoint point = new OverlayMeasurementPoint();
        point.setTargetCode("P01");
        point.setXCoord(new BigDecimal("10"));
        point.setYCoord(new BigDecimal("20"));
        point.setOverlayY(new BigDecimal("3.200000"));
        point.setConfidence(new BigDecimal("0.98"));
        point.setIsOutlier(0);
        when(overlayMeasurementPointMapper.selectList(any())).thenReturn(List.of(point));

        WaferHeatmapQueryDto query = new WaferHeatmapQueryDto();
        query.setMetricCode("overlay_y");
        List<WaferHeatmapPointVo> result = service.buildWaferHeatmap(query);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("3.200000"), result.get(0).getMetricValue());
    }

    @Test
    void buildHistogramShouldReturnEightBins() {
        OverlayAnalysisServiceImpl service = new OverlayAnalysisServiceImpl(
            overlayMeasurementPointMapper, simulationResultSummaryMapper);
        OverlayMeasurementPoint p1 = new OverlayMeasurementPoint();
        p1.setOverlayMagnitude(new BigDecimal("2.0"));
        OverlayMeasurementPoint p2 = new OverlayMeasurementPoint();
        p2.setOverlayMagnitude(new BigDecimal("4.0"));
        OverlayMeasurementPoint p3 = new OverlayMeasurementPoint();
        p3.setOverlayMagnitude(new BigDecimal("6.0"));
        when(overlayMeasurementPointMapper.selectList(any())).thenReturn(List.of(p1, p2, p3));

        List<OverlayHistogramBinVo> bins = service.buildHistogram(new OverlayQueryDto());

        assertEquals(8, bins.size());
        assertEquals(3L, bins.stream().mapToLong(OverlayHistogramBinVo::getCount).sum());
    }

    @Test
    void buildScatterShouldReturnPointCloud() {
        OverlayAnalysisServiceImpl service = new OverlayAnalysisServiceImpl(
            overlayMeasurementPointMapper, simulationResultSummaryMapper);
        OverlayMeasurementPoint p1 = new OverlayMeasurementPoint();
        p1.setTargetCode("P001");
        p1.setOverlayX(new BigDecimal("1.250000"));
        p1.setOverlayY(new BigDecimal("-0.420000"));
        p1.setOverlayMagnitude(new BigDecimal("1.319"));
        p1.setIsOutlier(0);
        OverlayMeasurementPoint p2 = new OverlayMeasurementPoint();
        p2.setTargetCode("P002");
        p2.setOverlayX(new BigDecimal("-0.810000"));
        p2.setOverlayY(new BigDecimal("0.530000"));
        p2.setOverlayMagnitude(new BigDecimal("0.968"));
        p2.setIsOutlier(1);
        when(overlayMeasurementPointMapper.selectList(any())).thenReturn(List.of(p1, p2));

        List<OverlayScatterPointVo> points = service.buildScatter(new OverlayQueryDto());

        assertEquals(2, points.size());
        assertEquals(new BigDecimal("1.2500"), points.get(0).getXValue());
        assertEquals(new BigDecimal("-0.4200"), points.get(0).getYValue());
        assertEquals(new BigDecimal("0.9680"), points.get(1).getOverlayMagnitude());
    }

    @Test
    void buildTrendsShouldReturnRunLevelKpiPoints() {
        OverlayAnalysisServiceImpl service = new OverlayAnalysisServiceImpl(
            overlayMeasurementPointMapper, simulationResultSummaryMapper);
        SimulationResultSummary row1 = new SimulationResultSummary();
        row1.setCreatedAt(LocalDateTime.of(2026, 4, 1, 8, 0));
        row1.setPassRate(new BigDecimal("0.90"));
        row1.setMeanOverlay(new BigDecimal("4.2"));
        row1.setP95Overlay(new BigDecimal("6.1"));
        SimulationResultSummary row2 = new SimulationResultSummary();
        row2.setCreatedAt(LocalDateTime.of(2026, 4, 1, 9, 0));
        row2.setPassRate(new BigDecimal("0.80"));
        row2.setMeanOverlay(new BigDecimal("4.8"));
        row2.setP95Overlay(new BigDecimal("6.9"));
        when(simulationResultSummaryMapper.selectList(any())).thenReturn(List.of(row1, row2));

        List<OverlayTrendPointVo> result = service.buildTrends(new TrendQueryDto());

        assertEquals(2, result.size());
        assertEquals("Run-1", result.get(0).getLabel());
        assertEquals(new BigDecimal("0.90"), result.get(0).getPassRate());
        assertEquals("Run-2", result.get(1).getLabel());
        assertEquals(new BigDecimal("0.80"), result.get(1).getPassRate());
    }

    @Test
    void buildWaferHeatmapShouldUseRedisCacheOnSecondCall() {
        StringRedisTemplate redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = org.mockito.Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Map<String, String> cache = new ConcurrentHashMap<>();
        when(valueOperations.get(anyString())).thenAnswer(invocation -> cache.get(invocation.getArgument(0, String.class)));
        doAnswer(invocation -> {
            cache.put(invocation.getArgument(0, String.class), invocation.getArgument(1, String.class));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(java.time.Duration.class));
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(java.time.Duration.class))).thenReturn(true);
        when(valueOperations.getBit(anyString(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(true);

        OverlayAnalysisServiceImpl service = new OverlayAnalysisServiceImpl(
            overlayMeasurementPointMapper, simulationResultSummaryMapper, redisTemplate, new ObjectMapper());
        OverlayMeasurementPoint point = new OverlayMeasurementPoint();
        point.setTargetCode("P01");
        point.setXCoord(new BigDecimal("10"));
        point.setYCoord(new BigDecimal("20"));
        point.setOverlayMagnitude(new BigDecimal("3.200000"));
        when(overlayMeasurementPointMapper.selectList(any())).thenReturn(List.of(point));

        WaferHeatmapQueryDto query = new WaferHeatmapQueryDto();
        query.setMeasurementRunId(6001L);
        query.setMetricCode("overlay_magnitude");

        long coldStarted = System.nanoTime();
        List<WaferHeatmapPointVo> first = service.buildWaferHeatmap(query);
        long coldElapsedMs = (System.nanoTime() - coldStarted) / 1_000_000;

        long warmStarted = System.nanoTime();
        List<WaferHeatmapPointVo> second = service.buildWaferHeatmap(query);
        long warmElapsedMs = (System.nanoTime() - warmStarted) / 1_000_000;

        assertEquals(first.size(), second.size());
        org.junit.jupiter.api.Assertions.assertTrue(first.size() >= 11000 && first.size() <= 13000);
        verify(overlayMeasurementPointMapper, times(1)).selectList(any());
        verify(valueOperations, org.mockito.Mockito.atLeast(2)).get(anyString());
        verify(valueOperations, times(1)).set(anyString(), anyString(), any(java.time.Duration.class));
        verify(simulationResultSummaryMapper, never()).selectList(any());

        System.out.println("[perf][overlay-heatmap] coldMs=" + coldElapsedMs + ", warmMs=" + warmElapsedMs);
    }

    @Test
    void buildWaferHeatmapShouldSkipDbWhenBloomMiss() {
        StringRedisTemplate redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = org.mockito.Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getBit(anyString(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(false);
        OverlayAnalysisServiceImpl service = new OverlayAnalysisServiceImpl(
            overlayMeasurementPointMapper, simulationResultSummaryMapper, redisTemplate, new ObjectMapper());
        WaferHeatmapQueryDto query = new WaferHeatmapQueryDto();
        query.setMeasurementRunId(999999L);

        List<WaferHeatmapPointVo> result = service.buildWaferHeatmap(query);

        assertEquals(0, result.size());
        verify(overlayMeasurementPointMapper, never()).selectList(any());
    }

    @Test
    void buildWaferHeatmapBatchShouldReturnGroupedItems() {
        OverlayAnalysisServiceImpl service = new OverlayAnalysisServiceImpl(
            overlayMeasurementPointMapper, simulationResultSummaryMapper);
        OverlayMeasurementPoint point = new OverlayMeasurementPoint();
        point.setTargetCode("P01");
        point.setXCoord(new BigDecimal("10"));
        point.setYCoord(new BigDecimal("20"));
        point.setOverlayMagnitude(new BigDecimal("2.200000"));
        when(overlayMeasurementPointMapper.selectList(any())).thenReturn(List.of(point));

        WaferHeatmapBatchQueryDto query = new WaferHeatmapBatchQueryDto();
        query.setMetricCode("overlay_magnitude");
        query.setMeasurementRunIds(List.of(7001L, 7002L));

        var result = service.buildWaferHeatmapBatch(query);

        assertEquals(2, result.size());
        org.junit.jupiter.api.Assertions.assertTrue(Boolean.TRUE.equals(result.get(0).getSuccess()));
        org.junit.jupiter.api.Assertions.assertTrue(result.stream()
            .allMatch(item -> item.getPoints() != null && item.getPoints().size() >= 11000 && item.getPoints().size() <= 13000));
    }

    @Test
    void buildWaferHeatmapBatchShouldRejectWhenRunCountExceedsFour() {
        OverlayAnalysisServiceImpl service = new OverlayAnalysisServiceImpl(
            overlayMeasurementPointMapper, simulationResultSummaryMapper);
        WaferHeatmapBatchQueryDto query = new WaferHeatmapBatchQueryDto();
        query.setMeasurementRunIds(List.of(1L, 2L, 3L, 4L, 5L));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.buildWaferHeatmapBatch(query));

        assertEquals("OVERLAY_BATCH_LIMIT_EXCEEDED", ex.getCode());
    }

    @Test
    void startHeatmapBatchAsyncTaskShouldBeDisabled() {
        OverlayAnalysisServiceImpl service = new OverlayAnalysisServiceImpl(
            overlayMeasurementPointMapper, simulationResultSummaryMapper);
        WaferHeatmapBatchQueryDto query = new WaferHeatmapBatchQueryDto();
        query.setMeasurementRunIds(List.of(1L, 2L, 3L, 4L));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.startHeatmapBatchAsyncTask(query));

        assertEquals("OVERLAY_BATCH_ASYNC_DISABLED", ex.getCode());
    }
}
