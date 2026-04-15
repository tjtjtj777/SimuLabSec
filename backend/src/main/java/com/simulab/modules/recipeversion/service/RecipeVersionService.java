package com.simulab.modules.recipeversion.service;

import com.simulab.modules.recipeversion.dto.RecipeVersionQueryDto;
import com.simulab.modules.recipeversion.vo.RecipeVersionCompareVo;
import com.simulab.modules.recipeversion.vo.RecipeVersionSummaryVo;
import java.util.List;

public interface RecipeVersionService {

    List<RecipeVersionSummaryVo> listVersions(RecipeVersionQueryDto queryDto);

    RecipeVersionCompareVo compareVersions(Long leftVersionId, Long rightVersionId);
}
