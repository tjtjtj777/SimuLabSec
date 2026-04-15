package com.simulab.modules.wafer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.simulab.common.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fab_wafer")
public class Wafer extends BaseEntity {

    // 所属批次。业务上是一对多：一个 lot 下有多片 wafer。
    private Long lotId;
    // 晶圆编号，例如 W01 / WS01。
    private String waferNo;
    private String waferStatus;
    // 在 lot 中的槽位序号，便于和设备或批次视角对齐。
    private Integer slotNo;
    private BigDecimal diameterMm;
    private String notchDirection;
    // 附加标签摘要，常用于标记 demo 场景或用户导入来源。
    private String summaryTagsJson;
    // 关联 demo_dataset；用户私有数据通常为空。
    private Long datasetId;
}
