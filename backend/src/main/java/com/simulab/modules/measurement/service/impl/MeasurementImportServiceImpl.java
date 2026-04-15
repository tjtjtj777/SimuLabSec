package com.simulab.modules.measurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulab.common.exception.BusinessException;
import com.simulab.common.security.SecurityContextUtils;
import com.simulab.modules.fileimport.entity.ImportFileRecord;
import com.simulab.modules.fileimport.mapper.ImportFileRecordMapper;
import com.simulab.modules.layer.entity.Layer;
import com.simulab.modules.layer.mapper.LayerMapper;
import com.simulab.modules.lot.entity.Lot;
import com.simulab.modules.lot.mapper.LotMapper;
import com.simulab.modules.measurement.dto.WaferAnalysisImportConfigDto;
import com.simulab.modules.measurement.dto.WaferImportFieldMappingDto;
import com.simulab.modules.measurement.entity.MeasurementRun;
import com.simulab.modules.measurement.mapper.MeasurementRunMapper;
import com.simulab.modules.measurement.service.MeasurementImportService;
import com.simulab.modules.measurement.vo.WaferAnalysisImportResultVo;
import com.simulab.modules.overlayanalysis.entity.OverlayMeasurementPoint;
import com.simulab.modules.overlayanalysis.mapper.OverlayMeasurementPointMapper;
import com.simulab.modules.wafer.entity.Wafer;
import com.simulab.modules.wafer.mapper.WaferMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MeasurementImportServiceImpl implements MeasurementImportService {

    private static final Logger log = LoggerFactory.getLogger(MeasurementImportServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final BigDecimal AXIS_MAX = new BigDecimal("250");
    private static final BigDecimal CIRCLE_CENTER = new BigDecimal("125");
    private static final BigDecimal CIRCLE_RADIUS = new BigDecimal("125");
    private static final long MAX_IMPORT_FILE_SIZE_BYTES = 10L * 1024L * 1024L;

    private final LotMapper lotMapper;
    private final WaferMapper waferMapper;
    private final LayerMapper layerMapper;
    private final MeasurementRunMapper measurementRunMapper;
    private final OverlayMeasurementPointMapper overlayMeasurementPointMapper;
    private final ImportFileRecordMapper importFileRecordMapper;

    public MeasurementImportServiceImpl(
        LotMapper lotMapper,
        WaferMapper waferMapper,
        LayerMapper layerMapper,
        MeasurementRunMapper measurementRunMapper,
        OverlayMeasurementPointMapper overlayMeasurementPointMapper,
        ImportFileRecordMapper importFileRecordMapper
    ) {
        this.lotMapper = lotMapper;
        this.waferMapper = waferMapper;
        this.layerMapper = layerMapper;
        this.measurementRunMapper = measurementRunMapper;
        this.overlayMeasurementPointMapper = overlayMeasurementPointMapper;
        this.importFileRecordMapper = importFileRecordMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WaferAnalysisImportResultVo importWaferAnalysisCsv(MultipartFile file, WaferAnalysisImportConfigDto config) {
        long startedAt = System.currentTimeMillis();
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        validateFile(file);
        validateConfig(config);

        log.info(
            "[wafer-import] start userId={} fileName={} fileSize={} lotNo={} waferNo={} layerId={} measurementType={} stage={}",
            currentUserId, file.getOriginalFilename(), file.getSize(), config.getLotNo(), config.getWaferNo(),
            config.getLayerId(), config.getMeasurementType(), config.getStage()
        );

        Layer layer = resolveLayer(config.getLayerId(), currentUserId);
        Lot lot = resolveLot(config, currentUserId);
        Wafer wafer = resolveWafer(config, lot);
        ImportFileRecord fileRecord = createImportFileRecord(file, currentUserId);
        MeasurementRun run = createMeasurementRun(config, lot.getId(), wafer.getId(), layer.getId(), fileRecord.getId());

        WaferAnalysisImportResultVo result = new WaferAnalysisImportResultVo();
        result.setLotId(lot.getId());
        result.setWaferId(wafer.getId());
        result.setMeasurementRunId(run.getId());
        result.setMeasurementRunNo(run.getRunNo());

        parseAndPersistPoints(file, config, run, wafer, result);

        boolean imported = result.getInsertedRows() > 0;
        result.setImported(imported);
        result.setStatus(imported ? "IMPORTED" : "FAILED");
        if (imported) {
            result.setMessage("导入完成");
        } else {
            result.setMessage("未导入有效点位，请检查字段映射与坐标范围");
        }
        result.setElapsedMs(System.currentTimeMillis() - startedAt);

        fileRecord.setStatus(result.getStatus());
        fileRecord.setValidationSummaryJson(toJsonQuietly(Map.of(
            "totalRows", result.getTotalRows(),
            "insertedRows", result.getInsertedRows(),
            "skippedOutsideRows", result.getSkippedOutsideRows(),
            "failedRows", result.getFailedRows()
        )));
        fileRecord.setErrorMessage(result.getErrors().isEmpty() ? null : result.getErrors().get(0));
        importFileRecordMapper.updateById(fileRecord);

        run.setSamplingCount(result.getInsertedRows());
        measurementRunMapper.updateById(run);

        log.info(
            "[wafer-import] done userId={} runNo={} totalRows={} insertedRows={} skippedOutsideRows={} failedRows={} elapsedMs={}",
            currentUserId, run.getRunNo(), result.getTotalRows(), result.getInsertedRows(), result.getSkippedOutsideRows(),
            result.getFailedRows(), result.getElapsedMs()
        );
        return result;
    }

    @Override
    public String buildImportTemplateCsv() {
        return "target_code,x_coord,y_coord,overlay_x,overlay_y,overlay_magnitude,residual,focus,dose,confidence,outlier\n"
            + "P0001,125.00,125.00,1.20,-0.80,1.4422,0.20,0.05,42.60,0.98,0\n"
            + "P0002,124.00,126.00,1.10,-0.60,1.2529,0.15,0.04,42.55,0.97,0\n";
    }

    private void parseAndPersistPoints(
        MultipartFile file,
        WaferAnalysisImportConfigDto config,
        MeasurementRun run,
        Wafer wafer,
        WaferAnalysisImportResultVo result
    ) {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (!StringUtils.hasText(headerLine)) {
                throw new BusinessException("IMPORT_FILE_EMPTY", "CSV 为空");
            }
            List<String> headers = parseCsvLine(headerLine);
            Map<String, Integer> headerIndex = buildHeaderIndex(headers);
            int rowNo = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                rowNo++;
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                result.setTotalRows(result.getTotalRows() + 1);
                List<String> columns = parseCsvLine(line);
                try {
                    OverlayMeasurementPoint point = buildPoint(columns, headerIndex, config.getFieldMapping(), config, run, wafer, rowNo);
                    if (!insideNormalizedCircle(point.getXCoord(), point.getYCoord())) {
                        result.setSkippedOutsideRows(result.getSkippedOutsideRows() + 1);
                        continue;
                    }
                    overlayMeasurementPointMapper.insert(point);
                    result.setInsertedRows(result.getInsertedRows() + 1);
                } catch (IllegalArgumentException ex) {
                    result.setFailedRows(result.getFailedRows() + 1);
                    pushError(result.getErrors(), "row " + rowNo + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            throw new BusinessException("IMPORT_IO_ERROR", "读取 CSV 失败: " + ex.getMessage());
        }
    }

    private OverlayMeasurementPoint buildPoint(
        List<String> columns,
        Map<String, Integer> headerIndex,
        WaferImportFieldMappingDto mapping,
        WaferAnalysisImportConfigDto config,
        MeasurementRun run,
        Wafer wafer,
        int rowNo
    ) {
        OverlayMeasurementPoint point = new OverlayMeasurementPoint();
        point.setMeasurementRunId(run.getId());
        point.setWaferId(wafer.getId());
        point.setLayerId(run.getLayerId());
        point.setTargetCode(readString(columns, headerIndex, mapping.getTargetCodeColumn(), "P" + String.format(Locale.ROOT, "%06d", rowNo)));
        point.setXCoord(readDecimal(columns, headerIndex, mapping.getXCoordColumn(), true));
        point.setYCoord(readDecimal(columns, headerIndex, mapping.getYCoordColumn(), true));
        point.setOverlayX(readDecimal(columns, headerIndex, mapping.getOverlayXColumn(), false));
        point.setOverlayY(readDecimal(columns, headerIndex, mapping.getOverlayYColumn(), false));
        BigDecimal magnitude = readDecimal(columns, headerIndex, mapping.getOverlayMagnitudeColumn(), false);
        if (magnitude == null && Boolean.TRUE.equals(config.getGenerateMagnitudeWhenMissing())
            && point.getOverlayX() != null && point.getOverlayY() != null) {
            magnitude = sqrt(point.getOverlayX().pow(2).add(point.getOverlayY().pow(2)));
        }
        point.setOverlayMagnitude(magnitude);
        point.setResidualValue(readDecimal(columns, headerIndex, mapping.getResidualColumn(), false));
        point.setFocusValue(readDecimal(columns, headerIndex, mapping.getFocusColumn(), false));
        point.setDoseValue(readDecimal(columns, headerIndex, mapping.getDoseColumn(), false));
        BigDecimal confidence = readDecimal(columns, headerIndex, mapping.getConfidenceColumn(), false);
        point.setConfidence(confidence != null ? confidence : BigDecimal.ONE);
        Integer outlier = readInteger(columns, headerIndex, mapping.getOutlierColumn());
        if (outlier == null && config.getOutlierThreshold() != null && magnitude != null) {
            outlier = magnitude.compareTo(config.getOutlierThreshold()) > 0 ? 1 : 0;
        }
        point.setIsOutlier(outlier != null ? outlier : 0);
        return point;
    }

    private Layer resolveLayer(Long layerId, Long currentUserId) {
        Layer layer = layerMapper.selectById(layerId);
        if (layer == null || (layer.getDeleted() != null && layer.getDeleted() == 1)) {
            throw new BusinessException("LAYER_NOT_FOUND", "Layer 不存在");
        }
        if (layer.getCreatedBy() != null && layer.getCreatedBy() != 0L && !layer.getCreatedBy().equals(currentUserId)) {
            throw new BusinessException("LAYER_FORBIDDEN", "无权使用该 Layer");
        }
        return layer;
    }

    private Lot resolveLot(WaferAnalysisImportConfigDto config, Long currentUserId) {
        Lot existing = lotMapper.selectOne(new LambdaQueryWrapper<Lot>()
            .eq(Lot::getLotNo, config.getLotNo())
            .eq(Lot::getDeleted, 0)
            .last("LIMIT 1"));
        if (existing != null) {
            if (existing.getIsDemo() != null && existing.getIsDemo() == 1) {
                throw new BusinessException("LOT_NO_CONFLICT_DEMO", "lotNo 已被 Demo 数据占用，请使用新 lotNo");
            }
            if (!currentUserId.equals(existing.getOwnerUserId())) {
                throw new BusinessException("LOT_NO_CONFLICT_OWNER", "lotNo 已被其他用户占用");
            }
            existing.setLotStatus(defaultOr(config.getLotStatus(), existing.getLotStatus(), "READY"));
            existing.setPriorityLevel(defaultOr(config.getPriorityLevel(), existing.getPriorityLevel(), "NORMAL"));
            existing.setRemark(config.getLotRemark());
            existing.setSourceType("USER");
            lotMapper.updateById(existing);
            return existing;
        }

        Lot lot = new Lot();
        lot.setLotNo(config.getLotNo());
        lot.setLotStatus(defaultOr(config.getLotStatus(), null, "READY"));
        lot.setPriorityLevel(defaultOr(config.getPriorityLevel(), null, "NORMAL"));
        lot.setRemark(config.getLotRemark());
        lot.setWaferCount(1);
        lot.setSourceType("USER");
        lot.setOwnerUserId(currentUserId);
        lot.setIsDemo(0);
        lotMapper.insert(lot);
        return lot;
    }

    private Wafer resolveWafer(WaferAnalysisImportConfigDto config, Lot lot) {
        Wafer existing = waferMapper.selectOne(new LambdaQueryWrapper<Wafer>()
            .eq(Wafer::getLotId, lot.getId())
            .eq(Wafer::getWaferNo, config.getWaferNo())
            .eq(Wafer::getDeleted, 0)
            .last("LIMIT 1"));
        if (existing != null) {
            existing.setWaferStatus(defaultOr(config.getWaferStatus(), existing.getWaferStatus(), "READY"));
            existing.setSlotNo(config.getSlotNo());
            existing.setDiameterMm(config.getDiameterMm());
            waferMapper.updateById(existing);
            return existing;
        }

        Wafer wafer = new Wafer();
        wafer.setLotId(lot.getId());
        wafer.setWaferNo(config.getWaferNo());
        wafer.setWaferStatus(defaultOr(config.getWaferStatus(), null, "READY"));
        wafer.setSlotNo(config.getSlotNo());
        wafer.setDiameterMm(config.getDiameterMm());
        wafer.setSummaryTagsJson("{\"source\":\"USER_UPLOAD\"}");
        waferMapper.insert(wafer);
        return wafer;
    }

    private MeasurementRun createMeasurementRun(
        WaferAnalysisImportConfigDto config,
        Long lotId,
        Long waferId,
        Long layerId,
        Long importFileId
    ) {
        String runNo = StringUtils.hasText(config.getRunNo()) ? config.getRunNo() : generateRunNo();
        boolean exists = measurementRunMapper.selectCount(new LambdaQueryWrapper<MeasurementRun>()
            .eq(MeasurementRun::getRunNo, runNo)
            .eq(MeasurementRun::getDeleted, 0)) > 0;
        if (exists) {
            throw new BusinessException("RUN_NO_EXISTS", "runNo 已存在，请更换");
        }
        MeasurementRun run = new MeasurementRun();
        run.setRunNo(runNo);
        run.setLotId(lotId);
        run.setWaferId(waferId);
        run.setLayerId(layerId);
        run.setMeasurementType(defaultOr(config.getMeasurementType(), null, "OVERLAY"));
        run.setStage(defaultOr(config.getStage(), null, "PRE_ETCH"));
        run.setSourceType("USER_IMPORT");
        run.setToolName(defaultOr(config.getToolName(), null, "USER_UPLOAD"));
        run.setSamplingCount(0);
        run.setImportFileId(importFileId);
        run.setStatus("COMPLETED");
        run.setMeasurementContextJson(toJsonQuietly(Map.of(
            "axisRange", "0-250",
            "circleCenter", "125,125",
            "circleRadius", "125",
            "physicalRadiusHint", "250"
        )));
        measurementRunMapper.insert(run);
        return run;
    }

    private ImportFileRecord createImportFileRecord(MultipartFile file, Long currentUserId) {
        ImportFileRecord record = new ImportFileRecord();
        record.setFileNo("FILE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT));
        record.setFileName(defaultOr(file.getOriginalFilename(), null, "upload.csv"));
        record.setFileType("CSV");
        record.setBizType("MEASUREMENT");
        record.setFileSize(file.getSize());
        record.setStatus("UPLOADED");
        record.setUploadedBy(currentUserId);
        importFileRecordMapper.insert(record);
        return record;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("IMPORT_FILE_REQUIRED", "请上传 CSV 文件");
        }
        if (file.getSize() > MAX_IMPORT_FILE_SIZE_BYTES) {
            throw new BusinessException("IMPORT_FILE_TOO_LARGE", "CSV 文件不能超过 10MB");
        }
        String name = file.getOriginalFilename();
        if (!StringUtils.hasText(name) || !name.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new BusinessException("IMPORT_FILE_TYPE_INVALID", "仅支持 CSV 文件");
        }
    }

    private void validateConfig(WaferAnalysisImportConfigDto config) {
        if (config == null) {
            throw new BusinessException("IMPORT_CONFIG_REQUIRED", "导入配置不能为空");
        }
        if (!Boolean.TRUE.equals(config.getHasHeader())) {
            throw new BusinessException("IMPORT_HEADER_REQUIRED", "当前版本仅支持带表头 CSV");
        }
        normalizeFieldMapping(config);
    }

    private static void normalizeFieldMapping(WaferAnalysisImportConfigDto config) {
        WaferImportFieldMappingDto mapping = config.getFieldMapping();
        if (mapping == null) {
            mapping = new WaferImportFieldMappingDto();
            config.setFieldMapping(mapping);
        }
        // 前端/网关在 multipart 透传时可能丢失部分嵌套字段，这里做统一兜底，保障模板可直接导入。
        mapping.setTargetCodeColumn(defaultOr(mapping.getTargetCodeColumn(), null, "target_code"));
        mapping.setXCoordColumn(defaultOr(mapping.getXCoordColumn(), null, "x_coord"));
        mapping.setYCoordColumn(defaultOr(mapping.getYCoordColumn(), null, "y_coord"));
    }

    private static boolean insideNormalizedCircle(BigDecimal x, BigDecimal y) {
        if (x == null || y == null) {
            return false;
        }
        if (x.compareTo(BigDecimal.ZERO) < 0 || x.compareTo(AXIS_MAX) > 0
            || y.compareTo(BigDecimal.ZERO) < 0 || y.compareTo(AXIS_MAX) > 0) {
            return false;
        }
        // 坐标统一采用 0-250 轴；其中圆心为 (125,125)、半径为 125。
        // 该坐标系与“物理半径 250”采用 2:1 缩放映射，便于前端按 0-250 轴完整展示。
        BigDecimal dx = x.subtract(CIRCLE_CENTER);
        BigDecimal dy = y.subtract(CIRCLE_CENTER);
        BigDecimal distanceSquare = dx.pow(2).add(dy.pow(2));
        return distanceSquare.compareTo(CIRCLE_RADIUS.pow(2)) <= 0;
    }

    private static String defaultOr(String value, String fallback, String hardDefault) {
        if (StringUtils.hasText(value)) {
            return value;
        }
        if (StringUtils.hasText(fallback)) {
            return fallback;
        }
        return hardDefault;
    }

    private static String generateRunNo() {
        return "MRU-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
    }

    private static void pushError(List<String> errors, String message) {
        if (errors.size() < 50) {
            errors.add(message);
        }
    }

    private static String toJsonQuietly(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private static BigDecimal sqrt(BigDecimal value) {
        return BigDecimal.valueOf(Math.sqrt(value.doubleValue())).setScale(6, RoundingMode.HALF_UP);
    }

    private static Integer readInteger(List<String> columns, Map<String, Integer> headerIndex, String columnName) {
        String raw = readString(columns, headerIndex, columnName, null);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized) || "yes".equals(normalized)) {
            return 1;
        }
        if ("false".equals(normalized) || "no".equals(normalized)) {
            return 0;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("列 " + columnName + " 不是合法整数: " + raw);
        }
    }

    private static BigDecimal readDecimal(List<String> columns, Map<String, Integer> headerIndex, String columnName, boolean required) {
        String raw = readString(columns, headerIndex, columnName, null);
        if (!StringUtils.hasText(raw)) {
            if (required) {
                throw new IllegalArgumentException("列 " + columnName + " 不能为空");
            }
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("列 " + columnName + " 不是合法数字: " + raw);
        }
    }

    private static String readString(
        List<String> columns,
        Map<String, Integer> headerIndex,
        String columnName,
        String defaultValue
    ) {
        if (!StringUtils.hasText(columnName)) {
            return defaultValue;
        }
        Integer index = headerIndex.get(columnName.trim().toLowerCase(Locale.ROOT));
        if (index == null) {
            throw new IllegalArgumentException("CSV 缺少列: " + columnName);
        }
        if (index < 0 || index >= columns.size()) {
            return defaultValue;
        }
        String value = columns.get(index);
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim();
    }

    private static Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String normalizedHeader = headers.get(i).replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT);
            headerIndex.put(normalizedHeader, i);
        }
        return headerIndex;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (ch == ',' && !inQuotes) {
                columns.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        columns.add(current.toString());
        return columns;
    }
}
