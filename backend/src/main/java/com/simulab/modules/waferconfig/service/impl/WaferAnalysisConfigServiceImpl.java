package com.simulab.modules.waferconfig.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simulab.config.AnalysisExecutionProperties;
import com.simulab.common.exception.BusinessException;
import com.simulab.common.security.SecurityContextUtils;
import com.simulab.modules.layer.entity.Layer;
import com.simulab.modules.layer.mapper.LayerMapper;
import com.simulab.modules.lot.entity.Lot;
import com.simulab.modules.lot.mapper.LotMapper;
import com.simulab.modules.measurement.entity.MeasurementRun;
import com.simulab.modules.measurement.mapper.MeasurementRunMapper;
import com.simulab.modules.overlayanalysis.entity.OverlayMeasurementPoint;
import com.simulab.modules.overlayanalysis.entity.SimulationResultSummary;
import com.simulab.modules.overlayanalysis.mapper.OverlayMeasurementPointMapper;
import com.simulab.modules.overlayanalysis.mapper.SimulationResultSummaryMapper;
import com.simulab.modules.simulationtask.entity.SimulationTask;
import com.simulab.modules.simulationtask.mapper.SimulationTaskMapper;
import com.simulab.modules.wafer.entity.Wafer;
import com.simulab.modules.wafer.mapper.WaferMapper;
import com.simulab.modules.waferconfig.dto.WaferAnalysisGenerateRequestDto;
import com.simulab.modules.waferconfig.dto.WaferConfigQueryDto;
import com.simulab.modules.waferconfig.dto.WaferConfigUpsertDto;
import com.simulab.modules.waferconfig.entity.WaferAnalysisConfig;
import com.simulab.modules.waferconfig.mapper.WaferAnalysisConfigMapper;
import com.simulab.modules.waferconfig.service.WaferAnalysisConfigService;
import com.simulab.modules.waferconfig.vo.WaferAnalysisGenerateResultVo;
import com.simulab.modules.waferconfig.vo.WaferConfigValidationRuleVo;
import com.simulab.modules.waferconfig.vo.WaferConfigVo;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class WaferAnalysisConfigServiceImpl implements WaferAnalysisConfigService {
    /*
     * 这个 Service 是“参数驱动型 Wafer 分析”的核心实现。
     *
     * 它承担两类职责：
     * 1. 配置管理：默认配置、校验、分页查询、增删改查。
     * 2. 分析生成：把一套配置真正转成 lot / wafer / run / 点位 / task / 汇总结果。
     *
     * 如果你想读懂 Wafer Analysis 页面背后的后端主链路，这个类是最重要的入口。
     */

    private static final Logger log = LoggerFactory.getLogger(WaferAnalysisConfigServiceImpl.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String FINGERPRINT_VERSION = "v1";
    private static final int MAX_USER_TASKS = 50;
    private static final int MAX_USER_CONFIGS = 50;
    private final ExecutorService waferPointGenerationExecutor;
    private final AnalysisExecutionProperties analysisExecutionProperties;

    private final WaferAnalysisConfigMapper configMapper;
    private final LayerMapper layerMapper;
    private final LotMapper lotMapper;
    private final WaferMapper waferMapper;
    private final MeasurementRunMapper runMapper;
    private final OverlayMeasurementPointMapper pointMapper;
    private final SimulationTaskMapper taskMapper;
    private final SimulationResultSummaryMapper summaryMapper;
    private final JdbcTemplate jdbcTemplate;

    public WaferAnalysisConfigServiceImpl(
        WaferAnalysisConfigMapper configMapper,
        LayerMapper layerMapper,
        LotMapper lotMapper,
        WaferMapper waferMapper,
        MeasurementRunMapper runMapper,
        OverlayMeasurementPointMapper pointMapper,
        SimulationTaskMapper taskMapper,
        SimulationResultSummaryMapper summaryMapper,
        JdbcTemplate jdbcTemplate,
        AnalysisExecutionProperties analysisExecutionProperties
    ) {
        this.configMapper = configMapper;
        this.layerMapper = layerMapper;
        this.lotMapper = lotMapper;
        this.waferMapper = waferMapper;
        this.runMapper = runMapper;
        this.pointMapper = pointMapper;
        this.taskMapper = taskMapper;
        this.summaryMapper = summaryMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.analysisExecutionProperties = analysisExecutionProperties == null ? defaultExecutionProperties() : analysisExecutionProperties;
        this.waferPointGenerationExecutor = newFixedExecutor(
            "wafer-point-generation",
            this.analysisExecutionProperties.getWaferGenerateExecutorPoolSize()
        );
    }

    @PreDestroy
    public void shutdownExecutors() {
        waferPointGenerationExecutor.shutdownNow();
    }

    @Override
    public WaferConfigVo buildDefaultConfig() {
        // 默认配置的目标不是“最真实”，而是“用户第一次进入页面就能马上跑出结果”。
        WaferConfigUpsertDto dto = defaultConfig();
        // 默认配置构建既会走真实接口，也会被无登录态的单测直接调用，
        // 这里使用 system 用户兜底即可，不应强依赖登录上下文。
        Long uid = SecurityContextUtils.currentUserIdOrSystem();
        WaferConfigVo vo = new WaferConfigVo();
        applyVo(vo, newEntity(dto), uid);
        vo.setDataScope("MINE");
        vo.setEditable(1);
        vo.setDeletable(1);
        vo.setValidationRules(validationRules());
        return vo;
    }

    @Override
    public List<String> validateConfig(WaferConfigUpsertDto dto) {
        // 这里返回 List<String> 而不是直接抛异常，是为了让前端可以把每条校验错误逐项展示出来。
        List<String> errors = new ArrayList<>();
        if (dto == null) {
            errors.add("Config payload is required.");
            return errors;
        }
        if (!StringUtils.hasText(dto.getConfigName())) errors.add("Config name is required.");
        if (!StringUtils.hasText(dto.getLotNo())) errors.add("Lot No is required.");
        if (!StringUtils.hasText(dto.getWaferNo())) errors.add("Wafer No is required.");
        if (dto.getLayerId() == null || dto.getLayerId() <= 0) errors.add("Layer is required.");
        if (!StringUtils.hasText(dto.getMeasurementType())) errors.add("Measurement type is required.");
        if (!StringUtils.hasText(dto.getStage())) errors.add("Stage is required.");
        range(errors, "scannerCorrectionGain", dto.getScannerCorrectionGain(), 0.70, 1.30);
        range(errors, "overlayBaseNm", dto.getOverlayBaseNm(), 1.00, 8.00);
        range(errors, "edgeGradient", dto.getEdgeGradient(), 0.00, 4.00);
        range(errors, "localHotspotStrength", dto.getLocalHotspotStrength(), 0.00, 5.00);
        range(errors, "noiseLevel", dto.getNoiseLevel(), 0.00, 1.00);
        range(errors, "gridStep", dto.getGridStep(), 0.50, 5.00);
        range(errors, "outlierThreshold", dto.getOutlierThreshold(), 2.00, 20.00);
        return errors;
    }

    @Override
    public Page<WaferConfigVo> pageConfigs(WaferConfigQueryDto queryDto) {
        Long uid = SecurityContextUtils.currentUserIdOrThrow();
        // 配置列表统一遵循 demo + mine 的可见性模型：
        // - DEMO: createdBy = 0，所有用户可读
        // - MINE: createdBy = 当前用户，仅本人可读写
        Page<WaferAnalysisConfig> page = new Page<>(normPageNo(queryDto.getPageNo()), normPageSize(queryDto.getPageSize()));
        Page<WaferAnalysisConfig> rows = configMapper.selectPage(page, new LambdaQueryWrapper<WaferAnalysisConfig>()
            .eq(WaferAnalysisConfig::getDeleted, 0)
            .eq(queryDto.getLayerId() != null, WaferAnalysisConfig::getLayerId, queryDto.getLayerId())
            .eq(StringUtils.hasText(queryDto.getStage()), WaferAnalysisConfig::getStage, queryDto.getStage())
            .and(StringUtils.hasText(queryDto.getKeyword()), w -> w
                .like(WaferAnalysisConfig::getConfigName, queryDto.getKeyword())
                .or().like(WaferAnalysisConfig::getConfigNo, queryDto.getKeyword())
                .or().like(WaferAnalysisConfig::getLotNo, queryDto.getKeyword())
                .or().like(WaferAnalysisConfig::getWaferNo, queryDto.getKeyword()))
            .and(w -> {
                if ("DEMO".equalsIgnoreCase(queryDto.getDataScope())) {
                    w.eq(WaferAnalysisConfig::getCreatedBy, 0L);
                } else if ("MINE".equalsIgnoreCase(queryDto.getDataScope())) {
                    w.eq(WaferAnalysisConfig::getCreatedBy, uid);
                } else {
                    w.eq(WaferAnalysisConfig::getCreatedBy, 0L).or().eq(WaferAnalysisConfig::getCreatedBy, uid);
                }
            })
            .orderByAsc(WaferAnalysisConfig::getCreatedBy)
            .orderByDesc(WaferAnalysisConfig::getUpdatedAt)
            .orderByDesc(WaferAnalysisConfig::getId));
        Page<WaferConfigVo> result = new Page<>(rows.getCurrent(), rows.getSize(), rows.getTotal());
        result.setRecords(rows.getRecords().stream().map(entity -> toVo(entity, uid)).toList());
        return result;
    }

    @Override
    public WaferConfigVo getConfig(Long configId) {
        Long uid = SecurityContextUtils.currentUserIdOrThrow();
        return toVo(findVisible(configId, uid), uid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WaferConfigVo createConfig(WaferConfigUpsertDto request) {
        validateOrThrow(request);
        Long uid = SecurityContextUtils.currentUserIdOrThrow();
        WaferAnalysisConfig entity = newEntity(request);
        entity.setConfigNo("CFG-U-" + LocalDateTime.now().format(TS) + "-" + shortId());
        entity.setStatus("ACTIVE");
        configMapper.insert(entity);
        trimExcessUserConfigs(uid);
        return toVo(entity, uid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WaferConfigVo updateConfig(Long configId, WaferConfigUpsertDto request) {
        validateOrThrow(request);
        Long uid = SecurityContextUtils.currentUserIdOrThrow();
        WaferAnalysisConfig entity = findVisible(configId, uid);
        if (isDemo(entity) || !uid.equals(entity.getCreatedBy())) {
            throw new BusinessException("WAFER_CONFIG_FORBIDDEN", "Demo config is read-only.");
        }
        applyUpsert(entity, request);
        configMapper.updateById(entity);
        return toVo(entity, uid);
    }

    @Override
    public void deleteConfig(Long configId) {
        Long uid = SecurityContextUtils.currentUserIdOrThrow();
        WaferAnalysisConfig entity = findVisible(configId, uid);
        if (isDemo(entity) || !uid.equals(entity.getCreatedBy())) {
            throw new BusinessException("WAFER_CONFIG_FORBIDDEN", "Demo config cannot be deleted.");
        }
        configMapper.deleteById(configId);
    }

    @Override
    public WaferAnalysisGenerateResultVo generateAnalysis(WaferAnalysisGenerateRequestDto request) {
        long startedAt = System.currentTimeMillis();
        Long uid = SecurityContextUtils.currentUserIdOrThrow();
        String locale = resolveLocale(request);
        long memoryBeforeMb = usedMemoryMb();
        // Wafer Analysis 的主链路入口：
        // 配置 -> lot/wafer/layer -> run -> 点位 -> KPI 汇总 -> task/result summary。
        //
        // 你可以把下面几行代码理解成一次完整业务流水：
        // 1. 决定这次分析到底使用哪套配置
        // 2. 解析配置所指向的 lot / wafer / layer
        // 3. 创建 measurement_run 作为“本次分析的数据头”
        // 4. 生成高密度 overlay_measurement_point 明细
        // 5. 把明细汇总成 KPI
        // 6. 创建 simulation_task 记录“用户触发过一次分析”
        // 7. 创建 simulation_result_summary 供趋势图和任务摘要使用
        WaferConfigUpsertDto cfg = resolveGenerateConfig(request, uid);
        validateOrThrow(cfg);
        Layer layer = resolveLayer(cfg.getLayerId(), uid);
        String fingerprint = buildAnalysisFingerprint(cfg, layer.getLayerCode());
        ReuseCandidate reuseCandidate = findReusableCandidate(fingerprint);
        WaferAnalysisConfig configToUpdate;
        MeasurementRun run;
        Summary summary;
        SimulationTask task;
        int generatedPoints;
        Long reusedRunId = null;
        Long reusedTaskId = null;
        boolean reuseHit = reuseCandidate != null;

        if (reuseHit) {
            run = reuseCandidate.run();
            summary = deriveSummaryForReuse(reuseCandidate.summary(), run, cfg, locale);
            reusedRunId = run.getId();
            reusedTaskId = reuseCandidate.sourceTaskId();
            task = createReuseTask(cfg, run.getLotId(), run.getLayerId(), uid, run.getId(), summary, fingerprint, reusedTaskId);
            createSummary(task.getId(), run.getWaferId(), run.getLayerId(), run.getId(), uid, summary);
            trimExcessUserTasks(uid);
            generatedPoints = run.getSamplingCount() == null ? 0 : run.getSamplingCount();
            log.info("[wafer-config] generate fingerprint={} reuseHit=true reusedRunId={} reusedTaskId={}",
                fingerprint, reusedRunId, reusedTaskId);
        } else {
            log.info("[wafer-config] generate fingerprint={} reuseHit=false reusedRunId=null reusedTaskId=null", fingerprint);
            Lot lot = resolveLot(cfg, uid);
            Wafer wafer = resolveWafer(cfg, lot.getId());
            run = createRun(cfg, lot.getId(), wafer.getId(), layer.getId(), fingerprint);
            PointStats stats = generatePoints(run.getId(), wafer.getId(), layer.getId(), uid, cfg);
            summary = summarize(stats, cfg.getOutlierThreshold(), cfg.getScannerCorrectionGain(), locale);
            task = createTask(cfg, lot.getId(), layer.getId(), uid, run.getId(), summary, fingerprint);
            createSummary(task.getId(), wafer.getId(), layer.getId(), run.getId(), uid, summary);
            trimExcessUserTasks(uid);
            run.setSamplingCount(stats.count);
            run.setSummaryJson(toSummaryJson(summary));
            runMapper.updateById(run);
            generatedPoints = stats.count;
        }

        configToUpdate = applyConfigTracking(request, cfg, uid, run.getId(), task.getId());
        WaferAnalysisGenerateResultVo vo = buildResultVo(
            request,
            configToUpdate,
            task,
            run,
            summary,
            generatedPoints,
            startedAt,
            reuseHit,
            reusedRunId,
            reusedTaskId
        );
        log.info("[wafer-config] generate locale={} fingerprint={} taskId={} runId={} points={} reuseHit={} reusedRunId={} reusedTaskId={} elapsedMs={} memoryBeforeMb={} memoryAfterMb={}",
            locale, fingerprint, task.getId(), run.getId(), generatedPoints, reuseHit, reusedRunId, reusedTaskId,
            vo.getElapsedMs(), memoryBeforeMb, usedMemoryMb());
        return vo;
    }

    private WaferConfigUpsertDto resolveGenerateConfig(WaferAnalysisGenerateRequestDto request, Long uid) {
        // 这里解决“这次分析到底以哪套配置为准”的问题：
        // - 传了 configId：先加载已保存配置，再叠加页面上的 patch
        // - 只传 config：直接用页面临时配置
        // - 两者都没传：退回默认配置
        if (request != null && request.getConfigId() != null) {
            WaferConfigUpsertDto dto = toUpsert(findVisible(request.getConfigId(), uid));
            if (request.getConfig() != null) {
                merge(dto, request.getConfig());
            }
            return dto;
        }
        if (request != null && request.getConfig() != null) {
            return request.getConfig();
        }
        return defaultConfig();
    }

    private Layer resolveLayer(Long layerId, Long uid) {
        // layer 是分析上下文的一部分。这里不仅检查是否存在，还检查当前用户是否有权使用。
        Layer layer = layerMapper.selectById(layerId);
        if (layer == null || (layer.getDeleted() != null && layer.getDeleted() == 1)) {
            throw new BusinessException("LAYER_NOT_FOUND", "Layer not found.");
        }
        if (layer.getCreatedBy() != null && layer.getCreatedBy() != 0L && !layer.getCreatedBy().equals(uid)) {
            throw new BusinessException("LAYER_FORBIDDEN", "No permission to use this layer.");
        }
        return layer;
    }

    private Lot resolveLot(WaferConfigUpsertDto cfg, Long uid) {
        // lot 的处理分 3 种：
        // 1. 已存在且是 demo：直接复用
        // 2. 已存在且属于当前用户：直接复用
        // 3. 不存在：按当前配置新建一条用户私有 lot
        Lot lot = lotMapper.selectOne(new LambdaQueryWrapper<Lot>()
            .eq(Lot::getLotNo, cfg.getLotNo())
            .eq(Lot::getDeleted, 0)
            .last("LIMIT 1"));
        if (lot != null) {
            if (lot.getIsDemo() != null && lot.getIsDemo() == 1) return lot;
            if (!uid.equals(lot.getOwnerUserId())) {
                throw new BusinessException("LOT_FORBIDDEN", "Lot belongs to another user.");
            }
            return lot;
        }
        Lot created = new Lot();
        created.setLotNo(cfg.getLotNo());
        created.setLotStatus("READY");
        created.setSourceType("USER");
        created.setPriorityLevel("NORMAL");
        created.setWaferCount(1);
        created.setOwnerUserId(uid);
        created.setIsDemo(0);
        created.setRemark("Generated by wafer config");
        lotMapper.insert(created);
        return created;
    }

    private Wafer resolveWafer(WaferConfigUpsertDto cfg, Long lotId) {
        // wafer 和 lot 类似：先尝试按 lot + waferNo 查已有记录；没有则创建。
        Wafer wafer = waferMapper.selectOne(new LambdaQueryWrapper<Wafer>()
            .eq(Wafer::getLotId, lotId)
            .eq(Wafer::getWaferNo, cfg.getWaferNo())
            .eq(Wafer::getDeleted, 0)
            .last("LIMIT 1"));
        if (wafer != null) return wafer;
        Wafer created = new Wafer();
        created.setLotId(lotId);
        created.setWaferNo(cfg.getWaferNo());
        created.setWaferStatus("READY");
        created.setSlotNo(1);
        created.setDiameterMm(new BigDecimal("300"));
        created.setSummaryTagsJson("{\"source\":\"USER_CONFIG_GENERATED\"}");
        waferMapper.insert(created);
        return created;
    }

    private WaferAnalysisConfig findVisible(Long configId, Long uid) {
        // 统一封装“当前用户可见配置”的查询逻辑，避免各个接口重复写 demo/mine 权限判断。
        WaferAnalysisConfig entity = configMapper.selectOne(new LambdaQueryWrapper<WaferAnalysisConfig>()
            .eq(WaferAnalysisConfig::getId, configId)
            .eq(WaferAnalysisConfig::getDeleted, 0)
            .and(w -> w.eq(WaferAnalysisConfig::getCreatedBy, 0L).or().eq(WaferAnalysisConfig::getCreatedBy, uid)));
        if (entity == null) {
            throw new BusinessException("WAFER_CONFIG_NOT_FOUND", "Wafer config not found.");
        }
        return entity;
    }

    private WaferConfigVo toVo(WaferAnalysisConfig entity, Long uid) {
        // 这里除了做字段映射，还顺便给前端补齐权限视图字段。
        // 前端拿到 editable/deletable 后，无需再自己推导 demo 和 mine 的行为差异。
        WaferConfigVo vo = new WaferConfigVo();
        applyVo(vo, entity, uid);
        vo.setDataScope(isDemo(entity) ? "DEMO" : "MINE");
        vo.setEditable(isDemo(entity) ? 0 : 1);
        vo.setDeletable(isDemo(entity) ? 0 : 1);
        vo.setValidationRules(validationRules());
        return vo;
    }

    private void applyVo(WaferConfigVo vo, WaferAnalysisConfig entity, Long uid) {
        vo.setId(entity.getId());
        vo.setConfigNo(entity.getConfigNo());
        vo.setConfigName(entity.getConfigName());
        vo.setDescription(entity.getDescription());
        vo.setLotNo(entity.getLotNo());
        vo.setWaferNo(entity.getWaferNo());
        vo.setLayerId(entity.getLayerId());
        vo.setMeasurementType(entity.getMeasurementType());
        vo.setStage(entity.getStage());
        vo.setScannerCorrectionGain(entity.getScannerCorrectionGain());
        vo.setOverlayBaseNm(entity.getOverlayBaseNm());
        vo.setEdgeGradient(entity.getEdgeGradient());
        vo.setLocalHotspotStrength(entity.getLocalHotspotStrength());
        vo.setNoiseLevel(entity.getNoiseLevel());
        vo.setGridStep(entity.getGridStep());
        vo.setOutlierThreshold(entity.getOutlierThreshold());
        vo.setLastMeasurementRunId(resolveLinkedRunId(entity, uid));
        vo.setLastTaskId(entity.getLastTaskId());
        vo.setUpdatedAt(entity.getUpdatedAt());
    }

    private Long resolveLinkedRunId(WaferAnalysisConfig entity, Long uid) {
        // 某些早期 seed demo config 没有回填 last_measurement_run_id。
        // 这里按 lot/wafer/layer/stage/measurementType 兜底推导一个当前用户可见的已完成 run，
        // 这样前端在多 Wafer 页或工作台里就能真正“选中并使用”这些 demo 配置。
        if (entity.getLastMeasurementRunId() != null) {
            return entity.getLastMeasurementRunId();
        }
        // 默认配置构建、轻量单测等场景不会注入完整 mapper，此时无需做 run 兜底推导。
        if (lotMapper == null || waferMapper == null || runMapper == null) {
            return null;
        }
        List<Lot> lots = lotMapper.selectList(new LambdaQueryWrapper<Lot>()
            .eq(Lot::getLotNo, entity.getLotNo())
            .eq(Lot::getDeleted, 0)
            .orderByAsc(Lot::getId));
        for (Lot lot : lots) {
            if (lot.getIsDemo() != null && lot.getIsDemo() == 0 && lot.getOwnerUserId() != null && !lot.getOwnerUserId().equals(uid)) {
                continue;
            }
            Wafer wafer = waferMapper.selectOne(new LambdaQueryWrapper<Wafer>()
                .eq(Wafer::getLotId, lot.getId())
                .eq(Wafer::getWaferNo, entity.getWaferNo())
                .eq(Wafer::getDeleted, 0)
                .last("LIMIT 1"));
            if (wafer == null) {
                continue;
            }
            MeasurementRun run = runMapper.selectOne(new LambdaQueryWrapper<MeasurementRun>()
                .eq(MeasurementRun::getWaferId, wafer.getId())
                .eq(MeasurementRun::getLayerId, entity.getLayerId())
                .eq(StringUtils.hasText(entity.getStage()), MeasurementRun::getStage, entity.getStage())
                .eq(StringUtils.hasText(entity.getMeasurementType()), MeasurementRun::getMeasurementType, entity.getMeasurementType())
                .eq(MeasurementRun::getStatus, "COMPLETED")
                .eq(MeasurementRun::getDeleted, 0)
                .and(w -> w.eq(MeasurementRun::getCreatedBy, 0L).or().eq(MeasurementRun::getCreatedBy, uid))
                .orderByDesc(MeasurementRun::getUpdatedAt)
                .orderByDesc(MeasurementRun::getId)
                .last("LIMIT 1"));
            if (run != null) {
                return run.getId();
            }
        }
        return null;
    }

    private WaferAnalysisConfig newEntity(WaferConfigUpsertDto dto) {
        // DTO -> Entity 的最薄一层转换，避免 create/update/generate 各自重复映射字段。
        WaferAnalysisConfig entity = new WaferAnalysisConfig();
        applyUpsert(entity, dto);
        return entity;
    }

    private void applyUpsert(WaferAnalysisConfig entity, WaferConfigUpsertDto dto) {
        // 这组参数是规则模型的核心输入：
        // 它们共同决定热图形态、边缘恶化程度、热点强度和噪声水平。
        entity.setConfigName(dto.getConfigName());
        entity.setDescription(dto.getDescription());
        entity.setLotNo(dto.getLotNo());
        entity.setWaferNo(dto.getWaferNo());
        entity.setLayerId(dto.getLayerId());
        entity.setMeasurementType(dto.getMeasurementType());
        entity.setStage(dto.getStage());
        entity.setScannerCorrectionGain(dto.getScannerCorrectionGain());
        entity.setOverlayBaseNm(dto.getOverlayBaseNm());
        entity.setEdgeGradient(dto.getEdgeGradient());
        entity.setLocalHotspotStrength(dto.getLocalHotspotStrength());
        entity.setNoiseLevel(dto.getNoiseLevel());
        entity.setGridStep(dto.getGridStep());
        entity.setOutlierThreshold(dto.getOutlierThreshold());
    }

    private void validateOrThrow(WaferConfigUpsertDto dto) {
        List<String> errors = validateConfig(dto);
        if (!errors.isEmpty()) {
            throw new BusinessException("WAFER_CONFIG_INVALID", String.join("; ", errors));
        }
    }

    private WaferConfigUpsertDto toUpsert(WaferAnalysisConfig entity) {
        WaferConfigUpsertDto dto = new WaferConfigUpsertDto();
        dto.setConfigName(entity.getConfigName());
        dto.setDescription(entity.getDescription());
        dto.setLotNo(entity.getLotNo());
        dto.setWaferNo(entity.getWaferNo());
        dto.setLayerId(entity.getLayerId());
        dto.setMeasurementType(entity.getMeasurementType());
        dto.setStage(entity.getStage());
        dto.setScannerCorrectionGain(entity.getScannerCorrectionGain());
        dto.setOverlayBaseNm(entity.getOverlayBaseNm());
        dto.setEdgeGradient(entity.getEdgeGradient());
        dto.setLocalHotspotStrength(entity.getLocalHotspotStrength());
        dto.setNoiseLevel(entity.getNoiseLevel());
        dto.setGridStep(entity.getGridStep());
        dto.setOutlierThreshold(entity.getOutlierThreshold());
        return dto;
    }

    private void merge(WaferConfigUpsertDto target, WaferConfigUpsertDto patch) {
        // merge 只覆盖 patch 中显式传入的字段，避免配置复用时被空值误清空。
        if (StringUtils.hasText(patch.getConfigName())) target.setConfigName(patch.getConfigName());
        if (patch.getDescription() != null) target.setDescription(patch.getDescription());
        if (StringUtils.hasText(patch.getLotNo())) target.setLotNo(patch.getLotNo());
        if (StringUtils.hasText(patch.getWaferNo())) target.setWaferNo(patch.getWaferNo());
        if (patch.getLayerId() != null) target.setLayerId(patch.getLayerId());
        if (StringUtils.hasText(patch.getMeasurementType())) target.setMeasurementType(patch.getMeasurementType());
        if (StringUtils.hasText(patch.getStage())) target.setStage(patch.getStage());
        if (patch.getScannerCorrectionGain() != null) target.setScannerCorrectionGain(patch.getScannerCorrectionGain());
        if (patch.getOverlayBaseNm() != null) target.setOverlayBaseNm(patch.getOverlayBaseNm());
        if (patch.getEdgeGradient() != null) target.setEdgeGradient(patch.getEdgeGradient());
        if (patch.getLocalHotspotStrength() != null) target.setLocalHotspotStrength(patch.getLocalHotspotStrength());
        if (patch.getNoiseLevel() != null) target.setNoiseLevel(patch.getNoiseLevel());
        if (patch.getGridStep() != null) target.setGridStep(patch.getGridStep());
        if (patch.getOutlierThreshold() != null) target.setOutlierThreshold(patch.getOutlierThreshold());
    }

    private WaferConfigUpsertDto defaultConfig() {
        // 默认配置更偏“演示友好型”：
        // - gridStep 默认 0.50，便于生成高密度热图
        // - scannerCorrectionGain 默认 1.00，作为稳定基线
        WaferConfigUpsertDto dto = new WaferConfigUpsertDto();
        dto.setConfigName("My Wafer Baseline");
        dto.setDescription("Config-driven generated wafer analysis");
        dto.setLotNo("LOT-U-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHH")));
        dto.setWaferNo("W01");
        dto.setLayerId(resolveDefaultLayerId());
        dto.setMeasurementType("OVERLAY");
        dto.setStage("PRE_ETCH");
        dto.setScannerCorrectionGain(new BigDecimal("1.00"));
        dto.setOverlayBaseNm(new BigDecimal("3.20"));
        dto.setEdgeGradient(new BigDecimal("1.30"));
        dto.setLocalHotspotStrength(new BigDecimal("1.00"));
        dto.setNoiseLevel(new BigDecimal("0.15"));
        dto.setGridStep(new BigDecimal("0.50"));
        dto.setOutlierThreshold(new BigDecimal("8.00"));
        return dto;
    }

    private List<WaferConfigValidationRuleVo> validationRules() {
        // 这些规则直接下发给前端展示，减少前后端对参数范围理解不一致的问题。
        return List.of(
            rule("scannerCorrectionGain", "[0.70, 1.30]", "1.00"),
            rule("overlayBaseNm", "[1.00, 8.00]", "3.20"),
            rule("edgeGradient", "[0.00, 4.00]", "1.30"),
            rule("localHotspotStrength", "[0.00, 5.00]", "1.00"),
            rule("noiseLevel", "[0.00, 1.00]", "0.15"),
            rule("gridStep", "[0.50, 5.00]", "0.50"),
            rule("outlierThreshold", "[2.00, 20.00]", "8.00")
        );
    }

    private WaferConfigValidationRuleVo rule(String field, String range, String recommended) {
        WaferConfigValidationRuleVo vo = new WaferConfigValidationRuleVo();
        vo.setField(field);
        vo.setLabel(field);
        vo.setRule(range);
        vo.setRecommended(recommended);
        return vo;
    }

    private void range(List<String> errors, String field, BigDecimal value, double min, double max) {
        if (value == null) errors.add(field + " is required.");
        else if (value.doubleValue() < min || value.doubleValue() > max) errors.add(field + " must be in range [" + min + ", " + max + "].");
    }

    private boolean isDemo(WaferAnalysisConfig entity) {
        return entity.getCreatedBy() != null && entity.getCreatedBy() == 0L;
    }

    private long normPageNo(Long pageNo) { return pageNo == null || pageNo < 1 ? 1L : pageNo; }
    private long normPageSize(Long pageSize) { return 10L; }
    private String shortId() { return UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase(Locale.ROOT); }
    private BigDecimal dec(double value, int scale) { return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP); }
    private String pct(BigDecimal rate) { return rate.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP) + "%"; }
    private String toSummaryJson(Summary summary) {
        // 这里保留一份轻量 JSON 快照，方便 run/task 直接回显核心结论。
        // 正式结构化 KPI 仍然会单独写入 simulation_result_summary 表。
        return "{\"meanOverlay\":" + summary.meanOverlay + ",\"maxOverlay\":" + summary.maxOverlay + ",\"p95Overlay\":" + summary.p95Overlay
            + ",\"stdOverlay\":" + summary.stdOverlay + ",\"passRate\":" + summary.passRate + ",\"overallQuality\":\"" + summary.overallQuality
            + "\",\"summaryText\":\"" + summary.summaryText.replace("\"", "'") + "\"}";
    }

    private MeasurementRun createRun(WaferConfigUpsertDto cfg, Long lotId, Long waferId, Long layerId, String analysisFingerprint) {
        // run 是“数据头记录”：
        // 点位、热图、散点图、直方图最终都是围绕这条 measurement_run 展开的。
        MeasurementRun run = new MeasurementRun();
        run.setRunNo("MRG-" + LocalDateTime.now().format(TS) + "-" + shortId());
        run.setLotId(lotId);
        run.setWaferId(waferId);
        run.setLayerId(layerId);
        run.setMeasurementType(cfg.getMeasurementType());
        run.setStage(cfg.getStage());
        run.setSourceType("USER_GENERATED");
        run.setToolName("CONFIG_GENERATOR");
        run.setSamplingCount(0);
        run.setMeasurementContextJson("{\"axisRange\":\"0-250\",\"circleCenter\":\"125,125\",\"circleRadius\":\"125\",\"keyParameter\":\"scannerCorrectionGain\"}");
        run.setAnalysisFingerprint(analysisFingerprint);
        run.setStatus("COMPLETED");
        runMapper.insert(run);
        return run;
    }

    private PointStats generatePoints(Long runId, Long waferId, Long layerId, Long uid, WaferConfigUpsertDto cfg) {
        long startedAt = System.currentTimeMillis();
        PointStats stats = new PointStats();
        double step = cfg.getGridStep().doubleValue();
        double gain = cfg.getScannerCorrectionGain().doubleValue();
        double base = cfg.getOverlayBaseNm().doubleValue();
        double edgeWeight = cfg.getEdgeGradient().doubleValue();
        double hotspotStrength = cfg.getLocalHotspotStrength().doubleValue();
        double noiseLevel = cfg.getNoiseLevel().doubleValue();
        double outlierThreshold = cfg.getOutlierThreshold().doubleValue();
        int parallelism = Math.max(1, analysisExecutionProperties.getWaferGenerateParallelism());
        List<Future<PointChunkResult>> futures = new ArrayList<>(parallelism);
        for (int slice = 0; slice < parallelism; slice++) {
            final int sliceIndex = slice;
            futures.add(waferPointGenerationExecutor.submit(() -> buildPointChunk(
                runId, waferId, layerId, uid, step, gain, base, edgeWeight, hotspotStrength, noiseLevel, outlierThreshold, sliceIndex, parallelism
            )));
        }
        List<OverlayMeasurementPoint> pending = new ArrayList<>(8192);
        for (Future<PointChunkResult> future : futures) {
            try {
                PointChunkResult chunk = future.get(45, TimeUnit.SECONDS);
                pending.addAll(chunk.points());
                stats.merge(chunk.stats());
            } catch (Exception ex) {
                throw new BusinessException("WAFER_POINT_GENERATION_FAILED", "Parallel point generation failed: " + ex.getMessage());
            }
        }
        pending.sort(Comparator.comparing(OverlayMeasurementPoint::getTargetCode));
        batchInsertPoints(pending);
        log.info("[wafer-config] dense-generation runId={} waferId={} layerId={} points={} parallelism={} elapsedMs={} memoryUsedMb={}",
            runId, waferId, layerId, stats.count, parallelism, System.currentTimeMillis() - startedAt, usedMemoryMb());
        return stats;
    }

    private PointChunkResult buildPointChunk(
        Long runId,
        Long waferId,
        Long layerId,
        Long uid,
        double step,
        double gain,
        double base,
        double edgeWeight,
        double hotspotStrength,
        double noiseLevel,
        double outlierThreshold,
        int sliceIndex,
        int slices
    ) {
        List<OverlayMeasurementPoint> points = new ArrayList<>(1024);
        PointStats stats = new PointStats();
        int localSeq = 0;
        for (double x = sliceIndex * step; x <= 250 + 1e-9; x += step * slices) {
            for (double y = 0; y <= 250 + 1e-9; y += step) {
                double nx = (x - 125d) / 125d;
                double ny = (y - 125d) / 125d;
                double r = Math.sqrt(nx * nx + ny * ny);
                if (r > 1d) {
                    continue;
                }
                double wave = Math.sin(nx * Math.PI * 4) + Math.cos(ny * Math.PI * 3) + Math.sin((nx - ny) * Math.PI * 2) * 0.6;
                double hotspot = Math.exp(-((x - 185) * (x - 185) + (y - 70) * (y - 70)) / 1800d)
                    - Math.exp(-((x - 65) * (x - 65) + (y - 185) * (y - 185)) / 2200d);
                double noise = noiseLevel * (Math.sin(x * 0.13 + y * 0.07) + Math.cos(x * 0.11 - y * 0.09)) * 0.5;
                double edge = edgeWeight * r * r;
                double ox = base * gain * (0.58 * nx + 0.24 * wave) + edge * nx + hotspotStrength * hotspot + noise;
                double oy = base * gain * (0.52 * ny - 0.20 * wave) + edge * ny - hotspotStrength * hotspot * 0.75 - noise * 0.8;
                double mag = Math.sqrt(ox * ox + oy * oy);
                int outlier = (mag > outlierThreshold || (r > 0.9 && mag > outlierThreshold * 0.85)) ? 1 : 0;
                OverlayMeasurementPoint p = new OverlayMeasurementPoint();
                p.setId(IdWorker.getId());
                p.setMeasurementRunId(runId);
                p.setWaferId(waferId);
                p.setLayerId(layerId);
                p.setTargetCode("P" + String.format(Locale.ROOT, "%02d%06d", sliceIndex, ++localSeq));
                p.setXCoord(dec(x, 4));
                p.setYCoord(dec(y, 4));
                p.setOverlayX(dec(ox, 6));
                p.setOverlayY(dec(oy, 6));
                p.setOverlayMagnitude(dec(mag, 6));
                p.setResidualValue(dec(Math.abs(wave) * 0.18 + Math.abs(hotspot) * 0.22 + noiseLevel * 0.1, 6));
                p.setFocusValue(dec(0.04 + nx * 0.02 * gain + hotspot * 0.01, 6));
                p.setDoseValue(dec(42 + (gain - 1) * 8 + ny * 0.5 + hotspot * 0.3, 6));
                p.setConfidence(dec(Math.max(0.85, 0.99 - r * 0.08 - noiseLevel * 0.05), 4));
                p.setIsOutlier(outlier);
                p.setCreatedBy(uid);
                points.add(p);
                stats.push(mag, outlier == 1, r > 0.85 && mag > 6.0);
            }
        }
        return new PointChunkResult(points, stats);
    }

    private void batchInsertPoints(List<OverlayMeasurementPoint> points) {
        if (points.isEmpty()) {
            return;
        }
        // 海量点位插入直接走 JDBC 批量写库，避免逐条 insert 带来的额外 ORM 开销。
        // 当前 batchSize 取 2000，是“实现优先”下比较稳妥的折中值，后续可以继续压测优化。
        String sql = """
            INSERT INTO overlay_measurement_point
            (id, measurement_run_id, wafer_id, layer_id, mark_id, target_code, x_coord, y_coord, overlay_x, overlay_y,
             overlay_magnitude, residual_value, focus_value, dose_value, confidence, is_outlier, extra_metrics_json, created_at, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?)
            """;
        final int batchSize = 2000;
        for (int i = 0; i < points.size(); i += batchSize) {
            List<OverlayMeasurementPoint> segment = points.subList(i, Math.min(points.size(), i + batchSize));
            jdbcTemplate.batchUpdate(sql, segment, segment.size(), (ps, p) -> {
                ps.setLong(1, p.getId());
                ps.setLong(2, p.getMeasurementRunId());
                ps.setLong(3, p.getWaferId());
                ps.setLong(4, p.getLayerId());
                ps.setObject(5, p.getMarkId());
                ps.setString(6, p.getTargetCode());
                ps.setBigDecimal(7, p.getXCoord());
                ps.setBigDecimal(8, p.getYCoord());
                ps.setBigDecimal(9, p.getOverlayX());
                ps.setBigDecimal(10, p.getOverlayY());
                ps.setBigDecimal(11, p.getOverlayMagnitude());
                ps.setBigDecimal(12, p.getResidualValue());
                ps.setBigDecimal(13, p.getFocusValue());
                ps.setBigDecimal(14, p.getDoseValue());
                ps.setBigDecimal(15, p.getConfidence());
                ps.setInt(16, p.getIsOutlier() == null ? 0 : p.getIsOutlier());
                ps.setString(17, p.getExtraMetricsJson());
                ps.setLong(18, p.getCreatedBy() == null ? 0L : p.getCreatedBy());
            });
        }
    }

    private Long resolveDefaultLayerId() {
        Layer layer = layerMapper.selectOne(new LambdaQueryWrapper<Layer>()
            .eq(Layer::getDeleted, 0)
            .orderByAsc(Layer::getSequenceNo, Layer::getId)
            .last("LIMIT 1"));
        if (layer == null) {
            throw new BusinessException("LAYER_NOT_FOUND", "No available layer found.");
        }
        return layer.getId();
    }

    private Summary summarize(PointStats stats, BigDecimal outlierThreshold, BigDecimal gain, String locale) {
        Summary s = new Summary();
        // 将海量点位压缩成页面和任务列表更容易消费的 KPI 汇总。
        // 这里聚合的结果会同时被：
        // - generate 接口返回给前端
        // - run.summaryJson 使用
        // - task.resultSummaryJson 使用
        // - simulation_result_summary 结构化落库
        s.meanOverlay = dec(stats.mean(), 4);
        s.maxOverlay = dec(stats.max, 4);
        s.p95Overlay = dec(stats.p95(), 4);
        s.stdOverlay = dec(stats.std(), 4);
        s.passRate = dec(stats.passRate(outlierThreshold.doubleValue() * 0.9), 4);
        double outlierRatio = stats.count == 0 ? 0 : (double) stats.outlierCount / stats.count;
        double edgeRatio = stats.count == 0 ? 0 : (double) stats.edgeRiskCount / stats.count;
        double gainShift = Math.abs(gain.doubleValue() - 1.0);
        s.overallQuality = s.passRate.doubleValue() >= 0.95 && s.p95Overlay.doubleValue() <= outlierThreshold.doubleValue() ? "GOOD"
            : (s.passRate.doubleValue() >= 0.85 ? "WATCH" : "POOR");
        s.overlayStability = s.stdOverlay.doubleValue() <= 1.6 ? "STABLE" : (s.stdOverlay.doubleValue() <= 2.8 ? "MEDIUM" : "UNSTABLE");
        s.edgeRisk = edgeRatio <= 0.05 ? "LOW" : (edgeRatio <= 0.12 ? "MEDIUM" : "HIGH");
        s.outlierDensity = outlierRatio <= 0.02 ? "LOW" : (outlierRatio <= 0.06 ? "MEDIUM" : "HIGH");
        s.parameterSensitivity = gainShift <= 0.05 ? "LOW" : (gainShift <= 0.12 ? "MEDIUM" : "HIGH");
        s.recommendedAction = "GOOD".equals(s.overallQuality)
            ? "Keep current setting and monitor edge trend."
            : ("WATCH".equals(s.overallQuality)
                ? "Tune scannerCorrectionGain slightly toward 1.0 and re-run."
                : "Reduce edgeGradient and gain, then investigate hotspot area.");
        s.summaryText = buildSummaryText(locale, s, gain, outlierThreshold);
        return s;
    }

    private String buildSummaryText(String locale, Summary s, BigDecimal gain, BigDecimal outlierThreshold) {
        // 通俗总结面向非工艺用户，所以除了结论，还会解释 KPI 的含义与风险。
        if (locale != null && locale.toLowerCase(Locale.ROOT).startsWith("zh")) {
            return "总体质量评估：" + qualityZh(s.overallQuality) + "。"
                + "关键指标方面，通过率为 " + pct(s.passRate) + "，P95 Overlay 为 " + s.p95Overlay + "nm，标准差为 " + s.stdOverlay + "nm。"
                + "稳定性判定为" + stabilityZh(s.overlayStability) + "，边缘风险为" + riskZh(s.edgeRisk) + "，离群密度为" + riskZh(s.outlierDensity) + "。"
                + "参数影响解释：scannerCorrectionGain 当前值 " + gain + "，用于放大或抑制整片 wafer 的空间偏移场；其敏感度判定为"
                + riskZh(s.parameterSensitivity) + "。"
                + "术语说明：P95 表示 95% 点位不超过该值；离群密度表示异常点占比；边缘风险表示 wafer 边缘区域偏移恶化概率。"
                + "风险解读：当前阈值基线为 " + outlierThreshold + "nm，若持续高于该水平，后续工艺窗口会收窄并提升良率波动风险。"
                + "建议动作：" + actionZh(s.overallQuality);
        }
        return "Overall quality assessment: " + s.overallQuality.toLowerCase(Locale.ROOT) + ". "
            + "Key KPIs: pass rate " + pct(s.passRate) + ", P95 overlay " + s.p95Overlay + " nm, and standard deviation "
            + s.stdOverlay + " nm. "
            + "Stability is " + s.overlayStability.toLowerCase(Locale.ROOT) + ", edge risk is "
            + s.edgeRisk.toLowerCase(Locale.ROOT) + ", and outlier density is " + s.outlierDensity.toLowerCase(Locale.ROOT) + ". "
            + "Parameter interpretation: scannerCorrectionGain=" + gain
            + " controls how strongly the spatial distortion field is amplified across the wafer; current sensitivity is "
            + s.parameterSensitivity.toLowerCase(Locale.ROOT) + ". "
            + "Term explanation: P95 means 95% of points are below this value; outlier density is the abnormal-point ratio; "
            + "edge risk indicates the probability of stronger deviation near wafer boundary. "
            + "Risk interpretation: threshold baseline is " + outlierThreshold
            + " nm; sustained values above this level usually narrow process window and increase yield fluctuation risk. "
            + "Recommended action: " + s.recommendedAction;
    }

    private String resolveLocale(WaferAnalysisGenerateRequestDto request) {
        if (request == null || !StringUtils.hasText(request.getLocale())) {
            return "en-US";
        }
        return request.getLocale();
    }

    private long usedMemoryMb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }

    private String qualityZh(String quality) {
        return switch (quality) {
            case "GOOD" -> "良好";
            case "WATCH" -> "可控但需关注";
            default -> "偏高风险";
        };
    }

    private String stabilityZh(String stability) {
        return switch (stability) {
            case "STABLE" -> "稳定";
            case "MEDIUM" -> "中等稳定";
            default -> "波动偏大";
        };
    }

    private String riskZh(String level) {
        return switch (level) {
            case "LOW" -> "低";
            case "MEDIUM" -> "中";
            case "HIGH" -> "高";
            default -> level;
        };
    }

    private String actionZh(String quality) {
        return switch (quality) {
            case "GOOD" -> "保持当前参数，同时持续观察边缘区域趋势。";
            case "WATCH" -> "建议将 scannerCorrectionGain 小幅调回 1.0 附近后复跑分析。";
            default -> "建议先降低边缘梯度和增益，再重点排查热点区域。";
        };
    }

    private SimulationTask createTask(
        WaferConfigUpsertDto cfg,
        Long lotId,
        Long layerId,
        Long uid,
        Long runId,
        Summary summary,
        String fingerprint
    ) {
        // task 记录“用户发起了一次分析”；run 记录“这次分析生成了哪些数据”。
        // 当前设计里 task 和 run 在业务上强相关，但没有强行做数据库一对一外键绑定，
        // 而是通过 summary 和 executionContextJson 等方式建立关联。
        SimulationTask task = new SimulationTask();
        task.setTaskNo("TSK-WAFER-" + LocalDateTime.now().format(TS) + "-" + shortId());
        task.setTaskName("Wafer Config Analysis - " + cfg.getConfigName());
        task.setLotId(lotId);
        task.setLayerId(layerId);
        task.setScenarioType("WAFER_CONFIG_ANALYSIS");
        task.setStatus("SUCCESS");
        task.setPriorityLevel("NORMAL");
        task.setIdempotencyKey("WAFER_CONFIG:" + fingerprint);
        task.setInputSnapshotJson(
            "{\"configName\":\"" + cfg.getConfigName() + "\",\"lotNo\":\"" + cfg.getLotNo() + "\",\"waferNo\":\""
                + cfg.getWaferNo() + "\",\"layerId\":" + cfg.getLayerId() + ",\"scannerCorrectionGain\":"
                + cfg.getScannerCorrectionGain() + ",\"gridStep\":" + cfg.getGridStep() + "}"
        );
        task.setExecutionContextJson("{\"measurementRunId\":" + runId + ",\"analysisFingerprint\":\"" + fingerprint + "\"}");
        task.setResultSummaryJson(toSummaryJson(summary));
        task.setRequestedBy(uid);
        taskMapper.insert(task);
        return task;
    }

    private SimulationTask createReuseTask(
        WaferConfigUpsertDto cfg,
        Long lotId,
        Long layerId,
        Long uid,
        Long reusedRunId,
        Summary summary,
        String fingerprint,
        Long reusedTaskId
    ) {
        SimulationTask task = new SimulationTask();
        task.setTaskNo("TSK-WAFER-" + LocalDateTime.now().format(TS) + "-" + shortId());
        task.setTaskName("Wafer Config Analysis (Reused) - " + cfg.getConfigName());
        task.setLotId(lotId);
        task.setLayerId(layerId);
        task.setScenarioType("WAFER_CONFIG_ANALYSIS");
        task.setStatus("SUCCESS");
        task.setPriorityLevel("NORMAL");
        task.setIdempotencyKey("WAFER_CONFIG_REUSED:" + fingerprint);
        task.setInputSnapshotJson(
            "{\"configName\":\"" + cfg.getConfigName() + "\",\"lotNo\":\"" + cfg.getLotNo() + "\",\"waferNo\":\""
                + cfg.getWaferNo() + "\",\"layerId\":" + cfg.getLayerId() + ",\"scannerCorrectionGain\":"
                + cfg.getScannerCorrectionGain() + ",\"gridStep\":" + cfg.getGridStep() + ",\"reuse\":true}"
        );
        task.setExecutionContextJson("{\"measurementRunId\":" + reusedRunId + ",\"analysisFingerprint\":\"" + fingerprint
            + "\",\"reusedTaskId\":" + (reusedTaskId == null ? "null" : reusedTaskId.toString()) + "}");
        task.setResultSummaryJson(toSummaryJson(summary));
        task.setRequestedBy(uid);
        taskMapper.insert(task);
        return task;
    }

    private String buildAnalysisFingerprint(WaferConfigUpsertDto cfg, String layerCode) {
        String canonical = String.join("|",
            "fingerprintVersion=" + FINGERPRINT_VERSION,
            "layerCode=" + normalizeToken(layerCode),
            "measurementType=" + normalizeToken(cfg.getMeasurementType()),
            "stage=" + normalizeToken(cfg.getStage()),
            "scannerCorrectionGain=" + normalizeDecimal(cfg.getScannerCorrectionGain()),
            "edgeGradient=" + normalizeDecimal(cfg.getEdgeGradient()),
            "overlayBaseNm=" + normalizeDecimal(cfg.getOverlayBaseNm()),
            "localHotspotStrength=" + normalizeDecimal(cfg.getLocalHotspotStrength()),
            "noiseLevel=" + normalizeDecimal(cfg.getNoiseLevel()),
            "gridStep=" + normalizeDecimal(cfg.getGridStep()),
            "outlierThreshold=" + normalizeDecimal(cfg.getOutlierThreshold())
        );
        return sha256Hex(canonical);
    }

    private ReuseCandidate findReusableCandidate(String fingerprint) {
        MeasurementRun reusableRun = runMapper.selectOne(new LambdaQueryWrapper<MeasurementRun>()
            .eq(MeasurementRun::getAnalysisFingerprint, fingerprint)
            .eq(MeasurementRun::getStatus, "COMPLETED")
            .eq(MeasurementRun::getDeleted, 0)
            .orderByDesc(MeasurementRun::getUpdatedAt)
            .orderByDesc(MeasurementRun::getId)
            .last("LIMIT 1"));
        if (reusableRun == null) {
            return null;
        }
        SimulationResultSummary summary = summaryMapper.selectOne(new LambdaQueryWrapper<SimulationResultSummary>()
            .eq(SimulationResultSummary::getMeasurementRunId, reusableRun.getId())
            .orderByDesc(SimulationResultSummary::getCreatedAt)
            .orderByDesc(SimulationResultSummary::getId)
            .last("LIMIT 1"));
        Long sourceTaskId = summary == null ? null : summary.getTaskId();
        return new ReuseCandidate(reusableRun, summary, sourceTaskId);
    }

    private Summary deriveSummaryForReuse(
        SimulationResultSummary resultSummary,
        MeasurementRun run,
        WaferConfigUpsertDto cfg,
        String locale
    ) {
        Summary summary = new Summary();
        summary.meanOverlay = resultSummary != null && resultSummary.getMeanOverlay() != null ? resultSummary.getMeanOverlay().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        summary.maxOverlay = resultSummary != null && resultSummary.getMaxOverlay() != null ? resultSummary.getMaxOverlay().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        summary.p95Overlay = resultSummary != null && resultSummary.getP95Overlay() != null ? resultSummary.getP95Overlay().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        summary.stdOverlay = resultSummary != null && resultSummary.getStdOverlay() != null ? resultSummary.getStdOverlay().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        summary.passRate = resultSummary != null && resultSummary.getPassRate() != null ? resultSummary.getPassRate().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        summary.overallQuality = summary.passRate.doubleValue() >= 0.95 && summary.p95Overlay.doubleValue() <= cfg.getOutlierThreshold().doubleValue() ? "GOOD"
            : (summary.passRate.doubleValue() >= 0.85 ? "WATCH" : "POOR");
        summary.overlayStability = summary.stdOverlay.doubleValue() <= 1.6 ? "STABLE" : (summary.stdOverlay.doubleValue() <= 2.8 ? "MEDIUM" : "UNSTABLE");
        summary.edgeRisk = resultSummary != null && "HIGH".equalsIgnoreCase(resultSummary.getWarningLevel()) ? "HIGH"
            : (resultSummary != null && "MEDIUM".equalsIgnoreCase(resultSummary.getWarningLevel()) ? "MEDIUM" : "LOW");
        summary.outlierDensity = "HIGH".equals(summary.edgeRisk) ? "HIGH" : ("MEDIUM".equals(summary.edgeRisk) ? "MEDIUM" : "LOW");
        summary.parameterSensitivity = Math.abs(cfg.getScannerCorrectionGain().doubleValue() - 1.0) <= 0.05 ? "LOW"
            : (Math.abs(cfg.getScannerCorrectionGain().doubleValue() - 1.0) <= 0.12 ? "MEDIUM" : "HIGH");
        summary.recommendedAction = "GOOD".equals(summary.overallQuality)
            ? "Keep current setting and monitor edge trend."
            : ("WATCH".equals(summary.overallQuality)
                ? "Tune scannerCorrectionGain slightly toward 1.0 and re-run."
                : "Reduce edgeGradient and gain, then investigate hotspot area.");
        summary.summaryText = buildSummaryText(locale, summary, cfg.getScannerCorrectionGain(), cfg.getOutlierThreshold());
        if (!StringUtils.hasText(run.getSummaryJson())) {
            run.setSummaryJson(toSummaryJson(summary));
            runMapper.updateById(run);
        }
        return summary;
    }

    private WaferAnalysisConfig applyConfigTracking(
        WaferAnalysisGenerateRequestDto request,
        WaferConfigUpsertDto cfg,
        Long uid,
        Long runId,
        Long taskId
    ) {
        WaferAnalysisConfig configToUpdate = null;
        if (request != null && request.getConfigId() != null) {
            configToUpdate = findVisible(request.getConfigId(), uid);
            if (!isDemo(configToUpdate) && uid.equals(configToUpdate.getCreatedBy())) {
                applyUpsert(configToUpdate, cfg);
                configToUpdate.setLastMeasurementRunId(runId);
                configToUpdate.setLastTaskId(taskId);
                configMapper.updateById(configToUpdate);
            }
        } else if (request != null && Boolean.TRUE.equals(request.getSaveAsConfig())) {
            configToUpdate = newEntity(cfg);
            configToUpdate.setConfigNo("CFG-U-" + LocalDateTime.now().format(TS) + "-" + shortId());
            configToUpdate.setStatus("ACTIVE");
            configToUpdate.setLastMeasurementRunId(runId);
            configToUpdate.setLastTaskId(taskId);
            configMapper.insert(configToUpdate);
            trimExcessUserConfigs(uid);
        }
        return configToUpdate;
    }

    private WaferAnalysisGenerateResultVo buildResultVo(
        WaferAnalysisGenerateRequestDto request,
        WaferAnalysisConfig configToUpdate,
        SimulationTask task,
        MeasurementRun run,
        Summary summary,
        int generatedPoints,
        long startedAt,
        boolean reuseHit,
        Long reusedRunId,
        Long reusedTaskId
    ) {
        WaferAnalysisGenerateResultVo vo = new WaferAnalysisGenerateResultVo();
        vo.setConfigId(configToUpdate != null ? configToUpdate.getId() : (request == null ? null : request.getConfigId()));
        vo.setTaskId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setMeasurementRunId(run.getId());
        vo.setMeasurementRunNo(run.getRunNo());
        vo.setLotId(run.getLotId());
        vo.setWaferId(run.getWaferId());
        vo.setLayerId(run.getLayerId());
        vo.setGeneratedPoints(generatedPoints);
        vo.setMeanOverlay(summary.meanOverlay);
        vo.setP95Overlay(summary.p95Overlay);
        vo.setMaxOverlay(summary.maxOverlay);
        vo.setStdOverlay(summary.stdOverlay);
        vo.setPassRate(summary.passRate);
        vo.setOverallQuality(summary.overallQuality);
        vo.setOverlayStability(summary.overlayStability);
        vo.setEdgeRisk(summary.edgeRisk);
        vo.setOutlierDensity(summary.outlierDensity);
        vo.setParameterSensitivity(summary.parameterSensitivity);
        vo.setRecommendedAction(summary.recommendedAction);
        vo.setSummaryText(summary.summaryText);
        vo.setReuseHit(reuseHit);
        vo.setReusedRunId(reusedRunId);
        vo.setReusedTaskId(reusedTaskId);
        vo.setValidationErrors(List.of());
        vo.setElapsedMs(System.currentTimeMillis() - startedAt);
        return vo;
    }

    private void trimExcessUserTasks(Long uid) {
        List<SimulationTask> userTasks = taskMapper.selectList(new LambdaQueryWrapper<SimulationTask>()
            .eq(SimulationTask::getCreatedBy, uid)
            .eq(SimulationTask::getDeleted, 0)
            .orderByAsc(SimulationTask::getCreatedAt)
            .orderByAsc(SimulationTask::getId));
        int overflow = userTasks.size() - MAX_USER_TASKS;
        if (overflow <= 0) {
            return;
        }
        List<Long> toDeleteIds = userTasks.stream().limit(overflow).map(SimulationTask::getId).toList();
        taskMapper.deleteBatchIds(toDeleteIds);
        log.info("[wafer-config] trim tasks uid={} removed={}", uid, toDeleteIds.size());
    }

    private void trimExcessUserConfigs(Long uid) {
        List<WaferAnalysisConfig> userConfigs = configMapper.selectList(new LambdaQueryWrapper<WaferAnalysisConfig>()
            .eq(WaferAnalysisConfig::getCreatedBy, uid)
            .eq(WaferAnalysisConfig::getDeleted, 0)
            .orderByAsc(WaferAnalysisConfig::getCreatedAt)
            .orderByAsc(WaferAnalysisConfig::getId));
        int overflow = userConfigs.size() - MAX_USER_CONFIGS;
        if (overflow <= 0) {
            return;
        }
        List<Long> toDeleteIds = userConfigs.stream().limit(overflow).map(WaferAnalysisConfig::getId).toList();
        configMapper.deleteBatchIds(toDeleteIds);
        log.info("[wafer-config] trim configs uid={} removed={}", uid, toDeleteIds.size());
    }

    private String normalizeToken(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "_";
    }

    private String normalizeDecimal(BigDecimal value) {
        return value == null ? "0.000000" : value.setScale(6, RoundingMode.HALF_UP).toPlainString();
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new BusinessException("FINGERPRINT_HASH_FAILED", "Failed to hash analysis fingerprint.");
        }
    }

    private void createSummary(Long taskId, Long waferId, Long layerId, Long runId, Long uid, Summary summary) {
        // 结构化 KPI 汇总表，后续趋势图、任务摘要和比较页面优先读取这张表。
        SimulationResultSummary row = new SimulationResultSummary();
        row.setTaskId(taskId);
        row.setWaferId(waferId);
        row.setLayerId(layerId);
        row.setMeasurementRunId(runId);
        row.setMeanOverlay(summary.meanOverlay);
        row.setMaxOverlay(summary.maxOverlay);
        row.setMinOverlay(BigDecimal.ZERO);
        row.setStdOverlay(summary.stdOverlay);
        row.setP95Overlay(summary.p95Overlay);
        row.setPassRate(summary.passRate);
        row.setPassFlag("GOOD".equals(summary.overallQuality) ? 1 : 0);
        row.setWarningLevel("GOOD".equals(summary.overallQuality) ? "LOW" : ("WATCH".equals(summary.overallQuality) ? "MEDIUM" : "HIGH"));
        row.setChartSnapshotJson("{\"summaryText\":\"" + summary.summaryText.replace("\"", "'") + "\"}");
        row.setCreatedBy(uid);
        summaryMapper.insert(row);
    }

    private static class PointStats {
        // PointStats 是“点位级统计累加器”：
        // generatePoints 在生成每个点时，顺手把后续汇总需要的统计量积累起来，
        // 这样 summarize() 就不需要再次全表扫描数据库。
        int count;
        int outlierCount;
        int edgeRiskCount;
        double sum;
        double sumSquare;
        double max;
        final List<Double> values = new ArrayList<>();

        void push(double value, boolean outlier, boolean edgeRisk) {
            // 这里维护的是在线统计量：
            // count/sum/sumSquare 用于均值与标准差，
            // values 用于后续计算 p95，
            // outlier / edgeRisk 用于风险等级判断。
            count++;
            sum += value;
            sumSquare += value * value;
            max = Math.max(max, value);
            values.add(value);
            if (outlier) outlierCount++;
            if (edgeRisk) edgeRiskCount++;
        }

        void merge(PointStats other) {
            if (other == null || other.count == 0) {
                return;
            }
            count += other.count;
            outlierCount += other.outlierCount;
            edgeRiskCount += other.edgeRiskCount;
            sum += other.sum;
            sumSquare += other.sumSquare;
            max = Math.max(max, other.max);
            values.addAll(other.values);
        }

        double mean() { return count == 0 ? 0 : sum / count; }

        double std() {
            if (count == 0) return 0;
            double mean = mean();
            return Math.sqrt(Math.max(0, (sumSquare / count) - mean * mean));
        }

        double p95() {
            if (values.isEmpty()) return 0;
            values.sort(Comparator.naturalOrder());
            int index = Math.max(0, (int) Math.ceil(values.size() * 0.95) - 1);
            return values.get(index);
        }

        double passRate(double threshold) {
            if (values.isEmpty()) return 0;
            long passed = values.stream().filter(v -> v <= threshold).count();
            return (double) passed / values.size();
        }
    }

    private record PointChunkResult(List<OverlayMeasurementPoint> points, PointStats stats) {}

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

    private static class Summary {
        // Summary 是 run 级别的统一结果对象。
        // 一个对象同时承载结构化 KPI、业务等级判断和面向用户的总结文案。
        BigDecimal meanOverlay;
        BigDecimal maxOverlay;
        BigDecimal p95Overlay;
        BigDecimal stdOverlay;
        BigDecimal passRate;
        String overallQuality;
        String overlayStability;
        String edgeRisk;
        String outlierDensity;
        String parameterSensitivity;
        String recommendedAction;
        String summaryText;
    }

    private record ReuseCandidate(MeasurementRun run, SimulationResultSummary summary, Long sourceTaskId) {}
}
