package com.simulab.modules.overlayanalysis.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class OverlayTrendPointVo {

    private LocalDate date;
    private String label;
    private BigDecimal passRate;
    private BigDecimal meanOverlay;
    private BigDecimal p95Overlay;
}
