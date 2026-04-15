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
@RequestMapping("/api/files")
public class FileImportController {

    private final FileImportService fileImportService;

    public FileImportController(FileImportService fileImportService) {
        this.fileImportService = fileImportService;
    }

    @GetMapping("/demo-datasets")
    public ApiResponse<List<DemoDatasetVo>> demoDatasets(@ModelAttribute DemoDatasetQueryDto queryDto) {
        return ApiResponse.success(fileImportService.listDemoDatasets(queryDto));
    }
}
