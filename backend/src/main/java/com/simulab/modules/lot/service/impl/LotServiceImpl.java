package com.simulab.modules.lot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simulab.common.exception.BusinessException;
import com.simulab.common.security.SecurityContextUtils;
import com.simulab.modules.lot.dto.LotCreateRequest;
import com.simulab.modules.lot.dto.LotQueryDto;
import com.simulab.modules.lot.dto.LotUpdateRequest;
import com.simulab.modules.lot.entity.Lot;
import com.simulab.modules.lot.mapper.LotMapper;
import com.simulab.modules.lot.service.LotService;
import com.simulab.modules.lot.vo.LotSummaryVo;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LotServiceImpl implements LotService {

    private static final long DEFAULT_PAGE_NO = 1L;
    private static final long DEFAULT_PAGE_SIZE = 20L;
    private static final long MAX_PAGE_SIZE = 200L;

    private static final String SOURCE_DEMO = "DEMO";
    private static final String SOURCE_USER = "USER";

    private final LotMapper lotMapper;

    public LotServiceImpl(LotMapper lotMapper) {
        this.lotMapper = lotMapper;
    }

    @Override
    public Page<LotSummaryVo> pageLots(LotQueryDto queryDto) {
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        // 业务规则：列表默认仅返回「系统 Demo 数据 + 当前用户私有数据」。
        Page<Lot> page = new Page<>(normalizePageNo(queryDto.getPageNo()), normalizePageSize(queryDto.getPageSize()));
        Page<Lot> lotPage = lotMapper.selectPage(page, new LambdaQueryWrapper<Lot>()
            .eq(Lot::getDeleted, 0)
            .eq(StringUtils.hasText(queryDto.getLotStatus()), Lot::getLotStatus, queryDto.getLotStatus())
            .eq(StringUtils.hasText(queryDto.getSourceType()), Lot::getSourceType, queryDto.getSourceType())
            .like(StringUtils.hasText(queryDto.getKeyword()), Lot::getLotNo, queryDto.getKeyword())
            .and(wrapper -> wrapper.eq(Lot::getIsDemo, 1).or().eq(Lot::getOwnerUserId, currentUserId))
            .orderByDesc(Lot::getUpdatedAt, Lot::getId));

        List<LotSummaryVo> records = lotPage.getRecords().stream().map(this::toSummary).toList();
        Page<LotSummaryVo> result = new Page<>(lotPage.getCurrent(), lotPage.getSize(), lotPage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LotSummaryVo createLot(LotCreateRequest request) {
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        boolean exists = lotMapper.selectCount(new LambdaQueryWrapper<Lot>()
            .eq(Lot::getLotNo, request.getLotNo())
            .eq(Lot::getDeleted, 0)) > 0;
        if (exists) {
            throw new BusinessException("LOT_NO_EXISTS", "Lot 编号已存在");
        }
        Lot lot = new Lot();
        lot.setLotNo(request.getLotNo());
        lot.setOwnerUserId(currentUserId);
        lot.setIsDemo(0);
        lot.setLotStatus(request.getLotStatus());
        lot.setSourceType(SOURCE_USER);
        lot.setPriorityLevel(StringUtils.hasText(request.getPriorityLevel()) ? request.getPriorityLevel() : "NORMAL");
        lot.setWaferCount(0);
        lot.setRemark(request.getRemark());
        lotMapper.insert(lot);
        return toSummary(lot);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LotSummaryVo updateLot(Long lotId, LotUpdateRequest request) {
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        Lot lot = findVisibleLotById(lotId, currentUserId);
        if (lot.getIsDemo() != null && lot.getIsDemo() == 0 && !currentUserId.equals(lot.getOwnerUserId())) {
            throw new BusinessException("LOT_FORBIDDEN", "无权修改其他用户的私有 Lot");
        }
        lot.setLotStatus(request.getLotStatus());
        lot.setPriorityLevel(StringUtils.hasText(request.getPriorityLevel()) ? request.getPriorityLevel() : lot.getPriorityLevel());
        lot.setRemark(request.getRemark());
        lotMapper.updateById(lot);
        return toSummary(lot);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLot(Long lotId) {
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        Lot lot = findVisibleLotById(lotId, currentUserId);
        if (lot.getIsDemo() != null && lot.getIsDemo() == 1) {
            throw new BusinessException("DEMO_LOT_DELETE_FORBIDDEN", "Demo 数据不允许删除");
        }
        if (!currentUserId.equals(lot.getOwnerUserId())) {
            throw new BusinessException("LOT_FORBIDDEN", "无权删除其他用户的私有 Lot");
        }
        lotMapper.deleteById(lotId);
    }

    private Lot findVisibleLotById(Long lotId, Long currentUserId) {
        Lot lot = lotMapper.selectOne(new LambdaQueryWrapper<Lot>()
            .eq(Lot::getId, lotId)
            .eq(Lot::getDeleted, 0)
            .and(wrapper -> wrapper.eq(Lot::getIsDemo, 1).or().eq(Lot::getOwnerUserId, currentUserId)));
        if (lot == null) {
            throw new BusinessException("LOT_NOT_FOUND", "Lot 不存在或无权访问");
        }
        return lot;
    }

    private LotSummaryVo toSummary(Lot lot) {
        LotSummaryVo vo = new LotSummaryVo();
        vo.setId(lot.getId());
        vo.setLotNo(lot.getLotNo());
        vo.setLotStatus(lot.getLotStatus());
        vo.setSourceType(lot.getSourceType());
        vo.setPriorityLevel(lot.getPriorityLevel());
        vo.setWaferCount(lot.getWaferCount());
        vo.setCollectedAt(lot.getCollectedAt());
        boolean isDemo = lot.getIsDemo() != null && lot.getIsDemo() == 1;
        vo.setDataScope(isDemo ? "DEMO" : "MINE");
        vo.setEditable(1);
        vo.setDeletable(isDemo ? 0 : 1);
        return vo;
    }

    private long normalizePageNo(Long pageNo) {
        return pageNo == null || pageNo < 1 ? DEFAULT_PAGE_NO : pageNo;
    }

    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
