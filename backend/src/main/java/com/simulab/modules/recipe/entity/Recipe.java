package com.simulab.modules.recipe.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.simulab.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recipe")
public class Recipe extends BaseEntity {

    private String recipeCode;
    private String recipeName;
    private String recipeType;
    private Long productId;
    private Long layerId;
    private Long ownerUserId;
    private String status;
    private String description;
}
