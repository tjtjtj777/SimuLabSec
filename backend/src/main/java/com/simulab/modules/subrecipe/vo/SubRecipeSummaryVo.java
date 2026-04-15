package com.simulab.modules.subrecipe.vo;

import lombok.Data;

@Data
public class SubRecipeSummaryVo {

    private Long id;
    private String subRecipeCode;
    private Long recipeVersionId;
    private Long sourceTaskId;
    private Long lotId;
    private Long waferId;
    private String status;
    private String generationType;
    private String exportFormat;
    private String dataScope;
}
