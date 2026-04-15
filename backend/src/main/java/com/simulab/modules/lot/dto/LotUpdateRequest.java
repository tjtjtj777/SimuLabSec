package com.simulab.modules.lot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LotUpdateRequest {

    @NotBlank(message = "Lot 状态不能为空")
    @Size(max = 32, message = "Lot 状态长度不能超过 32")
    private String lotStatus;

    @Size(max = 16, message = "优先级长度不能超过 16")
    private String priorityLevel;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
