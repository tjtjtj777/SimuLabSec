package com.simulab.modules.measurement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WaferAnalysisImportConfigDto {

    @NotBlank(message = "lotNo 不能为空")
    private String lotNo;

    private String lotStatus = "READY";
    private String priorityLevel = "NORMAL";
    private String lotRemark;

    @NotBlank(message = "waferNo 不能为空")
    private String waferNo;

    private String waferStatus = "READY";
    private Integer slotNo = 1;

    @DecimalMin(value = "100.0", message = "diameterMm 需 >= 100")
    private java.math.BigDecimal diameterMm = new java.math.BigDecimal("300.00");

    @NotNull(message = "layerId 不能为空")
    private Long layerId;

    private String runNo;
    private String measurementType = "OVERLAY";
    private String stage = "PRE_ETCH";
    private String toolName = "USER_UPLOAD";
    private Boolean hasHeader = true;
    private Boolean generateMagnitudeWhenMissing = true;
    private java.math.BigDecimal outlierThreshold;

    @Valid
    @NotNull(message = "fieldMapping 不能为空")
    private WaferImportFieldMappingDto fieldMapping;
}
