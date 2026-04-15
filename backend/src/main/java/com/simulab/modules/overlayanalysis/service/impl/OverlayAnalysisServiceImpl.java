package com.simulab.modules.overlayanalysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulab.config.AnalysisExecutionProperties;
import com.simulab.common.security.SecurityContextUtils;
import com.simulab.common.exception.BusinessException;
import com.simulab.modules.measurement.entity.MeasurementRun;
import com.simulab.modules.measurement.mapper.MeasurementRunMapper;
import com.simulab.modules.overlayanalysis.dto.OverlayQueryDto;
import com.simulab.modules.overlayanalysis.dto.TrendQueryDto;
import com.simulab.modules.overlayanalysis.dto.WaferHeatmapQueryDto;
import com.simulab.modules.overlayanalysis.dto.WaferHeatmapBatchQueryDto;
import com.simulab.modules.overlayanalysis.entity.OverlayMeasurementPoint;
import com.simulab.modules.overlayanalysis.entity.SimulationResultSummary;
import com.simulab.modules.overlayanalysis.mapper.OverlayMeasurementPointMapper;
import com.simulab.modules.overlayanalysis.mapper.SimulationResultSummaryMapper;
import com.simulab.modules.overlayanalysis.vo.OverlayHistogramBinVo;
import com.simulab.modules.overlayanalysis.vo.WaferHeatmapBatchItemVo;
import com.simulab.modules.overlayanalysis.vo.WaferHeatmapBatchTaskStartVo;
import com.simulab.modules.overlayanalysis.vo.OverlayScatterPointVo;
import com.simulab.modules.overlayanalysis.vo.OverlayTrendPointVo;
import com.simulab.modules.overlayanalysis.service.OverlayAnalysisService;
import com.simulab.modules.overlayanalysis.vo.WaferHeatmapPointVo;
import com.simulab.modules.simulationtask.entity.SimulationTask;
import com.simulab.modules.simulationtask.mapper.SimulationTaskMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import jakarta.annotation.PreDestroy;

@Service
public class OverlayAnalysisServiceImpl implements OverlayAnalysisService {
    /*
     * 这个 Service 负责把“已经存在的分析结果数据”组织成前端图表接口。
     *
     * 它和 WaferAnalysisConfigServiceImpl 的区别是：
     * - WaferAnalysisConfigServiceImpl：负责“生成数据”
     * - OverlayAnalysisServiceImpl：负责“消费已有数据并组装图表”
     *
     * 前端常见的 4 个图表接口都在这里：
     * 1. heatmap：直接返回热图点位
     * 2. scatter：返回适合散点图消费的抽样点
     * 3. histogram：返回分桶统计
     * 4. trends：返回 run 级 KPI 趋势
     *
     * 另外，本类也是 overlay 分析链路里最集中的性能优化入口。当前主要优化点包括：
     * - 布隆过滤器：提前挡掉明显不存在的 measurementRunId，防缓存穿透
     * - 空值缓存：挡住“布隆误判后仍为空”的重复空查
     * - Redis 最终结果缓存：缓存图表最终 VO 列表
     * - 互斥重建：热点 key 过期时避免缓存击穿
     * - 稀疏 run 密化：把老 run/导入 run 的稀疏点位补成高密度连续场
     * - 批量 heatmap + 并行密化：降低多 Wafer 页面和密化重计算成本
     */

    private static final Logger log = LoggerFactory.getLogger(OverlayAnalysisServiceImpl.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    // 稀疏 run 密化目标：按 0.5 步长在圆形 wafer 上补点，最终点位数接近高密度热图展示规模。
    private static final double DENSE_STEP = 0.5d;
    private static final int DENSE_MIN_POINTS = 180000;
    // 热图接口返回给前端的目标点位规模：约 1.2 万。
    // 这个规模可在“分布可读性”和“前端内存/渲染开销”之间取得平衡。
    private static final int HEATMAP_TARGET_POINTS = 12_000;
    // denseRunCache 只做短期复用，防止同一个 run 在短时间内被反复 densify。
    private static final long DENSE_CACHE_TTL_MS = 2 * 60 * 1000L;
    private static final int DENSE_CACHE_MAX_RUNS = 4;
    // 三类缓存前缀分别服务：
    // 1. CACHE_PREFIX：最终图表结果缓存
    // 2. LOCK_PREFIX：热点缓存重建锁
    // 3. NULL_CACHE_PREFIX：确认为空后的短 TTL 空值缓存
    private static final String CACHE_PREFIX = "simulab:overlay:";
    private static final String LOCK_PREFIX = "simulab:overlay:lock:";
    private static final String NULL_CACHE_PREFIX = "simulab:overlay:null:";
    // 布隆过滤器只服务于 measurement_run.id 的存在性预判，不负责权限判断。
    // 真正的 demo/mine 可见性，仍然由后面的 SQL where 条件保证。
    private static final String BLOOM_MEASUREMENT_RUN_KEY = "simulab:bloom:measurement-run:id";
    private static final int BLOOM_HASH_COUNT = 6;
    private static final int BLOOM_BITS = 1 << 24;
    private static final int MAX_BATCH_RUNS = 4;
    private static final int MAX_USER_TASKS = 50;
    // 热点缓存互斥重建使用短锁 + 短轮询，防击穿但不把等待线程无限挂死。
    private static final Duration LOCK_TTL = Duration.ofSeconds(8);
    private static final int LOCK_WAIT_ROUNDS = 8;
    private static final long LOCK_WAIT_MS = 60L;
    private final OverlayMeasurementPointMapper overlayMeasurementPointMapper;
    private final SimulationResultSummaryMapper simulationResultSummaryMapper;
    private final MeasurementRunMapper measurementRunMapper;
    private final SimulationTaskMapper simulationTaskMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AnalysisExecutionProperties analysisExecutionProperties;
    private final AtomicBoolean measurementRunBloomInitialized = new AtomicBoolean(false);
    private volatile long measurementRunBloomMaxId = 0L;
    private final ReentrantLock bloomInitLock = new ReentrantLock();
    private final ExecutorService overlayComputeExecutor;
    private final Map<Long, ReentrantLock> denseRunLocks = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Long, DensePointCacheEntry> denseRunCache = java.util.Collections.synchronizedMap(
        new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, DensePointCacheEntry> eldest) {
                return size() > DENSE_CACHE_MAX_RUNS;
            }
        }
    );
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
        "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
        Long.class
    );

    public OverlayAnalysisServiceImpl(
        OverlayMeasurementPointMapper overlayMeasurementPointMapper,
        SimulationResultSummaryMapper simulationResultSummaryMapper
    ) {
        // 轻量构造器，主要用于无需 Redis/MeasurementRunMapper 的测试场景。
        this(overlayMeasurementPointMapper, simulationResultSummaryMapper, null, null, null, null, defaultExecutionProperties());
    }

    @Autowired
    public OverlayAnalysisServiceImpl(
        OverlayMeasurementPointMapper overlayMeasurementPointMapper,
        SimulationResultSummaryMapper simulationResultSummaryMapper,
        StringRedisTemplate stringRedisTemplate,
        ObjectMapper objectMapper,
        MeasurementRunMapper measurementRunMapper,
        SimulationTaskMapper simulationTaskMapper,
        AnalysisExecutionProperties analysisExecutionProperties
    ) {
        // 主构造器中 3 个辅助依赖的用途：
        // - StringRedisTemplate：结果缓存 / 空值缓存 / bitmap bloom / 分布式锁
        // - ObjectMapper：缓存列表序列化与反序列化
        // - MeasurementRunMapper：布隆初始化与“run 是否存在且用户可见”的补充判断
        this.overlayMeasurementPointMapper = overlayMeasurementPointMapper;
        this.simulationResultSummaryMapper = simulationResultSummaryMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.measurementRunMapper = measurementRunMapper;
        this.simulationTaskMapper = simulationTaskMapper;
        this.analysisExecutionProperties = analysisExecutionProperties == null ? defaultExecutionProperties() : analysisExecutionProperties;
        this.overlayComputeExecutor = newFixedExecutor(
            "overlay-compute",
            this.analysisExecutionProperties.getOverlayExecutorPoolSize()
        );
    }

    public OverlayAnalysisServiceImpl(
        OverlayMeasurementPointMapper overlayMeasurementPointMapper,
        SimulationResultSummaryMapper simulationResultSummaryMapper,
        StringRedisTemplate stringRedisTemplate,
        ObjectMapper objectMapper
    ) {
        this(
            overlayMeasurementPointMapper,
            simulationResultSummaryMapper,
            stringRedisTemplate,
            objectMapper,
            null,
            null,
            defaultExecutionProperties()
        );
    }

    @PreDestroy
    public void shutdownExecutors() {
        overlayComputeExecutor.shutdownNow();
    }

    @Override
    public List<WaferHeatmapPointVo> buildWaferHeatmap(WaferHeatmapQueryDto queryDto) {
        // 单图热图入口。真正逻辑下沉到 internal 方法，方便批量热图直接复用。
        return buildWaferHeatmapInternal(queryDto, SecurityContextUtils.currentUserIdOrThrow());
    }

    @Override
    public List<WaferHeatmapBatchItemVo> buildWaferHeatmapBatch(WaferHeatmapBatchQueryDto queryDto) {
        // 批量热图接口服务于多 Wafer 页面。
        // 目标是把“前端 N 次单图请求”压缩为“后端 1 次批量请求 + 内部并发处理”。
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        return buildWaferHeatmapBatchInternal(queryDto, currentUserId);
    }

    @Override
    public WaferHeatmapBatchTaskStartVo startHeatmapBatchAsyncTask(WaferHeatmapBatchQueryDto queryDto) {
        // Multi Wafer 页面已收敛为最多 4 张图，同步 batch 能在可接受耗时内完成，不再走后台任务模式。
        throw new BusinessException("OVERLAY_BATCH_ASYNC_DISABLED", "Async batch mode is disabled. Batch supports up to 4 runs.");
    }

    private List<WaferHeatmapBatchItemVo> buildWaferHeatmapBatchInternal(WaferHeatmapBatchQueryDto queryDto, Long currentUserId) {
        if (queryDto == null || queryDto.getMeasurementRunIds() == null || queryDto.getMeasurementRunIds().isEmpty()) {
            return List.of();
        }
        List<Long> runIds = validateAndNormalizeBatchRunIds(queryDto);
        if (runIds.isEmpty()) {
            return List.of();
        }
        // 每个 run 独立提交任务，这样单 run 失败不会拖垮整个批量请求。
        List<Future<WaferHeatmapBatchItemVo>> futures = runIds.stream()
            .map(runId -> overlayComputeExecutor.submit(() -> {
                WaferHeatmapBatchItemVo vo = new WaferHeatmapBatchItemVo();
                vo.setMeasurementRunId(runId);
                try {
                    WaferHeatmapQueryDto single = new WaferHeatmapQueryDto();
                    single.setMeasurementRunId(runId);
                    single.setMetricCode(queryDto.getMetricCode());
                    vo.setPoints(buildWaferHeatmapInternal(single, currentUserId));
                    vo.setSuccess(true);
                } catch (Exception ex) {
                    vo.setSuccess(false);
                    vo.setError(ex.getMessage());
                    vo.setPoints(List.of());
                }
                return vo;
            }))
            .toList();
        List<WaferHeatmapBatchItemVo> result = new ArrayList<>(futures.size());
        for (Future<WaferHeatmapBatchItemVo> future : futures) {
            try {
                result.add(future.get());
            } catch (Exception ex) {
                WaferHeatmapBatchItemVo fallback = new WaferHeatmapBatchItemVo();
                fallback.setSuccess(false);
                fallback.setError(ex.getMessage());
                fallback.setPoints(List.of());
                result.add(fallback);
            }
        }
        log.info(
            "[overlay-analysis] heatmap-batch userId={} requestedRuns={} successRuns={} failedRuns={} finalPointCounts={}",
            currentUserId,
            runIds.size(),
            result.stream().filter(item -> Boolean.TRUE.equals(item.getSuccess())).count(),
            result.stream().filter(item -> !Boolean.TRUE.equals(item.getSuccess())).count(),
            result.stream()
                .filter(item -> item.getMeasurementRunId() != null)
                .map(item -> "runId=" + item.getMeasurementRunId() + ",points=" + (item.getPoints() == null ? 0 : item.getPoints().size()))
                .toList()
        );
        return result;
    }

    private List<Long> validateAndNormalizeBatchRunIds(WaferHeatmapBatchQueryDto queryDto) {
        if (queryDto == null || queryDto.getMeasurementRunIds() == null || queryDto.getMeasurementRunIds().isEmpty()) {
            return List.of();
        }
        if (queryDto.getMeasurementRunIds().size() > MAX_BATCH_RUNS) {
            throw new BusinessException("OVERLAY_BATCH_LIMIT_EXCEEDED", "Single batch request supports up to 4 runs.");
        }
        return queryDto.getMeasurementRunIds().stream().filter(Objects::nonNull).distinct().limit(MAX_BATCH_RUNS).toList();
    }

    private void executeAsyncHeatmapTask(Long taskId, Long currentUserId, WaferHeatmapBatchQueryDto queryDto) {
        try {
            List<WaferHeatmapBatchItemVo> rows = buildWaferHeatmapBatchInternal(queryDto, currentUserId);
            long success = rows.stream().filter(item -> Boolean.TRUE.equals(item.getSuccess())).count();
            long failed = rows.size() - success;
            SimulationTask update = new SimulationTask();
            update.setId(taskId);
            update.setStatus(failed == 0 ? "SUCCESS" : "FAILED");
            update.setResultSummaryJson("{\"requested\":" + rows.size() + ",\"success\":" + success + ",\"failed\":" + failed + "}");
            if (failed > 0) {
                update.setErrorMessage("Some heatmaps failed during async warmup.");
            }
            simulationTaskMapper.updateById(update);
            log.info("[overlay-analysis] async-heatmap taskId={} status={} requested={} success={} failed={}",
                taskId, update.getStatus(), rows.size(), success, failed);
        } catch (Exception ex) {
            SimulationTask failed = new SimulationTask();
            failed.setId(taskId);
            failed.setStatus("FAILED");
            failed.setErrorMessage(ex.getMessage());
            simulationTaskMapper.updateById(failed);
            log.warn("[overlay-analysis] async-heatmap taskId={} status=FAILED reason={}", taskId, ex.getMessage());
        }
    }

    private List<WaferHeatmapPointVo> buildWaferHeatmapInternal(WaferHeatmapQueryDto queryDto, Long currentUserId) {
        long startedAt = System.currentTimeMillis();
        // 第一层：穿透防护。先做 measurementRunId 布隆预判和空值缓存判断。
        // 只有“runId 可能存在且近期没有判空”时，才值得继续走最终结果缓存与数据库查询。
        if (shouldSkipByBloomAndNullCache("heatmap", currentUserId, queryDto.getMeasurementRunId(), queryDto.getMetricCode())) {
            return List.of();
        }
        String cacheKey = cacheKey("heatmap", currentUserId, queryDto.getWaferId(), queryDto.getLayerId(), queryDto.getMeasurementRunId(), queryDto.getMetricCode());
        List<WaferHeatmapPointVo> result = getOrComputeCached(
            "heatmap",
            cacheKey,
            WaferHeatmapPointVo.class,
            // supplier 代表“缓存 miss 后如何真正构建热图返回值”。
            // 这里先查点位，再把 entity 映射为前端直接可画图的 VO。
            () -> selectPoints("heatmap", currentUserId, queryDto.getWaferId(), queryDto.getLayerId(), queryDto.getMeasurementRunId(), queryDto.getMetricCode()).stream()
                .map(point -> {
                    WaferHeatmapPointVo vo = new WaferHeatmapPointVo();
                    vo.setTargetCode(point.getTargetCode());
                    vo.setXCoord(point.getXCoord());
                    vo.setYCoord(point.getYCoord());
                    vo.setMetricValue(resolveMetricValue(point, queryDto.getMetricCode()));
                    vo.setConfidence(point.getConfidence());
                    vo.setOutlier(point.getIsOutlier());
                    return vo;
                })
                .toList()
        );
        log.info(
            "[overlay-analysis] heatmap userId={} waferId={} layerId={} runId={} metricCode={} points={} elapsedMs={}",
            currentUserId, queryDto.getWaferId(), queryDto.getLayerId(), queryDto.getMeasurementRunId(), queryDto.getMetricCode(),
            result.size(), System.currentTimeMillis() - startedAt
        );
        return result;
    }

    @Override
    public List<OverlayScatterPointVo> buildScatter(OverlayQueryDto queryDto) {
        long startedAt = System.currentTimeMillis();
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        if (shouldSkipByBloomAndNullCache("scatter", currentUserId, queryDto.getMeasurementRunId(), queryDto.getMetricCode())) {
            return List.of();
        }
        String cacheKey = cacheKey("scatter", currentUserId, queryDto.getWaferId(), queryDto.getLayerId(), queryDto.getMeasurementRunId(), queryDto.getMetricCode());
        List<OverlayScatterPointVo> result = getOrComputeCached(
            "scatter",
            cacheKey,
            OverlayScatterPointVo.class,
            () -> {
                // 散点图和热图共用同一批点位来源，但展示意图不同：
                // - 热图关注 wafer 上的空间位置
                // - 散点图关注 overlayX / overlayY 的误差分布形态
                List<OverlayMeasurementPoint> points = selectPoints("scatter", currentUserId, queryDto.getWaferId(), queryDto.getLayerId(), queryDto.getMeasurementRunId(), queryDto.getMetricCode());
                if (points.isEmpty()) {
                    return List.of();
                }
                // 散点图不回传全部高密度点位，而是做限量抽样，平衡传输体积与图表可读性。
                // 当前散点图的横纵轴采用 overlayX / overlayY，overlayMagnitude 作为辅助 tooltip 数值。
                final int maxPoints = 6000;
                int step = Math.max(1, points.size() / maxPoints);
                List<OverlayScatterPointVo> rows = new ArrayList<>(Math.min(points.size(), maxPoints + 1));
                for (int i = 0; i < points.size(); i += step) {
                    OverlayMeasurementPoint point = points.get(i);
                    OverlayScatterPointVo vo = new OverlayScatterPointVo();
                    vo.setTargetCode(point.getTargetCode());
                    vo.setXValue(point.getOverlayX() == null ? BigDecimal.ZERO : point.getOverlayX().setScale(4, RoundingMode.HALF_UP));
                    vo.setYValue(point.getOverlayY() == null ? BigDecimal.ZERO : point.getOverlayY().setScale(4, RoundingMode.HALF_UP));
                    BigDecimal metricValue = resolveMetricValue(point, queryDto.getMetricCode());
                    vo.setOverlayMagnitude((metricValue == null ? BigDecimal.ZERO : metricValue).setScale(4, RoundingMode.HALF_UP));
                    vo.setOutlier(point.getIsOutlier() == null ? 0 : point.getIsOutlier());
                    rows.add(vo);
                }
                return rows;
            }
        );
        log.info(
            "[overlay-analysis] scatter userId={} waferId={} layerId={} runId={} metricCode={} points={} elapsedMs={} memoryUsedMb={}",
            currentUserId, queryDto.getWaferId(), queryDto.getLayerId(), queryDto.getMeasurementRunId(), queryDto.getMetricCode(),
            result.size(), System.currentTimeMillis() - startedAt, usedMemoryMb()
        );
        return result;
    }

    @Override
    public List<OverlayHistogramBinVo> buildHistogram(OverlayQueryDto queryDto) {
        long startedAt = System.currentTimeMillis();
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        if (shouldSkipByBloomAndNullCache("histogram", currentUserId, queryDto.getMeasurementRunId(), queryDto.getMetricCode())) {
            return List.of();
        }
        String cacheKey = cacheKey("histogram", currentUserId, queryDto.getWaferId(), queryDto.getLayerId(), queryDto.getMeasurementRunId(), queryDto.getMetricCode());
        List<OverlayHistogramBinVo> result = getOrComputeCached(
            "histogram",
            cacheKey,
            OverlayHistogramBinVo.class,
            () -> {
                // 直方图只关心“某个 metric 的数值分布”，所以先抽出 metric 值再做分桶。
                List<BigDecimal> values = selectPoints("histogram", currentUserId, queryDto.getWaferId(), queryDto.getLayerId(), queryDto.getMeasurementRunId(), queryDto.getMetricCode())
                    .stream()
                    .map(point -> resolveMetricValue(point, queryDto.getMetricCode()))
                    .filter(value -> value != null)
                    .sorted()
                    .toList();
                if (values.isEmpty()) {
                    return List.of();
                }
                BigDecimal min = values.get(0);
                BigDecimal max = values.get(values.size() - 1);
                int bins = 8;
                BigDecimal range = max.subtract(min);
                BigDecimal step = range.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE
                    : range.divide(BigDecimal.valueOf(bins), 6, RoundingMode.HALF_UP);
                return java.util.stream.IntStream.range(0, bins)
                    .mapToObj(index -> {
                        BigDecimal start = min.add(step.multiply(BigDecimal.valueOf(index)));
                        BigDecimal end = index == bins - 1 ? max : start.add(step);
                        long count = values.stream().filter(value -> {
                            if (index == bins - 1) {
                                return value.compareTo(start) >= 0 && value.compareTo(end) <= 0;
                            }
                            return value.compareTo(start) >= 0 && value.compareTo(end) < 0;
                        }).count();
                        OverlayHistogramBinVo vo = new OverlayHistogramBinVo();
                        vo.setRangeStart(start.setScale(3, RoundingMode.HALF_UP));
                        vo.setRangeEnd(end.setScale(3, RoundingMode.HALF_UP));
                        vo.setCount(count);
                        return vo;
                    })
                    .toList();
            }
        );
        log.info(
            "[overlay-analysis] histogram userId={} waferId={} layerId={} runId={} metricCode={} bins={} elapsedMs={}",
            currentUserId, queryDto.getWaferId(), queryDto.getLayerId(), queryDto.getMeasurementRunId(), queryDto.getMetricCode(),
            result.size(), System.currentTimeMillis() - startedAt
        );
        return result;
    }

    @Override
    public List<OverlayTrendPointVo> buildTrends(TrendQueryDto queryDto) {
        long startedAt = System.currentTimeMillis();
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        String cacheKey = cacheKey("trend", currentUserId, queryDto.getWaferId(), queryDto.getLayerId(), queryDto.getTaskId());
        List<OverlayTrendPointVo> result = getOrComputeCached(
            "trend",
            cacheKey,
            OverlayTrendPointVo.class,
            () -> {
                // 趋势图直接读 simulation_result_summary，避免每次趋势查询都重扫海量点位表。
                // 换句话说：趋势图关注的是“每次 run 的汇总表现”，不是单次 run 内部的空间分布。
                List<SimulationResultSummary> rows = simulationResultSummaryMapper.selectList(
                    new LambdaQueryWrapper<SimulationResultSummary>()
                        // 趋势图允许按 layer / wafer / task 这些“run 级维度”切 KPI 序列。
                        .eq(queryDto.getLayerId() != null, SimulationResultSummary::getLayerId, queryDto.getLayerId())
                        .eq(queryDto.getWaferId() != null, SimulationResultSummary::getWaferId, queryDto.getWaferId())
                        .eq(queryDto.getTaskId() != null, SimulationResultSummary::getTaskId, queryDto.getTaskId())
                        // summary 的可见性通过 taskId 反向约束，统一复用任务的 demo/mine 口径。
                        .inSql(SimulationResultSummary::getTaskId,
                            "SELECT id FROM simulation_task WHERE deleted = 0 AND (created_by = 0 OR created_by = " + currentUserId + ")")
                        .orderByAsc(SimulationResultSummary::getCreatedAt)
                );
                List<OverlayTrendPointVo> rowsVo = new ArrayList<>(rows.size());
                for (int i = 0; i < rows.size(); i++) {
                    SimulationResultSummary row = rows.get(i);
                    OverlayTrendPointVo vo = new OverlayTrendPointVo();
                    vo.setDate(row.getCreatedAt().toLocalDate());
                    vo.setLabel("Run-" + (i + 1));
                    vo.setPassRate(row.getPassRate() == null ? BigDecimal.ZERO : row.getPassRate());
                    vo.setMeanOverlay(row.getMeanOverlay() == null ? BigDecimal.ZERO : row.getMeanOverlay());
                    vo.setP95Overlay(row.getP95Overlay() == null ? BigDecimal.ZERO : row.getP95Overlay());
                    rowsVo.add(vo);
                }
                return rowsVo;
            }
        );
        log.info(
            "[overlay-analysis] trend-kpi userId={} waferId={} layerId={} taskId={} summaryRows={} trendPoints={} elapsedMs={} memoryUsedMb={}",
            currentUserId, queryDto.getWaferId(), queryDto.getLayerId(), queryDto.getTaskId(),
            result.size(), result.size(), System.currentTimeMillis() - startedAt, usedMemoryMb()
        );
        return result;
    }

    private List<OverlayMeasurementPoint> selectPoints(
        String chartType,
        Long currentUserId,
        Long waferId,
        Long layerId,
        Long measurementRunId,
        String metricCode
    ) {
        long startedAt = System.currentTimeMillis();
        // selectPoints 是 heatmap / scatter / histogram 共用的数据入口。
        // 它统一处理：
        // 1. demo + mine 可见性约束
        // 2. 按 runId 或 waferId/layerId 查点位
        // 3. 查询为空时写空值缓存
        // 4. 稀疏 run 自动 densify
        LambdaQueryWrapper<OverlayMeasurementPoint> wrapper = new LambdaQueryWrapper<OverlayMeasurementPoint>()
            .inSql(OverlayMeasurementPoint::getMeasurementRunId,
                "SELECT id FROM measurement_run WHERE deleted = 0 "
                    + "AND (created_by = 0 OR created_by = " + currentUserId + " OR analysis_fingerprint IS NOT NULL)");
        if (measurementRunId != null) {
            // runId 是最精确、最常见的图表查询条件，优先级最高。
            wrapper.eq(OverlayMeasurementPoint::getMeasurementRunId, measurementRunId);
        } else {
            // 没有 runId 时，退化为按 waferId + layerId 过滤。
            wrapper.eq(waferId != null, OverlayMeasurementPoint::getWaferId, waferId)
                .eq(layerId != null, OverlayMeasurementPoint::getLayerId, layerId);
        }
        List<OverlayMeasurementPoint> points = overlayMeasurementPointMapper.selectList(wrapper);
        // 查询为空后，不直接写空值缓存，而是再确认 run 对当前用户是否真的不存在/不可见。
        // 这样能避免“只是没有点位数据”与“run 根本不存在”被混淆。
        if (measurementRunId != null && points.isEmpty() && !measurementRunExistsForUser(measurementRunId, currentUserId)) {
            writeNullCache(chartType, currentUserId, measurementRunId, metricCode);
            return List.of();
        }
        boolean denseCacheHit = false;
        long densifyElapsedMs = 0L;
        int sourcePoints = points.size();
        int densePoints = sourcePoints;
        int parallelism = 1;
        if (measurementRunId != null && points.size() < DENSE_MIN_POINTS) {
            long densifyStart = System.currentTimeMillis();
            // 同一个 run 的密化是纯 CPU 密集重计算，必须加 run 级锁，
            // 避免多个请求同时把同一个 run 重复 densify。
            ReentrantLock runLock = denseRunLocks.computeIfAbsent(measurementRunId, key -> new ReentrantLock());
            runLock.lock();
            try {
                DensePointCacheEntry entry = denseRunCache.get(measurementRunId);
                if (entry != null && (System.currentTimeMillis() - entry.cachedAtMs) <= DENSE_CACHE_TTL_MS) {
                    points = entry.points;
                    denseCacheHit = true;
                } else {
                    DenseResult dense = densifyPoints(measurementRunId, points);
                    points = dense.points();
                    parallelism = dense.parallelism();
                    denseRunCache.put(measurementRunId, new DensePointCacheEntry(points, System.currentTimeMillis()));
                }
            } finally {
                runLock.unlock();
            }
            densifyElapsedMs = System.currentTimeMillis() - densifyStart;
            densePoints = points.size();
        }
        long downsampleElapsedMs = 0L;
        int finalPoints = points.size();
        if ("heatmap".equalsIgnoreCase(chartType) && points.size() > HEATMAP_TARGET_POINTS) {
            long downsampleStartedAt = System.currentTimeMillis();
            points = downsampleHeatmapPoints(points, HEATMAP_TARGET_POINTS);
            downsampleElapsedMs = System.currentTimeMillis() - downsampleStartedAt;
            finalPoints = points.size();
        }
        log.info(
            "[overlay-analysis] {} selectPoints userId={} waferId={} layerId={} runId={} sourcePoints={} densePoints={} finalPoints={} densifyElapsedMs={} downsampleElapsedMs={} parallelism={} denseCacheHit={} queryElapsedMs={} memoryUsedMb={}",
            chartType, currentUserId, waferId, layerId, measurementRunId, sourcePoints, densePoints, finalPoints,
            densifyElapsedMs, downsampleElapsedMs, parallelism, denseCacheHit, System.currentTimeMillis() - startedAt, usedMemoryMb()
        );
        return points;
    }

    private List<OverlayMeasurementPoint> downsampleHeatmapPoints(List<OverlayMeasurementPoint> points, int targetPoints) {
        /*
         * densify 后二次稀疏策略（网格聚合）：
         * 1. 先按目标点位反推网格分辨率，保证全圆区域覆盖尽量均匀；
         * 2. 每个网格桶只保留 1 个代表点，避免同一区域过度重复采样；
         * 3. 代表点使用“坐标与指标均值 + 热点最大值保真”，兼顾整体形态与热点可见性。
         *
         * 选择该策略而不是 random / 前 N 的原因：
         * - random 会导致局部空洞和边缘覆盖不稳定；
         * - 前 N 严重依赖数据顺序，空间分布失真；
         * - 网格聚合可以稳定保留 wafer 圆形区域的空间分布特征。
         */
        if (points.size() <= targetPoints) {
            return points;
        }
        int gridCount = Math.max(1, (int) Math.ceil(Math.sqrt(targetPoints * 4d / Math.PI)));
        double cellSize = 250d / gridCount;
        Map<Long, HeatmapBucket> buckets = new HashMap<>(targetPoints * 2);
        for (OverlayMeasurementPoint point : points) {
            if (point.getXCoord() == null || point.getYCoord() == null) {
                continue;
            }
            int gx = Math.max(0, Math.min(gridCount - 1, (int) Math.floor(point.getXCoord().doubleValue() / cellSize)));
            int gy = Math.max(0, Math.min(gridCount - 1, (int) Math.floor(point.getYCoord().doubleValue() / cellSize)));
            long key = bucketKey(gy, gx);
            HeatmapBucket bucket = buckets.computeIfAbsent(key, ignored -> new HeatmapBucket());
            bucket.accept(point);
        }
        if (buckets.isEmpty()) {
            return List.of();
        }

        // 关键修复：旧实现基于 HashMap.values() + 固定 stride 裁剪，容易产生结构性斜线伪影。
        // 这里改为“按 gy/gx 有序遍历 + 行偏移采样”，打散固定步长带来的方向性纹理。
        int stride = Math.max(1, (int) Math.ceil(Math.sqrt((double) buckets.size() / targetPoints)));
        List<Long> sampledKeys = new ArrayList<>(Math.min(targetPoints * 2, buckets.size()));
        HashSet<Long> sampledKeySet = new HashSet<>(Math.min(targetPoints * 2, buckets.size()));
        for (int gy = 0; gy < gridCount; gy++) {
            int startGx = gy % stride;
            for (int gx = startGx; gx < gridCount; gx += stride) {
                long key = bucketKey(gy, gx);
                if (buckets.containsKey(key) && sampledKeySet.add(key)) {
                    sampledKeys.add(key);
                }
            }
        }
        if (sampledKeys.size() < targetPoints) {
            for (int gy = 0; gy < gridCount && sampledKeys.size() < targetPoints; gy++) {
                for (int gx = 0; gx < gridCount && sampledKeys.size() < targetPoints; gx++) {
                    long key = bucketKey(gy, gx);
                    if (buckets.containsKey(key) && sampledKeySet.add(key)) {
                        sampledKeys.add(key);
                    }
                }
            }
        }

        List<OverlayMeasurementPoint> sampled = sampledKeys.stream()
            .map(key -> buckets.get(key))
            .filter(Objects::nonNull)
            .map(HeatmapBucket::toPoint)
            .toList();
        if (sampled.size() <= targetPoints) {
            return sampled;
        }

        // 超过目标时做等距抽样，避免再次引入“从 0 号位固定跳步”导致的方向偏置。
        List<OverlayMeasurementPoint> trimmed = new ArrayList<>(targetPoints);
        double step = sampled.size() / (double) targetPoints;
        double cursor = step / 2d;
        while (trimmed.size() < targetPoints && sampled.size() > 0) {
            int index = Math.max(0, Math.min(sampled.size() - 1, (int) Math.floor(cursor)));
            trimmed.add(sampled.get(index));
            cursor += step;
        }
        return trimmed;
    }

    private long bucketKey(int gy, int gx) {
        return (((long) gy) << 32) | (gx & 0xffffffffL);
    }

    private DenseResult densifyPoints(Long runId, List<OverlayMeasurementPoint> source) {
        // densifyPoints 的目标不是“精确还原真实量测点”，而是把历史稀疏 run 重建成高密度连续场，
        // 让老 run/导入 run 在热图页面上的视觉效果和新生成 run 更接近。
        //
        // 实现思路：
        // 1. 先从 source 中提取几个基线统计量（平均 magnitude / 平均 x / 平均 y）
        // 2. 再用规则模型在固定步长网格上补点
        // 3. 最后返回一个高密度的 OverlayMeasurementPoint 列表
        double meanMagnitude = source.stream()
            .map(OverlayMeasurementPoint::getOverlayMagnitude)
            .filter(v -> v != null)
            .mapToDouble(BigDecimal::doubleValue)
            .average()
            .orElse(3.0d);
        double meanX = source.stream().map(OverlayMeasurementPoint::getOverlayX).filter(v -> v != null).mapToDouble(BigDecimal::doubleValue).average().orElse(0d);
        double meanY = source.stream().map(OverlayMeasurementPoint::getOverlayY).filter(v -> v != null).mapToDouble(BigDecimal::doubleValue).average().orElse(0d);
        double seed = (runId % 997) / 997d;
        // 并行度上限 8，避免在大核机器上无限开线程导致线程切换开销失控。
        int parallelism = Math.max(1, analysisExecutionProperties.getOverlayDensifyParallelism());
        int xSlices = Math.max(1, parallelism);
        List<Future<DenseChunk>> futures = new ArrayList<>(xSlices);
        for (int slice = 0; slice < xSlices; slice++) {
            final int sliceIndex = slice;
            futures.add(overlayComputeExecutor.submit(() -> {
                // 每个分片只负责自己那一列 x 区间，局部生成后再 merge，避免共享可变集合竞争。
                List<OverlayMeasurementPoint> dense = new ArrayList<>(25000);
                int seq = 0;
                for (double x = sliceIndex * DENSE_STEP; x <= 250 + 1e-9; x += DENSE_STEP * xSlices) {
                    for (double y = 0; y <= 250 + 1e-9; y += DENSE_STEP) {
                        double nx = (x - 125d) / 125d;
                        double ny = (y - 125d) / 125d;
                        double r = Math.sqrt(nx * nx + ny * ny);
                        if (r > 1d) {
                            continue;
                        }
                        double wave = Math.sin((nx + seed) * Math.PI * 4.2) + Math.cos((ny - seed) * Math.PI * 3.7);
                        double hotspot = Math.exp(-((x - 182) * (x - 182) + (y - 72) * (y - 72)) / 1600d)
                            - Math.exp(-((x - 62) * (x - 62) + (y - 185) * (y - 185)) / 2100d);
                        double ox = meanX + meanMagnitude * (0.52 * nx + 0.21 * wave) + 1.4 * r * nx + 1.3 * hotspot;
                        double oy = meanY + meanMagnitude * (0.48 * ny - 0.19 * wave) + 1.2 * r * ny - 0.9 * hotspot;
                        double mag = Math.sqrt(ox * ox + oy * oy);
                        OverlayMeasurementPoint p = new OverlayMeasurementPoint();
                        p.setTargetCode("DENSE-" + String.format(Locale.ROOT, "%02d%06d", sliceIndex, ++seq));
                        p.setXCoord(BigDecimal.valueOf(x).setScale(4, RoundingMode.HALF_UP));
                        p.setYCoord(BigDecimal.valueOf(y).setScale(4, RoundingMode.HALF_UP));
                        p.setOverlayX(BigDecimal.valueOf(ox).setScale(6, RoundingMode.HALF_UP));
                        p.setOverlayY(BigDecimal.valueOf(oy).setScale(6, RoundingMode.HALF_UP));
                        p.setOverlayMagnitude(BigDecimal.valueOf(mag).setScale(6, RoundingMode.HALF_UP));
                        p.setResidualValue(BigDecimal.valueOf(Math.abs(wave) * 0.2 + Math.abs(hotspot) * 0.25).setScale(6, RoundingMode.HALF_UP));
                        p.setFocusValue(BigDecimal.valueOf(0.03 + nx * 0.015 + hotspot * 0.012).setScale(6, RoundingMode.HALF_UP));
                        p.setDoseValue(BigDecimal.valueOf(42 + ny * 0.6 + hotspot * 0.5).setScale(6, RoundingMode.HALF_UP));
                        p.setConfidence(BigDecimal.valueOf(Math.max(0.85, 0.99 - r * 0.1)).setScale(4, RoundingMode.HALF_UP));
                        p.setIsOutlier(mag > meanMagnitude * 2.8 ? 1 : 0);
                        dense.add(p);
                    }
                }
                return new DenseChunk(sliceIndex, dense);
            }));
        }
        List<DenseChunk> chunks = new ArrayList<>(futures.size());
        for (Future<DenseChunk> future : futures) {
            try {
                chunks.add(future.get(30, TimeUnit.SECONDS));
            } catch (Exception ex) {
                log.warn("[overlay-analysis] densify chunk failed runId={} reason={}", runId, ex.getMessage());
            }
        }
        chunks.sort(Comparator.comparingInt(DenseChunk::slice));
        List<OverlayMeasurementPoint> merged = new ArrayList<>(200000);
        for (DenseChunk chunk : chunks) {
            merged.addAll(chunk.points());
        }
        return new DenseResult(merged, Math.max(1, chunks.size()));
    }

    private boolean measurementRunExistsForUser(Long runId, Long currentUserId) {
        // 布隆过滤器只能做“是否可能存在”的粗判断。
        // 当查询为空、准备写空值缓存时，仍需用真实 SQL 确认当前用户是否真的看不到这个 run。
        if (measurementRunMapper == null) {
            return true;
        }
        Long count = measurementRunMapper.selectCount(new LambdaQueryWrapper<MeasurementRun>()
            .eq(MeasurementRun::getId, runId)
            .eq(MeasurementRun::getDeleted, 0)
            .and(w -> w.eq(MeasurementRun::getCreatedBy, 0L)
                .or().eq(MeasurementRun::getCreatedBy, currentUserId)
                .or().isNotNull(MeasurementRun::getAnalysisFingerprint)));
        return count != null && count > 0;
    }

    private boolean shouldSkipByBloomAndNullCache(String bizType, Long currentUserId, Long runId, String metricCode) {
        // 这是图表查询链路最前面的“穿透防护层”：
        // 1. 先用布隆过滤器挡掉明显不存在的 runId
        // 2. 再用空值缓存挡住“近期已确认不存在/不可见”的 runId
        // 3. 只有两层都没拦住，才继续走最终结果缓存和数据库查询
        if (runId == null) {
            return false;
        }
        boolean bloomHit = bloomMayContainRunId(runId);
        if (!bloomHit) {
            log.info("[overlay-analysis] bloomMiss bizType={} runId={} dbSkipped=true", bizType, runId);
            return true;
        }
        if (isNullCacheHit(bizType, currentUserId, runId, metricCode)) {
            log.info("[overlay-analysis] bloomHit bizType={} runId={} nullCacheHit=true dbSkipped=true", bizType, runId);
            return true;
        }
        log.info("[overlay-analysis] bloomHit bizType={} runId={} nullCacheHit=false dbSkipped=false", bizType, runId);
        return false;
    }

    private boolean bloomMayContainRunId(Long runId) {
        // true = 可能存在，false = 一定不存在。
        // 这是布隆过滤器的典型语义：允许误判，但不允许漏判。
        if (runId == null || stringRedisTemplate == null) {
            return true;
        }
        ensureBloomReady();
        for (long offset : bloomOffsets(runId)) {
            Boolean bit = stringRedisTemplate.opsForValue().getBit(BLOOM_MEASUREMENT_RUN_KEY, offset);
            if (Boolean.FALSE.equals(bit)) {
                return false;
            }
        }
        return true;
    }

    private void ensureBloomReady() {
        // 布隆过滤器采用“懒初始化 + 增量刷新”策略：
        // - 第一次请求触达时，把已有 measurement_run.id 批量灌入 bitmap
        // - 后续请求仅补充比 measurementRunBloomMaxId 更大的 runId
        if (measurementRunMapper == null) {
            return;
        }
        if (measurementRunBloomInitialized.get()) {
            refreshBloomIncremental();
            return;
        }
        if (!bloomInitLock.tryLock()) {
            return;
        }
        try {
            if (measurementRunBloomInitialized.get()) {
                refreshBloomIncremental();
                return;
            }
            refreshBloomFrom(0L);
            measurementRunBloomInitialized.set(true);
        } finally {
            bloomInitLock.unlock();
        }
    }

    private void refreshBloomIncremental() {
        refreshBloomFrom(measurementRunBloomMaxId);
    }

    private void refreshBloomFrom(long fromExclusiveId) {
        // 每次最多刷新 2000 个 runId，避免一次扫描过多 measurement_run 影响接口首响应。
        if (stringRedisTemplate == null || measurementRunMapper == null) {
            return;
        }
        List<MeasurementRun> runs;
        try {
            runs = measurementRunMapper.selectList(new LambdaQueryWrapper<MeasurementRun>()
                .select(MeasurementRun::getId)
                .gt(fromExclusiveId > 0, MeasurementRun::getId, fromExclusiveId)
                .eq(MeasurementRun::getDeleted, 0)
                .orderByAsc(MeasurementRun::getId)
                .last("LIMIT 2000"));
        } catch (Exception ex) {
            log.warn("[overlay-analysis] bloom refresh skipped reason={}", ex.getMessage());
            return;
        }
        if (runs == null || runs.isEmpty()) {
            return;
        }
        for (MeasurementRun run : runs) {
            if (run.getId() == null) {
                continue;
            }
            for (long offset : bloomOffsets(run.getId())) {
                stringRedisTemplate.opsForValue().setBit(BLOOM_MEASUREMENT_RUN_KEY, offset, true);
            }
            measurementRunBloomMaxId = Math.max(measurementRunBloomMaxId, run.getId());
        }
    }

    private long[] bloomOffsets(Long runId) {
        // 通过多次 hash 混合生成多个 bit 位偏移，组成一个简单 bitmap bloom。
        long[] offsets = new long[BLOOM_HASH_COUNT];
        long base = runId == null ? 0L : runId;
        for (int i = 0; i < BLOOM_HASH_COUNT; i++) {
            long mixed = mixHash(base + (i + 1L) * 0x9e3779b97f4a7c15L);
            offsets[i] = Math.floorMod(mixed, BLOOM_BITS);
        }
        return offsets;
    }

    private long mixHash(long value) {
        long x = value;
        x ^= (x >>> 33);
        x *= 0xff51afd7ed558ccdL;
        x ^= (x >>> 33);
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= (x >>> 33);
        return x;
    }

    private boolean isNullCacheHit(String bizType, Long currentUserId, Long runId, String metricCode) {
        // 空值缓存必须带 userId，否则 demo/mine 场景会串用户结果。
        if (stringRedisTemplate == null) {
            return false;
        }
        String key = nullCacheKey(bizType, currentUserId, runId, metricCode);
        String raw = stringRedisTemplate.opsForValue().get(key);
        return raw != null && !raw.isBlank();
    }

    private void writeNullCache(String bizType, Long currentUserId, Long runId, String metricCode) {
        if (stringRedisTemplate == null) {
            return;
        }
        String key = nullCacheKey(bizType, currentUserId, runId, metricCode);
        stringRedisTemplate.opsForValue().set(key, "1", ttlWithJitterSec(45, 15));
    }

    private String nullCacheKey(String bizType, Long currentUserId, Long runId, String metricCode) {
        return NULL_CACHE_PREFIX + bizType + ":" + currentUserId + ":" + runId + ":" + (metricCode == null ? "_" : metricCode);
    }

    private void trimExcessUserTasks(Long currentUserId) {
        if (simulationTaskMapper == null) {
            return;
        }
        List<SimulationTask> tasks = simulationTaskMapper.selectList(new LambdaQueryWrapper<SimulationTask>()
            .eq(SimulationTask::getCreatedBy, currentUserId)
            .eq(SimulationTask::getDeleted, 0)
            .orderByAsc(SimulationTask::getCreatedAt)
            .orderByAsc(SimulationTask::getId));
        int overflow = tasks.size() - MAX_USER_TASKS;
        if (overflow <= 0) {
            return;
        }
        List<Long> toDelete = tasks.stream().limit(overflow).map(SimulationTask::getId).toList();
        simulationTaskMapper.deleteBatchIds(toDelete);
        log.info("[overlay-analysis] trim async tasks userId={} removed={}", currentUserId, toDelete.size());
    }

    private <T> List<T> getOrComputeCached(
        String bizType,
        String cacheKey,
        Class<T> elementType,
        Supplier<List<T>> supplier
    ) {
        // getOrComputeCached 是“最终结果缓存”的统一入口。
        // 它缓存的是图表接口最终 VO 列表，与前面的布隆过滤器/空值缓存不是一个层次。
        List<T> cached = readCachedList(cacheKey, elementType);
        if (cached != null) {
            return cached;
        }
        if (stringRedisTemplate == null || objectMapper == null) {
            return supplier.get();
        }
        String lockKey = LOCK_PREFIX + bizType + ":" + cacheKey;
        String lockValue = UUID.randomUUID().toString();
        // 互斥重建：热点 key 过期时，只允许一个线程回源构建缓存，其他线程短暂等待再重查缓存。
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, LOCK_TTL);
        if (Boolean.TRUE.equals(locked)) {
            log.info("[overlay-analysis] {} lockAcquireSuccess cacheKey={}", bizType, cacheKey);
            try {
                // 双重检查：等待锁竞争期间，可能已有其他线程重建完缓存。
                List<T> secondCheck = readCachedList(cacheKey, elementType);
                if (secondCheck != null) {
                    log.info("[overlay-analysis] {} cacheHitAfterWait cacheKey={} size={}", bizType, cacheKey, secondCheck.size());
                    return secondCheck;
                }
                List<T> computed = supplier.get();
                writeCachedList(cacheKey, computed, ttlWithJitterSec(60, 30));
                log.info("[overlay-analysis] {} cacheRebuild cacheKey={} size={}", bizType, cacheKey, computed.size());
                return computed;
            } finally {
                try {
                    stringRedisTemplate.execute(UNLOCK_SCRIPT, List.of(lockKey), lockValue);
                } catch (Exception ex) {
                    log.warn("[overlay-analysis] unlock failed lockKey={} reason={}", lockKey, ex.getMessage());
                }
            }
        }
        log.info("[overlay-analysis] {} lockWait cacheKey={}", bizType, cacheKey);
        for (int i = 0; i < LOCK_WAIT_ROUNDS; i++) {
            try {
                Thread.sleep(LOCK_WAIT_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
            List<T> waited = readCachedList(cacheKey, elementType);
            if (waited != null) {
                log.info("[overlay-analysis] {} cacheHitAfterWait cacheKey={} size={}", bizType, cacheKey, waited.size());
                return waited;
            }
        }
        return supplier.get();
    }

    private record DenseChunk(int slice, List<OverlayMeasurementPoint> points) {}

    private static ExecutorService newFixedExecutor(String threadPrefix, int poolSize) {
        int bounded = Math.max(1, Math.min(poolSize, 32));
        AtomicInteger threadSeq = new AtomicInteger(1);
        return Executors.newFixedThreadPool(
            bounded,
            runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName(threadPrefix + "-" + threadSeq.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        );
    }

    private static AnalysisExecutionProperties defaultExecutionProperties() {
        AnalysisExecutionProperties properties = new AnalysisExecutionProperties();
        int processors = Runtime.getRuntime().availableProcessors();
        int defaultParallelism = Math.max(2, Math.min(8, processors));
        properties.setOverlayExecutorPoolSize(defaultParallelism);
        properties.setOverlayDensifyParallelism(defaultParallelism);
        properties.setWaferGenerateExecutorPoolSize(defaultParallelism);
        properties.setWaferGenerateParallelism(defaultParallelism);
        return properties;
    }

    private record DenseResult(List<OverlayMeasurementPoint> points, int parallelism) {
        // DenseResult 同时返回“密化结果”和“实际并行度”，便于日志观察和性能分析。
    }

    private static class HeatmapBucket {
        private int count;
        private double xSum;
        private double ySum;
        private double overlayXSum;
        private double overlayYSum;
        private double metricSum;
        private double residualSum;
        private double focusSum;
        private double doseSum;
        private double confidenceSum;
        private int outlierCount;
        private double hotspotMetricMax = Double.NEGATIVE_INFINITY;
        private String targetCode = "DS";

        void accept(OverlayMeasurementPoint point) {
            count++;
            xSum += asDouble(point.getXCoord());
            ySum += asDouble(point.getYCoord());
            overlayXSum += asDouble(point.getOverlayX());
            overlayYSum += asDouble(point.getOverlayY());
            double metric = asDouble(point.getOverlayMagnitude());
            metricSum += metric;
            hotspotMetricMax = Math.max(hotspotMetricMax, metric);
            residualSum += asDouble(point.getResidualValue());
            focusSum += asDouble(point.getFocusValue());
            doseSum += asDouble(point.getDoseValue());
            confidenceSum += asDouble(point.getConfidence());
            outlierCount += point.getIsOutlier() != null && point.getIsOutlier() > 0 ? 1 : 0;
            if (point.getTargetCode() != null && !point.getTargetCode().isBlank()) {
                targetCode = point.getTargetCode();
            }
        }

        OverlayMeasurementPoint toPoint() {
            OverlayMeasurementPoint point = new OverlayMeasurementPoint();
            point.setTargetCode(targetCode);
            point.setXCoord(scale4(xSum / count));
            point.setYCoord(scale4(ySum / count));
            point.setOverlayX(scale6(overlayXSum / count));
            point.setOverlayY(scale6(overlayYSum / count));
            double avgMetric = metricSum / count;
            // 热点保真：避免桶内热点在均值化后被完全抹平。
            double preservedMetric = Math.max(avgMetric, hotspotMetricMax * 0.85d);
            point.setOverlayMagnitude(scale6(preservedMetric));
            point.setResidualValue(scale6(residualSum / count));
            point.setFocusValue(scale6(focusSum / count));
            point.setDoseValue(scale6(doseSum / count));
            point.setConfidence(scale4(confidenceSum / count));
            point.setIsOutlier(outlierCount * 2 >= count ? 1 : 0);
            return point;
        }

        private static double asDouble(BigDecimal value) {
            return value == null ? 0d : value.doubleValue();
        }

        private static BigDecimal scale4(double value) {
            return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
        }

        private static BigDecimal scale6(double value) {
            return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
        }
    }

    private record DensePointCacheEntry(List<OverlayMeasurementPoint> points, long cachedAtMs) {
        // 进程内短 TTL 缓存，专门用于“稀疏 run -> 高密度连续场”的密化结果复用。
    }

    private record KpiStats(
        BigDecimal meanOverlay,
        BigDecimal p95Overlay,
        BigDecimal stdOverlay,
        BigDecimal passRate,
        BigDecimal outlierDensity,
        BigDecimal edgeRiskScore,
        BigDecimal stabilityScore,
        BigDecimal edgeMeanOverlay,
        BigDecimal centerMeanOverlay,
        BigDecimal centerEdgeDeviation,
        BigDecimal hotspotSeverity,
        BigDecimal maxOverlay
    ) {
        static KpiStats empty() {
            BigDecimal zero = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            return new KpiStats(zero, zero, zero, zero, zero, zero, zero, zero, zero, zero, zero, zero);
        }
    }
    private OverlayScatterPointVo kpiPoint(String name, BigDecimal xValue, BigDecimal yValue) {
        // 预留的 KPI 点构造器：如果后续散点图从“点位散点”切到“KPI 散点”，这里可以直接复用。
        OverlayScatterPointVo vo = new OverlayScatterPointVo();
        vo.setTargetCode(name);
        vo.setXValue(xValue.setScale(4, RoundingMode.HALF_UP));
        vo.setYValue(yValue.setScale(4, RoundingMode.HALF_UP));
        vo.setOverlayMagnitude(yValue.setScale(4, RoundingMode.HALF_UP));
        vo.setOutlier(0);
        return vo;
    }

    private KpiStats summarizePoints(List<OverlayMeasurementPoint> points) {
        // summarizePoints 目前主要是沉淀成一个“可复用 KPI 统计模型”，
        // 方便后续散点图、结果总结或单独 KPI 面板继续扩展。
        List<BigDecimal> mags = points.stream().map(OverlayMeasurementPoint::getOverlayMagnitude).filter(v -> v != null).sorted().toList();
        if (mags.isEmpty()) {
            return KpiStats.empty();
        }
        BigDecimal mean = average(mags);
        BigDecimal p95 = mags.get(Math.max(0, (int) Math.ceil(mags.size() * 0.95) - 1));
        BigDecimal std = stddev(mags, mean);
        long outliers = mags.stream().filter(v -> v.doubleValue() > p95.doubleValue() * 1.2).count();
        List<OverlayMeasurementPoint> edge = points.stream().filter(this::isEdgePoint).toList();
        List<OverlayMeasurementPoint> center = points.stream().filter(p -> !isEdgePoint(p)).toList();
        BigDecimal edgeMean = average(edge.stream().map(OverlayMeasurementPoint::getOverlayMagnitude).filter(v -> v != null).toList());
        BigDecimal centerMean = average(center.stream().map(OverlayMeasurementPoint::getOverlayMagnitude).filter(v -> v != null).toList());
        BigDecimal passRate = BigDecimal.valueOf(mags.stream().filter(v -> v.doubleValue() <= p95.doubleValue()).count())
            .divide(BigDecimal.valueOf(mags.size()), 4, RoundingMode.HALF_UP);
        BigDecimal outlierDensity = BigDecimal.valueOf(outliers).divide(BigDecimal.valueOf(mags.size()), 4, RoundingMode.HALF_UP);
        BigDecimal edgeRiskScore = edgeMean.multiply(new BigDecimal("12")).setScale(4, RoundingMode.HALF_UP);
        BigDecimal stabilityScore = BigDecimal.valueOf(Math.max(0, 100 - std.doubleValue() * 12)).setScale(4, RoundingMode.HALF_UP);
        BigDecimal hotspot = mags.get(mags.size() - 1).subtract(p95).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
        return new KpiStats(mean, p95, std, passRate, outlierDensity, edgeRiskScore, stabilityScore,
            edgeMean, centerMean, edgeMean.subtract(centerMean).abs().setScale(4, RoundingMode.HALF_UP), hotspot, mags.get(mags.size() - 1));
    }

    private boolean isEdgePoint(OverlayMeasurementPoint point) {
        // edge 的定义采用归一化半径阈值，而不是直接看绝对坐标值。
        // 这样不管点位密度怎么变化，边缘区域判断都更稳定。
        if (point.getXCoord() == null || point.getYCoord() == null) {
            return false;
        }
        double nx = (point.getXCoord().doubleValue() - 125d) / 125d;
        double ny = (point.getYCoord().doubleValue() - 125d) / 125d;
        return Math.sqrt(nx * nx + ny * ny) >= 0.82;
    }

    private BigDecimal stddev(List<BigDecimal> values, BigDecimal mean) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        double meanValue = mean.doubleValue();
        double variance = values.stream()
            .mapToDouble(v -> {
                double d = v.doubleValue() - meanValue;
                return d * d;
            })
            .average()
            .orElse(0d);
        return BigDecimal.valueOf(Math.sqrt(Math.max(0d, variance))).setScale(4, RoundingMode.HALF_UP);
    }

    private long usedMemoryMb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }

    private <T> List<T> readCachedList(String cacheKey, Class<T> elementType) {
        if (stringRedisTemplate == null || objectMapper == null) {
            return null;
        }
        try {
            // Redis 中缓存的是 JSON 字符串，这里按“列表 + 元素类型”反序列化回来。
            String raw = stringRedisTemplate.opsForValue().get(cacheKey);
            if (raw == null || raw.isBlank()) {
                return null;
            }
            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            return objectMapper.readValue(raw, listType);
        } catch (Exception ex) {
            log.warn("[overlay-analysis] cache read failed key={} reason={}", cacheKey, ex.getMessage());
            return null;
        }
    }

    private <T> void writeCachedList(String cacheKey, List<T> value, Duration ttl) {
        if (stringRedisTemplate == null || objectMapper == null) {
            return;
        }
        try {
            // 图表接口缓存的是“接口最终返回值”，这样命中缓存时可直接返回给 Controller/前端。
            String raw = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(cacheKey, raw, ttl);
        } catch (Exception ex) {
            log.warn("[overlay-analysis] cache write failed key={} reason={}", cacheKey, ex.getMessage());
        }
    }

    private String cacheKey(String bizType, Object... parts) {
        // cache key 设计思路：业务类型 + 用户维度 + 查询条件。
        // 把 userId 放进去是为了避免 demo/mine 数据在缓存层串用户。
        StringBuilder sb = new StringBuilder(CACHE_PREFIX).append(bizType);
        for (Object part : parts) {
            sb.append(':');
            sb.append(part == null ? "_" : String.valueOf(part));
        }
        return sb.toString();
    }

    private Duration ttlWithJitterSec(int baseSec, int jitterSec) {
        // TTL 加随机抖动，避免一批 key 在同一时刻同时过期引发雪崩式回源。
        int ttl = baseSec + ThreadLocalRandom.current().nextInt(0, Math.max(1, jitterSec + 1));
        return Duration.ofSeconds(ttl);
    }

    private BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> nonNullValues = values.stream().filter(value -> value != null).toList();
        if (nonNullValues.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return nonNullValues.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(nonNullValues.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveMetricValue(OverlayMeasurementPoint point, String metricCode) {
        // 这个方法把前端传来的 metricCode 翻译成点位对象上的具体字段，
        // 是 heatmap / histogram 等多图共用的 metric 选择开关。
        if ("overlay_x".equalsIgnoreCase(metricCode)) {
            return point.getOverlayX();
        }
        if ("overlay_y".equalsIgnoreCase(metricCode)) {
            return point.getOverlayY();
        }
        if ("residual".equalsIgnoreCase(metricCode)) {
            return point.getResidualValue();
        }
        if ("focus".equalsIgnoreCase(metricCode)) {
            return point.getFocusValue();
        }
        if ("dose".equalsIgnoreCase(metricCode)) {
            return point.getDoseValue();
        }
        return point.getOverlayMagnitude();
    }
}
