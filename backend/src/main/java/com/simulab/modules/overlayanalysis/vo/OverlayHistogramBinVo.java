package com.simulab.modules.overlayanalysis.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class OverlayHistogramBinVo {

    private BigDecimal rangeStart;
    private BigDecimal rangeEnd;
    private Long count;
}
