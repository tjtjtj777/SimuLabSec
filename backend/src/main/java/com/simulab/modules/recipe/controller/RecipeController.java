package com.simulab.modules.recipe.controller;

import com.simulab.common.api.ApiResponse;
import com.simulab.modules.recipe.dto.RecipeQueryDto;
import com.simulab.modules.recipe.service.RecipeService;
import com.simulab.modules.recipe.vo.RecipeSummaryVo;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public ApiResponse<List<RecipeSummaryVo>> list(@ModelAttribute RecipeQueryDto queryDto) {
        return ApiResponse.success(recipeService.listRecipes(queryDto));
    }
}
