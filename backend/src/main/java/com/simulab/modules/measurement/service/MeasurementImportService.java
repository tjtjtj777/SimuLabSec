package com.simulab.modules.measurement.service;

import com.simulab.modules.measurement.dto.WaferAnalysisImportConfigDto;
import com.simulab.modules.measurement.vo.WaferAnalysisImportResultVo;
import org.springframework.web.multipart.MultipartFile;

public interface MeasurementImportService {

    WaferAnalysisImportResultVo importWaferAnalysisCsv(MultipartFile file, WaferAnalysisImportConfigDto config);

    String buildImportTemplateCsv();
}
