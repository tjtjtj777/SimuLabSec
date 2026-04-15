package com.simulab.modules.measurement.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class MeasurementRunSummaryVo {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String runNo;
    private Long lotId;
    private Long waferId;
    private Long layerId;
    private String measurementType;
    private String stage;
    private String sourceType;
    private Integer samplingCount;
    private String status;
    private String dataScope;
}
