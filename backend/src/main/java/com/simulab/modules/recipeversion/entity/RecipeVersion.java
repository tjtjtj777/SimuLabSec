package com.simulab.modules.recipeversion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.simulab.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recipe_version")
public class RecipeVersion extends BaseEntity {

    private Long recipeId;
    private Integer versionNo;
    private String versionLabel;
    private String status;
    private Long parentVersionId;
    private String parameterSchemaJson;
    private String paramsJson;
    private String changeSummary;
}
