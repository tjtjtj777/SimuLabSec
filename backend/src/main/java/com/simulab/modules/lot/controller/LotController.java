package com.simulab.modules.lot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simulab.common.api.ApiResponse;
import com.simulab.modules.lot.dto.LotCreateRequest;
import com.simulab.modules.lot.dto.LotQueryDto;
import com.simulab.modules.lot.dto.LotUpdateRequest;
import com.simulab.modules.lot.service.LotService;
import com.simulab.modules.lot.vo.LotSummaryVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lots")
@Validated
public class LotController {

    private final LotService lotService;

    public LotController(LotService lotService) {
        this.lotService = lotService;
    }

    @GetMapping
    public ApiResponse<Page<LotSummaryVo>> page(@Valid @ModelAttribute LotQueryDto queryDto) {
        return ApiResponse.success(lotService.pageLots(queryDto));
    }

    @PostMapping
    public ApiResponse<LotSummaryVo> create(@Valid @RequestBody LotCreateRequest request) {
        return ApiResponse.success("创建成功", lotService.createLot(request));
    }

    @PutMapping("/{lotId}")
    public ApiResponse<LotSummaryVo> update(@PathVariable Long lotId, @Valid @RequestBody LotUpdateRequest request) {
        return ApiResponse.success("更新成功", lotService.updateLot(lotId, request));
    }

    @DeleteMapping("/{lotId}")
    public ApiResponse<Void> delete(@PathVariable Long lotId) {
        lotService.deleteLot(lotId);
        return ApiResponse.success("删除成功", null);
    }
}
