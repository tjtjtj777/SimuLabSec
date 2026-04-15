package com.simulab.modules.lot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.simulab.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fab_lot")
public class Lot extends BaseEntity {

    private String lotNo;
    private Long productId;
    private Long ownerUserId;
    private Integer isDemo;
    private String lotStatus;
    private String sourceType;
    private String priorityLevel;
    private Integer waferCount;
    private LocalDateTime collectedAt;
    private String remark;
    private Long datasetId;
}
