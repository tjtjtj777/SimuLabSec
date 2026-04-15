package com.simulab.modules.fileimport.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.simulab.modules.fileimport.dto.DemoDatasetQueryDto;
import com.simulab.modules.fileimport.entity.DemoDataset;
import com.simulab.modules.fileimport.mapper.DemoDatasetMapper;
import com.simulab.modules.fileimport.service.FileImportService;
import com.simulab.modules.fileimport.vo.DemoDatasetVo;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FileImportServiceImpl implements FileImportService {

    private final DemoDatasetMapper demoDatasetMapper;

    public FileImportServiceImpl(DemoDatasetMapper demoDatasetMapper) {
        this.demoDatasetMapper = demoDatasetMapper;
    }

    @Override
    public List<DemoDatasetVo> listDemoDatasets(DemoDatasetQueryDto queryDto) {
        return demoDatasetMapper.selectList(new LambdaQueryWrapper<DemoDataset>()
                .eq(StringUtils.hasText(queryDto.getStatus()), DemoDataset::getStatus, queryDto.getStatus())
                .eq(DemoDataset::getDeleted, 0))
            .stream()
            .map(dataset -> {
                DemoDatasetVo vo = new DemoDatasetVo();
                vo.setId(dataset.getId());
                vo.setDatasetCode(dataset.getDatasetCode());
                vo.setDatasetName(dataset.getDatasetName());
                vo.setScenarioType(dataset.getScenarioType());
                vo.setStatus(dataset.getStatus());
                vo.setDescription(dataset.getDescription());
                return vo;
            })
            .toList();
    }
}
