package com.simulab.modules.subrecipe.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SubRecipeDetailVo {

    private Long id;
    private String subRecipeCode;
    private Long recipeVersionId;
    private Long sourceTaskId;
    private Long lotId;
    private Long waferId;
    private String status;
    private String generationType;
    private String exportFormat;
    private String paramDeltaJson;
    private String paramSetJson;
    private LocalDateTime createdAt;
}
