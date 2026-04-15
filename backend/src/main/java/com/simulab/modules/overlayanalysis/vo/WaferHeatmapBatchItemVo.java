package com.simulab.modules.overlayanalysis.vo;

import java.util.List;
import lombok.Data;

@Data
public class WaferHeatmapBatchItemVo {

    private Long measurementRunId;
    private Boolean success;
    private String error;
    private List<WaferHeatmapPointVo> points;
}
