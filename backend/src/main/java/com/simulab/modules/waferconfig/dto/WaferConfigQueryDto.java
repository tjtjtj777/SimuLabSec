package com.simulab.modules.waferconfig.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WaferConfigQueryDto {

    @Size(max = 64, message = "关键字长度不能超过 64")
    private String keyword;
    // DEMO / MINE / 空。空表示同时查 demo + mine。
    private String dataScope;
    private Long layerId;
    private String stage;
    @Min(value = 1, message = "页码最小为 1")
    private Long pageNo = 1L;
    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 50, message = "每页条数不能超过 50")
    private Long pageSize = 10L;
}
