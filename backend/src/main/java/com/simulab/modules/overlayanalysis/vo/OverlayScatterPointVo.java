package com.simulab.modules.overlayanalysis.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class OverlayScatterPointVo {

    private String targetCode;
    private BigDecimal xValue;
    private BigDecimal yValue;
    private BigDecimal overlayMagnitude;
    private Integer outlier;
}
