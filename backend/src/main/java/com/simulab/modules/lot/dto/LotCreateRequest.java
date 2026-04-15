package com.simulab.modules.lot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LotCreateRequest {

    @NotBlank(message = "Lot 编号不能为空")
    @Size(min = 4, max = 64, message = "Lot 编号长度需在 4-64 之间")
    @Pattern(regexp = "^[A-Z0-9\\-]+$", message = "Lot 编号仅支持大写字母、数字和中划线")
    private String lotNo;

    @NotBlank(message = "Lot 状态不能为空")
    @Size(max = 32, message = "Lot 状态长度不能超过 32")
    private String lotStatus;

    @Size(max = 16, message = "优先级长度不能超过 16")
    private String priorityLevel;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
