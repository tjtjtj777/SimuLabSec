package com.simulab.modules.wafer.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class WaferSummaryVo {

    private Long id;
    private Long lotId;
    private String waferNo;
    private String waferStatus;
    private Integer slotNo;
    private BigDecimal diameterMm;
    // 数据范围标签：DEMO / MINE。前端可直接据此做徽标展示和默认排序。
    private String dataScope;
}
