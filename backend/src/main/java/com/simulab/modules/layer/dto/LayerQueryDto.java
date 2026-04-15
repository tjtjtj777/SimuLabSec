package com.simulab.modules.layer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LayerQueryDto {

    @Size(max = 64, message = "关键字长度不能超过 64")
    private String keyword;
    private String status;
    @Min(value = 1, message = "页码最小为 1")
    private Long pageNo = 1L;
    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Long pageSize = 20L;
}
