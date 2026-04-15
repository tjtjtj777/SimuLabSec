package com.simulab.modules.subrecipe.controller;

import com.simulab.common.api.ApiResponse;
import com.simulab.modules.subrecipe.dto.SubRecipeExportRequest;
import com.simulab.modules.subrecipe.dto.SubRecipeQueryDto;
import com.simulab.modules.subrecipe.dto.SubRecipeUploadRequest;
import com.simulab.modules.subrecipe.service.SubRecipeService;
import com.simulab.modules.subrecipe.vo.SubRecipeDetailVo;
import com.simulab.modules.subrecipe.vo.SubRecipeFileTicketVo;
import com.simulab.modules.subrecipe.vo.SubRecipeSummaryVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sub-recipes")
public class SubRecipeController {

    private final SubRecipeService subRecipeService;

    public SubRecipeController(SubRecipeService subRecipeService) {
        this.subRecipeService = subRecipeService;
    }

    @GetMapping
    public ApiResponse<List<SubRecipeSummaryVo>> list(@ModelAttribute SubRecipeQueryDto queryDto) {
        return ApiResponse.success(subRecipeService.listSubRecipes(queryDto));
    }

    @GetMapping("/{subRecipeId}")
    public ApiResponse<SubRecipeDetailVo> detail(@PathVariable Long subRecipeId) {
        return ApiResponse.success(subRecipeService.getDetail(subRecipeId));
    }

    @PostMapping("/upload-ticket")
    public ApiResponse<SubRecipeFileTicketVo> uploadTicket(@Valid @RequestBody SubRecipeUploadRequest request) {
        return ApiResponse.success(subRecipeService.buildUploadTicket(request));
    }

    @GetMapping("/{subRecipeId}/download-ticket")
    public ApiResponse<SubRecipeFileTicketVo> downloadTicket(@PathVariable Long subRecipeId) {
        return ApiResponse.success(subRecipeService.buildDownloadTicket(subRecipeId));
    }

    @PostMapping("/{subRecipeId}/export")
    public ApiResponse<SubRecipeFileTicketVo> export(
        @PathVariable Long subRecipeId,
        @Valid @RequestBody SubRecipeExportRequest request
    ) {
        return ApiResponse.success(subRecipeService.buildExportTicket(subRecipeId, request));
    }
}
