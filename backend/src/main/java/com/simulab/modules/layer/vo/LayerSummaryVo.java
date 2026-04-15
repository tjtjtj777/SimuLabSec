package com.simulab.modules.layer.vo;

import lombok.Data;

@Data
public class LayerSummaryVo {

    private Long id;
    private String layerCode;
    private String layerName;
    private String layerType;
    private Integer sequenceNo;
    private String status;
    private String dataScope;
}
