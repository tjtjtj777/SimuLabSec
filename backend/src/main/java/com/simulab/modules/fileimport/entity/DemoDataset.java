package com.simulab.modules.fileimport.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.simulab.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("demo_dataset")
public class DemoDataset extends BaseEntity {

    private String datasetCode;
    private String datasetName;
    private String scenarioType;
    private String status;
    private String description;
    private String tagsJson;
    private String seedVersion;
}
