package com.simulab.modules.simulationtask.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SimulationTaskDetailVo {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String taskNo;
    private String taskName;
    private Long lotId;
    private Long layerId;
    private Long recipeVersionId;
    private String scenarioType;
    private String status;
    private String priorityLevel;
    private String idempotencyKey;
    private String inputSnapshotJson;
    private String executionContextJson;
    private String resultSummaryJson;
    private String errorMessage;
    private Long requestedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
