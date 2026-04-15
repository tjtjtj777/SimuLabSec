package com.simulab.modules.measurement.controller;

import com.simulab.common.api.ApiResponse;
import com.simulab.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulab.modules.measurement.dto.MeasurementRunQueryDto;
import com.simulab.modules.measurement.dto.WaferAnalysisImportConfigDto;
import com.simulab.modules.measurement.service.MeasurementImportService;
import com.simulab.modules.measurement.service.MeasurementRunService;
import com.simulab.modules.measurement.vo.WaferAnalysisImportResultVo;
import com.simulab.modules.measurement.vo.MeasurementRunSummaryVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/measurement-runs")
public class MeasurementRunController {

    private final MeasurementRunService measurementRunService;
    private final MeasurementImportService measurementImportService;
    private final ObjectMapper objectMapper;

    public MeasurementRunController(
        MeasurementRunService measurementRunService,
        MeasurementImportService measurementImportService,
        ObjectMapper objectMapper
    ) {
        this.measurementRunService = measurementRunService;
        this.measurementImportService = measurementImportService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ApiResponse<List<MeasurementRunSummaryVo>> list(@ModelAttribute MeasurementRunQueryDto queryDto) {
        return ApiResponse.success(measurementRunService.listRuns(queryDto));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<WaferAnalysisImportResultVo> importCsv(
        @RequestPart("file") MultipartFile file,
        @RequestPart("config") String configJson
    ) {
        WaferAnalysisImportConfigDto config;
        try {
            config = objectMapper.readValue(configJson, WaferAnalysisImportConfigDto.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("IMPORT_CONFIG_INVALID", "导入配置 JSON 解析失败");
        }
        return ApiResponse.success("导入执行完成", measurementImportService.importWaferAnalysisCsv(file, config));
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> importTemplate() {
        String content = measurementImportService.buildImportTemplateCsv();
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=wafer-analysis-import-template.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
