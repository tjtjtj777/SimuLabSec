package com.simulab.modules.recipe.vo;

import lombok.Data;

@Data
public class RecipeSummaryVo {

    private Long id;
    private String recipeCode;
    private String recipeName;
    private String recipeType;
    private String status;
    private String dataScope;
}
