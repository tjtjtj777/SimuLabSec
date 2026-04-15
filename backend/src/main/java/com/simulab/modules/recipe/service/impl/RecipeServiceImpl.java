package com.simulab.modules.recipe.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.simulab.common.security.SecurityContextUtils;
import com.simulab.modules.recipe.dto.RecipeQueryDto;
import com.simulab.modules.recipe.entity.Recipe;
import com.simulab.modules.recipe.mapper.RecipeMapper;
import com.simulab.modules.recipe.service.RecipeService;
import com.simulab.modules.recipe.vo.RecipeSummaryVo;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RecipeServiceImpl implements RecipeService {

    private final RecipeMapper recipeMapper;

    public RecipeServiceImpl(RecipeMapper recipeMapper) {
        this.recipeMapper = recipeMapper;
    }

    @Override
    public List<RecipeSummaryVo> listRecipes(RecipeQueryDto queryDto) {
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        return recipeMapper.selectList(new LambdaQueryWrapper<Recipe>()
                .eq(StringUtils.hasText(queryDto.getStatus()), Recipe::getStatus, queryDto.getStatus())
                .like(StringUtils.hasText(queryDto.getKeyword()), Recipe::getRecipeName, queryDto.getKeyword())
                .and(wrapper -> wrapper.isNull(Recipe::getOwnerUserId).or().eq(Recipe::getOwnerUserId, currentUserId))
                .eq(Recipe::getDeleted, 0))
            .stream()
            .map(recipe -> {
                RecipeSummaryVo vo = new RecipeSummaryVo();
                vo.setId(recipe.getId());
                vo.setRecipeCode(recipe.getRecipeCode());
                vo.setRecipeName(recipe.getRecipeName());
                vo.setRecipeType(recipe.getRecipeType());
                vo.setStatus(recipe.getStatus());
                vo.setDataScope(recipe.getOwnerUserId() == null ? "DEMO" : "MINE");
                return vo;
            })
            .toList();
    }
}
