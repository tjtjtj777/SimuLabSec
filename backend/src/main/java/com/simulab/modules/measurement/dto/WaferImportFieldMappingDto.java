package com.simulab.modules.measurement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WaferImportFieldMappingDto {

    @NotBlank(message = "xCoordColumn 不能为空")
    private String xCoordColumn;

    @NotBlank(message = "yCoordColumn 不能为空")
    private String yCoordColumn;

    private String targetCodeColumn;
    private String overlayXColumn;
    private String overlayYColumn;
    private String overlayMagnitudeColumn;
    private String residualColumn;
    private String focusColumn;
    private String doseColumn;
    private String confidenceColumn;
    private String outlierColumn;
}
