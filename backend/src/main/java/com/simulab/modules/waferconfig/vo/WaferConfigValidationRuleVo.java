package com.simulab.modules.waferconfig.vo;

import lombok.Data;

@Data
public class WaferConfigValidationRuleVo {

    // field 用于前端和具体参数字段绑定；rule/recommended 用于生成提示文案。
    private String field;
    private String label;
    private String rule;
    private String recommended;
}
