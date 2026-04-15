package com.simulab.modules.fileimport.vo;

import lombok.Data;

@Data
public class DemoDatasetVo {

    private Long id;
    private String datasetCode;
    private String datasetName;
    private String scenarioType;
    private String status;
    private String description;
}
