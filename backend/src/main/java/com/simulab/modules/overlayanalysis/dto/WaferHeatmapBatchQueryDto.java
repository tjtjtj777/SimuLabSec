package com.simulab.modules.overlayanalysis.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class WaferHeatmapBatchQueryDto {

    @NotEmpty(message = "measurementRunIds 不能为空")
    @Size(max = 24, message = "measurementRunIds 最大支持 24 个")
    private List<@Positive(message = "measurementRunId 必须为正整数") Long> measurementRunIds;
    @Size(max = 32, message = "metricCode 长度不能超过 32")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-]*$", message = "metricCode 格式不合法")
    private String metricCode;
}
