package com.simulab.modules.overlayanalysis.controller;

import com.simulab.common.api.ApiResponse;
import com.simulab.common.security.ClientIpResolver;
import com.simulab.common.security.SecurityContextUtils;
import com.simulab.common.security.SecurityProtectionService;
import com.simulab.modules.overlayanalysis.dto.OverlayQueryDto;
import com.simulab.modules.overlayanalysis.dto.TrendQueryDto;
import com.simulab.modules.overlayanalysis.dto.WaferHeatmapBatchQueryDto;
import com.simulab.modules.overlayanalysis.dto.WaferHeatmapQueryDto;
import com.simulab.modules.overlayanalysis.vo.OverlayHistogramBinVo;
import com.simulab.modules.overlayanalysis.vo.OverlayScatterPointVo;
import com.simulab.modules.overlayanalysis.vo.OverlayTrendPointVo;
import com.simulab.modules.overlayanalysis.service.OverlayAnalysisService;
import com.simulab.modules.overlayanalysis.vo.WaferHeatmapBatchItemVo;
import com.simulab.modules.overlayanalysis.vo.WaferHeatmapBatchTaskStartVo;
import com.simulab.modules.overlayanalysis.vo.WaferHeatmapPointVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/overlay-results")
@Validated
public class OverlayAnalysisController {

    private final OverlayAnalysisService overlayAnalysisService;
    private final SecurityProtectionService securityProtectionService;

    public OverlayAnalysisController(
        OverlayAnalysisService overlayAnalysisService,
        SecurityProtectionService securityProtectionService
    ) {
        this.overlayAnalysisService = overlayAnalysisService;
        this.securityProtectionService = securityProtectionService;
    }

    @GetMapping("/heatmap")
    public ApiResponse<List<WaferHeatmapPointVo>> heatmap(
        @Valid @ModelAttribute WaferHeatmapQueryDto queryDto,
        HttpServletRequest httpServletRequest
    ) {
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        String clientIp = ClientIpResolver.resolve(httpServletRequest);
        securityProtectionService.guardHeatmap("/api/overlay-results/heatmap", currentUserId, clientIp);
        return ApiResponse.success(overlayAnalysisService.buildWaferHeatmap(queryDto));
    }

    @PostMapping("/heatmap/batch")
    public ApiResponse<List<WaferHeatmapBatchItemVo>> heatmapBatch(
        @Valid @RequestBody WaferHeatmapBatchQueryDto queryDto,
        HttpServletRequest httpServletRequest
    ) {
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        String clientIp = ClientIpResolver.resolve(httpServletRequest);
        securityProtectionService.guardHeatmapBatch("/api/overlay-results/heatmap/batch", currentUserId, clientIp);
        return ApiResponse.success(overlayAnalysisService.buildWaferHeatmapBatch(queryDto));
    }

    @PostMapping("/heatmap/batch/tasks")
    public ApiResponse<WaferHeatmapBatchTaskStartVo> startHeatmapBatchTask(
        @Valid @RequestBody WaferHeatmapBatchQueryDto queryDto,
        HttpServletRequest httpServletRequest
    ) {
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        String clientIp = ClientIpResolver.resolve(httpServletRequest);
        securityProtectionService.guardHeatmapBatch("/api/overlay-results/heatmap/batch/tasks", currentUserId, clientIp);
        return ApiResponse.success("Started", overlayAnalysisService.startHeatmapBatchAsyncTask(queryDto));
    }

    @GetMapping("/scatter")
    public ApiResponse<List<OverlayScatterPointVo>> scatter(@ModelAttribute OverlayQueryDto queryDto) {
        return ApiResponse.success(overlayAnalysisService.buildScatter(queryDto));
    }

    @GetMapping("/histogram")
    public ApiResponse<List<OverlayHistogramBinVo>> histogram(@ModelAttribute OverlayQueryDto queryDto) {
        return ApiResponse.success(overlayAnalysisService.buildHistogram(queryDto));
    }

    @GetMapping("/trends")
    public ApiResponse<List<OverlayTrendPointVo>> trends(@ModelAttribute TrendQueryDto queryDto) {
        return ApiResponse.success(overlayAnalysisService.buildTrends(queryDto));
    }
}
