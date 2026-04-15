package com.simulab.modules.lot.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simulab.modules.lot.dto.LotCreateRequest;
import com.simulab.modules.lot.dto.LotQueryDto;
import com.simulab.modules.lot.dto.LotUpdateRequest;
import com.simulab.modules.lot.vo.LotSummaryVo;

public interface LotService {

    Page<LotSummaryVo> pageLots(LotQueryDto queryDto);

    LotSummaryVo createLot(LotCreateRequest request);

    LotSummaryVo updateLot(Long lotId, LotUpdateRequest request);

    void deleteLot(Long lotId);
}
