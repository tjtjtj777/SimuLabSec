package com.simulab.modules.layer.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simulab.modules.layer.dto.LayerQueryDto;
import com.simulab.modules.layer.vo.LayerSummaryVo;

public interface LayerService {

    Page<LayerSummaryVo> pageLayers(LayerQueryDto queryDto);
}
