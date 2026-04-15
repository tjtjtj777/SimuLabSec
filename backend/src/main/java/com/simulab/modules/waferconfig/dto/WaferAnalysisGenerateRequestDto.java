package com.simulab.modules.waferconfig.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WaferAnalysisGenerateRequestDto {

    // 如果传 configId，表示基于已保存配置生成；否则使用 request.config 即时生成。
    @Positive(message = "configId 必须为正整数")
    private Long configId;
    // 是否把这次分析使用的配置顺便保存为一条可复用配置。
    private Boolean saveAsConfig = false;
    // 用于决定后端返回中文还是英文总结文案。
    @Size(max = 16, message = "locale 长度不能超过 16")
    private String locale;
    // 用户当前表单中的配置快照。
    @Valid
    private WaferConfigUpsertDto config;
}
