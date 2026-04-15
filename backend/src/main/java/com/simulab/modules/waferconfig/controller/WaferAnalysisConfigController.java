package com.simulab.modules.waferconfig.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simulab.common.api.ApiResponse;
import com.simulab.common.security.ClientIpResolver;
import com.simulab.common.security.SecurityContextUtils;
import com.simulab.common.security.SecurityProtectionService;
import com.simulab.modules.waferconfig.dto.WaferAnalysisGenerateRequestDto;
import com.simulab.modules.waferconfig.dto.WaferConfigQueryDto;
import com.simulab.modules.waferconfig.dto.WaferConfigUpsertDto;
import com.simulab.modules.waferconfig.service.WaferAnalysisConfigService;
import com.simulab.modules.waferconfig.vo.WaferAnalysisGenerateResultVo;
import com.simulab.modules.waferconfig.vo.WaferConfigVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/wafer-configs")
@Validated
public class WaferAnalysisConfigController {

    private final WaferAnalysisConfigService waferAnalysisConfigService;
    private final SecurityProtectionService securityProtectionService;

    public WaferAnalysisConfigController(
        WaferAnalysisConfigService waferAnalysisConfigService,
        SecurityProtectionService securityProtectionService
    ) {
        this.waferAnalysisConfigService = waferAnalysisConfigService;
        this.securityProtectionService = securityProtectionService;
    }

    @GetMapping("/default")
    // 返回一套可直接运行的默认配置，保证用户首次进入 Wafer Analysis 就能开始分析。
    public ApiResponse<WaferConfigVo> defaultConfig() {
        return ApiResponse.success(waferAnalysisConfigService.buildDefaultConfig());
    }

    @PostMapping("/validate")
    // 前端保存/生成前先走一次后端校验，确保参数范围与业务规则一致。
    public ApiResponse<List<String>> validate(@Valid @RequestBody WaferConfigUpsertDto request) {
        return ApiResponse.success(waferAnalysisConfigService.validateConfig(request));
    }

    @GetMapping
    public ApiResponse<Page<WaferConfigVo>> page(@Valid @ModelAttribute WaferConfigQueryDto queryDto) {
        return ApiResponse.success(waferAnalysisConfigService.pageConfigs(queryDto));
    }

    @GetMapping("/{configId}")
    public ApiResponse<WaferConfigVo> detail(@PathVariable Long configId) {
        return ApiResponse.success(waferAnalysisConfigService.getConfig(configId));
    }

    @PostMapping
    public ApiResponse<WaferConfigVo> create(@Valid @RequestBody WaferConfigUpsertDto request) {
        return ApiResponse.success("Created", waferAnalysisConfigService.createConfig(request));
    }

    @PutMapping("/{configId}")
    public ApiResponse<WaferConfigVo> update(@PathVariable Long configId, @Valid @RequestBody WaferConfigUpsertDto request) {
        return ApiResponse.success("Updated", waferAnalysisConfigService.updateConfig(configId, request));
    }

    @DeleteMapping("/{configId}")
    public ApiResponse<Void> delete(@PathVariable Long configId) {
        waferAnalysisConfigService.deleteConfig(configId);
        return ApiResponse.success("Deleted", null);
    }

    @PostMapping("/generate")
    // generate 是 Wafer Analysis 的业务入口：
    // 用户基于一套配置触发分析，后端会生成 run、点位、task 和汇总结果。
    public ApiResponse<WaferAnalysisGenerateResultVo> generate(
        @Valid @RequestBody WaferAnalysisGenerateRequestDto request,
        HttpServletRequest httpServletRequest
    ) {
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        String clientIp = ClientIpResolver.resolve(httpServletRequest);
        securityProtectionService.guardGenerate("/api/wafer-configs/generate", currentUserId, clientIp);
        return ApiResponse.success("Generated", waferAnalysisConfigService.generateAnalysis(request));
    }
}
