package com.simulab.modules.overlayanalysis.service;

import com.simulab.modules.overlayanalysis.dto.OverlayQueryDto;
import com.simulab.modules.overlayanalysis.dto.TrendQueryDto;
import com.simulab.modules.overlayanalysis.dto.WaferHeatmapBatchQueryDto;
import com.simulab.modules.overlayanalysis.vo.OverlayHistogramBinVo;
import com.simulab.modules.overlayanalysis.vo.OverlayScatterPointVo;
import com.simulab.modules.overlayanalysis.vo.OverlayTrendPointVo;
import com.simulab.modules.overlayanalysis.dto.WaferHeatmapQueryDto;
import com.simulab.modules.overlayanalysis.vo.WaferHeatmapBatchItemVo;
import com.simulab.modules.overlayanalysis.vo.WaferHeatmapBatchTaskStartVo;
import com.simulab.modules.overlayanalysis.vo.WaferHeatmapPointVo;
import java.util.List;

public interface OverlayAnalysisService {

    List<WaferHeatmapPointVo> buildWaferHeatmap(WaferHeatmapQueryDto queryDto);

    List<WaferHeatmapBatchItemVo> buildWaferHeatmapBatch(WaferHeatmapBatchQueryDto queryDto);

    WaferHeatmapBatchTaskStartVo startHeatmapBatchAsyncTask(WaferHeatmapBatchQueryDto queryDto);

    List<OverlayScatterPointVo> buildScatter(OverlayQueryDto queryDto);

    List<OverlayHistogramBinVo> buildHistogram(OverlayQueryDto queryDto);

    List<OverlayTrendPointVo> buildTrends(TrendQueryDto queryDto);
}
