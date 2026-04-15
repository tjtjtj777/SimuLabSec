package com.simulab.modules.wafer.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simulab.modules.wafer.dto.WaferQueryDto;
import com.simulab.modules.wafer.vo.WaferSummaryVo;

public interface WaferService {

    // 返回当前用户可见的 wafer 分页结果，口径统一为 demo + mine。
    Page<WaferSummaryVo> pageWafers(WaferQueryDto queryDto);
}
