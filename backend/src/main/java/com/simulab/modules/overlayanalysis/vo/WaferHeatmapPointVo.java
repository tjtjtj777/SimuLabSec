package com.simulab.modules.overlayanalysis.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class WaferHeatmapPointVo {

    private String targetCode;
    private BigDecimal xCoord;
    private BigDecimal yCoord;
    private BigDecimal metricValue;
    private BigDecimal confidence;
    private Integer outlier;
}
