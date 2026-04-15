package com.simulab.modules.measurement.service;

import com.simulab.modules.measurement.dto.MeasurementRunQueryDto;
import com.simulab.modules.measurement.vo.MeasurementRunSummaryVo;
import java.util.List;

public interface MeasurementRunService {

    List<MeasurementRunSummaryVo> listRuns(MeasurementRunQueryDto queryDto);
}
