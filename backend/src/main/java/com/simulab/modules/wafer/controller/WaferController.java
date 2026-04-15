package com.simulab.modules.wafer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simulab.common.api.ApiResponse;
import com.simulab.modules.wafer.dto.WaferQueryDto;
import com.simulab.modules.wafer.service.WaferService;
import com.simulab.modules.wafer.vo.WaferSummaryVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wafers")
@Validated
public class WaferController {

    private final WaferService waferService;

    public WaferController(WaferService waferService) {
        this.waferService = waferService;
    }

    @GetMapping
    // Wafer 列表是多个页面的基础主数据来源，典型场景包括：
    // 1. Wafer Analysis 中根据 lot 选择 wafer
    // 2. Multi Wafer Heatmap 中挑选要展示的 wafer
    // 3. 任务或量测详情页回显 wafer 基本信息
    public ApiResponse<Page<WaferSummaryVo>> page(@Valid @ModelAttribute WaferQueryDto queryDto) {
        return ApiResponse.success(waferService.pageWafers(queryDto));
    }
}
