package com.simulab.modules.recipe.service;

import com.simulab.modules.recipe.dto.RecipeQueryDto;
import com.simulab.modules.recipe.vo.RecipeSummaryVo;
import java.util.List;

public interface RecipeService {

    List<RecipeSummaryVo> listRecipes(RecipeQueryDto queryDto);
}
