package com.simulab.modules.recipeversion.vo;

import lombok.Data;

@Data
public class RecipeVersionSummaryVo {

    private Long id;
    private Long recipeId;
    private Integer versionNo;
    private String versionLabel;
    private String status;
    private String changeSummary;
}
