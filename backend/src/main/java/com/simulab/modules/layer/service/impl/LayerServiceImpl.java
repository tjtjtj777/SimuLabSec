package com.simulab.modules.layer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simulab.common.security.SecurityContextUtils;
import com.simulab.modules.layer.dto.LayerQueryDto;
import com.simulab.modules.layer.entity.Layer;
import com.simulab.modules.layer.mapper.LayerMapper;
import com.simulab.modules.layer.service.LayerService;
import com.simulab.modules.layer.vo.LayerSummaryVo;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LayerServiceImpl implements LayerService {

    private static final long DEFAULT_PAGE_NO = 1L;
    private static final long DEFAULT_PAGE_SIZE = 20L;
    private static final long MAX_PAGE_SIZE = 200L;

    private final LayerMapper layerMapper;

    public LayerServiceImpl(LayerMapper layerMapper) {
        this.layerMapper = layerMapper;
    }

    @Override
    public Page<LayerSummaryVo> pageLayers(LayerQueryDto queryDto) {
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        // Layer 为基础主数据，分页接口优先用于 Recipe 与任务筛选下拉，不做复杂联表。
        Page<Layer> page = new Page<>(normalizePageNo(queryDto.getPageNo()), normalizePageSize(queryDto.getPageSize()));
        Page<Layer> layerPage = layerMapper.selectPage(page, new LambdaQueryWrapper<Layer>()
            .eq(Layer::getDeleted, 0)
            .eq(StringUtils.hasText(queryDto.getStatus()), Layer::getStatus, queryDto.getStatus())
            .and(wrapper -> wrapper.eq(Layer::getCreatedBy, 0L).or().eq(Layer::getCreatedBy, currentUserId))
            .and(StringUtils.hasText(queryDto.getKeyword()), wrapper -> wrapper
                .like(Layer::getLayerCode, queryDto.getKeyword())
                .or()
                .like(Layer::getLayerName, queryDto.getKeyword()))
            .orderByAsc(Layer::getSequenceNo, Layer::getId));

        List<LayerSummaryVo> records = layerPage.getRecords().stream().map(this::toSummary).toList();
        Page<LayerSummaryVo> result = new Page<>(layerPage.getCurrent(), layerPage.getSize(), layerPage.getTotal());
        result.setRecords(records);
        return result;
    }

    private LayerSummaryVo toSummary(Layer layer) {
        LayerSummaryVo vo = new LayerSummaryVo();
        vo.setId(layer.getId());
        vo.setLayerCode(layer.getLayerCode());
        vo.setLayerName(layer.getLayerName());
        vo.setLayerType(layer.getLayerType());
        vo.setSequenceNo(layer.getSequenceNo());
        vo.setStatus(layer.getStatus());
        vo.setDataScope(layer.getCreatedBy() != null && layer.getCreatedBy() == 0L ? "DEMO" : "MINE");
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
