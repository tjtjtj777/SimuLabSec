package com.simulab.modules.recipeversion.vo;

import lombok.Data;

@Data
public class RecipeParamDiffVo {

    private String paramName;
    private String leftValue;
    private String rightValue;
}
