package com.simulab.modules.overlayanalysis.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class WaferHeatmapBatchTaskStartVo {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long taskId;
    private String taskNo;
    private String status;
    private Integer requestedRuns;
}

