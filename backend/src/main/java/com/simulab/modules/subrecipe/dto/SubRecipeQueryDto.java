package com.simulab.modules.subrecipe.dto;

import lombok.Data;

@Data
public class SubRecipeQueryDto {

    private Long recipeVersionId;
    private Long lotId;
    private Long waferId;
    private String generationType;
    private String status;
}
