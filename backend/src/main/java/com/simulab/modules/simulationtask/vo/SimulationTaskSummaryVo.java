package com.simulab.modules.simulationtask.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SimulationTaskSummaryVo {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String taskNo;
    private String taskName;
    private String scenarioType;
    private String status;
    private Long lotId;
    private Long layerId;
    private Long recipeVersionId;
    private String priorityLevel;
    private String errorMessage;
    private LocalDateTime createdAt;
    private String dataScope;
}
