package com.simulab.modules.lot.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class LotSummaryVo {

    private Long id;
    private String lotNo;
    private String lotStatus;
    private String sourceType;
    private String priorityLevel;
    private Integer waferCount;
    private LocalDateTime collectedAt;
    private String dataScope;
    private Integer editable;
    private Integer deletable;
}
