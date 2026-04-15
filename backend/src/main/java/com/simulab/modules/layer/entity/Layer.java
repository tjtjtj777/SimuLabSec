package com.simulab.modules.layer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.simulab.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fab_layer")
public class Layer extends BaseEntity {

    private String layerCode;
    private String layerName;
    private String layerType;
    private Integer sequenceNo;
    private String description;
    private String status;
}
