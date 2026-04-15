package com.simulab.modules.measurement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.simulab.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("measurement_run")
public class MeasurementRun extends BaseEntity {

    private String runNo;
    private Long lotId;
    private Long waferId;
    private Long layerId;
    private String measurementType;
    private String stage;
    private String sourceType;
    private String toolName;
    private Integer samplingCount;
    private Long importFileId;
    private String analysisFingerprint;
    private String measurementContextJson;
    private String summaryJson;
    private String status;
}
