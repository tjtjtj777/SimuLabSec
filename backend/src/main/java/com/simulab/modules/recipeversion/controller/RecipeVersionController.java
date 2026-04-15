package com.simulab.modules.recipeversion.controller;

import com.simulab.common.api.ApiResponse;
import com.simulab.modules.recipeversion.dto.RecipeVersionQueryDto;
import com.simulab.modules.recipeversion.service.RecipeVersionService;
import com.simulab.modules.recipeversion.vo.RecipeVersionCompareVo;
import com.simulab.modules.recipeversion.vo.RecipeVersionSummaryVo;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipe-versions")
public class RecipeVersionController {

    private final RecipeVersionService recipeVersionService;

    public RecipeVersionController(RecipeVersionService recipeVersionService) {
        this.recipeVersionService = recipeVersionService;
    }

    @GetMapping
    public ApiResponse<List<RecipeVersionSummaryVo>> list(@ModelAttribute RecipeVersionQueryDto queryDto) {
        return ApiResponse.success(recipeVersionService.listVersions(queryDto));
    }

    @GetMapping("/compare")
    public ApiResponse<RecipeVersionCompareVo> compare(
        @RequestParam Long leftVersionId,
        @RequestParam Long rightVersionId
    ) {
        return ApiResponse.success(recipeVersionService.compareVersions(leftVersionId, rightVersionId));
    }
}
