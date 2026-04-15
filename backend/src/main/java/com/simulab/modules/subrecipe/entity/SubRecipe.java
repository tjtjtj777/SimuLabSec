package com.simulab.modules.subrecipe.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.simulab.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sub_recipe")
public class SubRecipe extends BaseEntity {

    private String subRecipeCode;
    private Long recipeVersionId;
    private Long sourceTaskId;
    private Long lotId;
    private Long waferId;
    private String status;
    private String generationType;
    private String paramDeltaJson;
    private String paramSetJson;
    private String exportFormat;
}
