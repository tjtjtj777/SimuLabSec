package com.simulab.modules.measurement.dto;

import lombok.Data;

@Data
public class MeasurementRunQueryDto {

    private Long lotId;
    private Long waferId;
    private Long layerId;
    private String measurementType;
    private String stage;
    private String status;
    private String sourceType;
}
