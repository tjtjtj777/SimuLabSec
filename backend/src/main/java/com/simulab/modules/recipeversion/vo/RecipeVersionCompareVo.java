package com.simulab.modules.recipeversion.vo;

import java.util.List;
import lombok.Data;

@Data
public class RecipeVersionCompareVo {

    private Long leftVersionId;
    private Long rightVersionId;
    private String leftVersionLabel;
    private String rightVersionLabel;
    private List<RecipeParamDiffVo> diffs;
}
