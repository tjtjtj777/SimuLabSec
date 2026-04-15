package com.simulab.modules.simulationtask.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SimulationTaskQueryDto {

    @Size(max = 64, message = "关键字长度不能超过 64")
    private String keyword;
    private String status;
    private String scenarioType;
    private Long lotId;
    private Long layerId;
    private Long recipeVersionId;
}
