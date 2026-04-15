package com.simulab.modules.wafer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simulab.common.security.SecurityContextUtils;
import com.simulab.modules.wafer.dto.WaferQueryDto;
import com.simulab.modules.wafer.entity.Wafer;
import com.simulab.modules.wafer.mapper.WaferMapper;
import com.simulab.modules.wafer.service.WaferService;
import com.simulab.modules.wafer.vo.WaferSummaryVo;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WaferServiceImpl implements WaferService {
    /*
     * 这个 Service 负责返回“当前用户可见的 wafer 基础列表”。
     *
     * 它的定位不是做复杂分析，而是提供稳定的主数据入口，供这些页面使用：
     * 1. Wafer Analysis：根据 lot 选择 wafer
     * 2. Multi Wafer Heatmap：选择多片 wafer 做展示
     * 3. 任务/量测相关页面：回显 wafer 基本信息
     *
     * 这个模块有一个很关键的业务约束：
     * wafer 自己不直接区分 demo / mine，而是通过所属 lot 的可见性来约束。
     * 所以“当前用户能不能看到这片 wafer”，本质上先取决于“能不能看到它的 lot”。
     */

    private static final long DEFAULT_PAGE_NO = 1L;
    private static final long DEFAULT_PAGE_SIZE = 20L;
    private static final long MAX_PAGE_SIZE = 200L;

    private final WaferMapper waferMapper;

    public WaferServiceImpl(WaferMapper waferMapper) {
        this.waferMapper = waferMapper;
    }

    @Override
    public Page<WaferSummaryVo> pageWafers(WaferQueryDto queryDto) {
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        // Wafer 查询本质上是在 lot 维度下查“当前用户可见的晶圆”：
        // 1. demo 数据对所有用户可见
        // 2. mine 数据只对 owner 可见
        //
        // 这里之所以不直接在 wafer 表上判断 owner，是因为当前数据模型里 lot 才是 demo/mine 的主要归属对象。
        // 也就是说：wafer 的可见性，跟着 lot 走。
        Page<Wafer> page = new Page<>(normalizePageNo(queryDto.getPageNo()), normalizePageSize(queryDto.getPageSize()));
        Page<Wafer> waferPage = waferMapper.selectPage(page, new LambdaQueryWrapper<Wafer>()
            .eq(Wafer::getDeleted, 0)
            // lotId 是最常用的过滤条件，因为 wafer 总是在 lot 下被查看。
            .eq(queryDto.getLotId() != null, Wafer::getLotId, queryDto.getLotId())
            .eq(StringUtils.hasText(queryDto.getWaferStatus()), Wafer::getWaferStatus, queryDto.getWaferStatus())
            .like(StringUtils.hasText(queryDto.getWaferNo()), Wafer::getWaferNo, queryDto.getWaferNo())
            // 这里通过子查询把“lot 可见性规则”传导到 wafer：
            // - is_demo = 1：所有用户都能看到
            // - owner_user_id = 当前用户：本人私有数据可见
            .inSql(Wafer::getLotId, "SELECT id FROM fab_lot WHERE deleted = 0 AND (is_demo = 1 OR owner_user_id = " + currentUserId + ")")
            // 排序遵循“同一个 lot 内按槽位顺序展示”的思路，便于和真实批次视角对齐。
            .orderByAsc(Wafer::getLotId, Wafer::getSlotNo, Wafer::getId));

        // Entity -> VO 映射时，会补齐前端直接需要的 dataScope 字段，
        // 这样前端无需再次推导“这条记录是 demo 还是 mine”。
        List<WaferSummaryVo> records = waferPage.getRecords().stream().map(this::toSummary).toList();
        Page<WaferSummaryVo> result = new Page<>(waferPage.getCurrent(), waferPage.getSize(), waferPage.getTotal());
        result.setRecords(records);
        return result;
    }

    private WaferSummaryVo toSummary(Wafer wafer) {
        // WaferSummaryVo 是一个“列表展示对象”，只保留页面直接需要的字段，
        // 不把 entity 上的所有数据库字段都暴露给前端。
        WaferSummaryVo vo = new WaferSummaryVo();
        vo.setId(wafer.getId());
        vo.setLotId(wafer.getLotId());
        vo.setWaferNo(wafer.getWaferNo());
        vo.setWaferStatus(wafer.getWaferStatus());
        vo.setSlotNo(wafer.getSlotNo());
        vo.setDiameterMm(wafer.getDiameterMm());
        // createdBy=0 约定为系统 demo 数据，其余视为当前用户私有数据。
        vo.setDataScope(wafer.getCreatedBy() != null && wafer.getCreatedBy() == 0L ? "DEMO" : "MINE");
        return vo;
    }

    private long normalizePageNo(Long pageNo) {
        // 所有列表接口都做防御式分页归一化，避免前端传空值或非法页码。
        return pageNo == null || pageNo < 1 ? DEFAULT_PAGE_NO : pageNo;
    }

    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        // 这里限制最大 pageSize，避免列表接口一次返回过大数据量。
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
