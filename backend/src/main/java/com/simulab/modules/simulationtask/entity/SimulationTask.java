package com.simulab.modules.simulationtask.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.simulab.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("simulation_task")
public class SimulationTask extends BaseEntity {

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
}
