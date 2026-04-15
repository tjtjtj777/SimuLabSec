package com.simulab.modules.fileimport.controller;

import com.simulab.common.api.ApiResponse;
import com.simulab.modules.fileimport.dto.DemoDatasetQueryDto;
import com.simulab.modules.fileimport.service.FileImportService;
import com.simulab.modules.fileimport.vo.DemoDatasetVo;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo-datasets")
public class DemoDatasetController {

    private final FileImportService fileImportService;

    public DemoDatasetController(FileImportService fileImportService) {
        this.fileImportService = fileImportService;
    }

    @GetMapping
    public ApiResponse<List<DemoDatasetVo>> list(@ModelAttribute DemoDatasetQueryDto queryDto) {
        return ApiResponse.success(fileImportService.listDemoDatasets(queryDto));
    }
}
