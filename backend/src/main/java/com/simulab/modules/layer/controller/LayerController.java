package com.simulab.modules.layer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simulab.common.api.ApiResponse;
import com.simulab.modules.layer.dto.LayerQueryDto;
import com.simulab.modules.layer.service.LayerService;
import com.simulab.modules.layer.vo.LayerSummaryVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/layers")
@Validated
public class LayerController {

    private final LayerService layerService;

    public LayerController(LayerService layerService) {
        this.layerService = layerService;
    }

    @GetMapping
    public ApiResponse<Page<LayerSummaryVo>> page(@Valid @ModelAttribute LayerQueryDto queryDto) {
        return ApiResponse.success(layerService.pageLayers(queryDto));
    }
}
