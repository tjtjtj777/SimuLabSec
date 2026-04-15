package com.simulab.modules.waferconfig.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simulab.modules.waferconfig.dto.WaferAnalysisGenerateRequestDto;
import com.simulab.modules.waferconfig.dto.WaferConfigQueryDto;
import com.simulab.modules.waferconfig.dto.WaferConfigUpsertDto;
import com.simulab.modules.waferconfig.vo.WaferAnalysisGenerateResultVo;
import com.simulab.modules.waferconfig.vo.WaferConfigVo;
import java.util.List;

public interface WaferAnalysisConfigService {

    // 返回一套“首次进入页面即可运行”的默认配置。
    WaferConfigVo buildDefaultConfig();

    // 返回错误列表而不是直接抛异常，便于前端逐项提示参数合法范围。
    List<String> validateConfig(WaferConfigUpsertDto dto);

    // 查询当前用户可见的配置列表，口径统一为 demo + mine。
    Page<WaferConfigVo> pageConfigs(WaferConfigQueryDto queryDto);

    WaferConfigVo getConfig(Long configId);

    WaferConfigVo createConfig(WaferConfigUpsertDto request);

    WaferConfigVo updateConfig(Long configId, WaferConfigUpsertDto request);

    void deleteConfig(Long configId);

    // 触发一次完整的 Wafer 分析，并返回这次生成出的 run/task/KPI 摘要。
    WaferAnalysisGenerateResultVo generateAnalysis(WaferAnalysisGenerateRequestDto request);
}
