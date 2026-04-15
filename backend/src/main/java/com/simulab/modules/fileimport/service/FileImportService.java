package com.simulab.modules.fileimport.service;

import com.simulab.modules.fileimport.dto.DemoDatasetQueryDto;
import com.simulab.modules.fileimport.vo.DemoDatasetVo;
import java.util.List;

public interface FileImportService {

    List<DemoDatasetVo> listDemoDatasets(DemoDatasetQueryDto queryDto);
}
