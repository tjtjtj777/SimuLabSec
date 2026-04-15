package com.simulab.modules.subrecipe.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.simulab.common.exception.BusinessException;
import com.simulab.common.security.SecurityContextUtils;
import com.simulab.modules.subrecipe.dto.SubRecipeExportRequest;
import com.simulab.modules.subrecipe.dto.SubRecipeQueryDto;
import com.simulab.modules.subrecipe.dto.SubRecipeUploadRequest;
import com.simulab.modules.subrecipe.entity.SubRecipe;
import com.simulab.modules.subrecipe.mapper.SubRecipeMapper;
import com.simulab.modules.subrecipe.service.SubRecipeService;
import com.simulab.modules.subrecipe.vo.SubRecipeDetailVo;
import com.simulab.modules.subrecipe.vo.SubRecipeFileTicketVo;
import com.simulab.modules.subrecipe.vo.SubRecipeSummaryVo;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SubRecipeServiceImpl implements SubRecipeService {

    private final SubRecipeMapper subRecipeMapper;

    public SubRecipeServiceImpl(SubRecipeMapper subRecipeMapper) {
        this.subRecipeMapper = subRecipeMapper;
    }

    @Override
    public List<SubRecipeSummaryVo> listSubRecipes(SubRecipeQueryDto queryDto) {
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        return subRecipeMapper.selectList(new LambdaQueryWrapper<SubRecipe>()
                .eq(queryDto.getRecipeVersionId() != null, SubRecipe::getRecipeVersionId, queryDto.getRecipeVersionId())
                .eq(queryDto.getLotId() != null, SubRecipe::getLotId, queryDto.getLotId())
                .eq(queryDto.getWaferId() != null, SubRecipe::getWaferId, queryDto.getWaferId())
                .eq(StringUtils.hasText(queryDto.getGenerationType()), SubRecipe::getGenerationType, queryDto.getGenerationType())
                .eq(StringUtils.hasText(queryDto.getStatus()), SubRecipe::getStatus, queryDto.getStatus())
                .and(wrapper -> wrapper.eq(SubRecipe::getCreatedBy, 0L).or().eq(SubRecipe::getCreatedBy, currentUserId))
                .eq(SubRecipe::getDeleted, 0)
                .orderByDesc(SubRecipe::getCreatedAt))
            .stream()
            .map(this::toSummaryVo)
            .toList();
    }

    @Override
    public SubRecipeDetailVo getDetail(Long subRecipeId) {
        SubRecipe entity = findById(subRecipeId);
        SubRecipeDetailVo vo = new SubRecipeDetailVo();
        vo.setId(entity.getId());
        vo.setSubRecipeCode(entity.getSubRecipeCode());
        vo.setRecipeVersionId(entity.getRecipeVersionId());
        vo.setSourceTaskId(entity.getSourceTaskId());
        vo.setLotId(entity.getLotId());
        vo.setWaferId(entity.getWaferId());
        vo.setStatus(entity.getStatus());
        vo.setGenerationType(entity.getGenerationType());
        vo.setExportFormat(entity.getExportFormat());
        vo.setParamDeltaJson(entity.getParamDeltaJson());
        vo.setParamSetJson(entity.getParamSetJson());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    @Override
    public SubRecipeFileTicketVo buildUploadTicket(SubRecipeUploadRequest request) {
        // 第一阶段仅提供上传预留票据，便于前端完成交互链路。
        SubRecipeFileTicketVo ticket = new SubRecipeFileTicketVo();
        ticket.setFileName(request.getFileName());
        ticket.setFileType(request.getFileType());
        ticket.setAction("UPLOAD");
        ticket.setObjectPath("/sub-recipe/upload/" + System.currentTimeMillis() + "-" + request.getFileName());
        ticket.setExpireAt(LocalDateTime.now().plusMinutes(15));
        return ticket;
    }

    @Override
    public SubRecipeFileTicketVo buildDownloadTicket(Long subRecipeId) {
        SubRecipe entity = findById(subRecipeId);
        SubRecipeFileTicketVo ticket = new SubRecipeFileTicketVo();
        ticket.setSubRecipeId(entity.getId());
        ticket.setFileName(entity.getSubRecipeCode() + ".json");
        ticket.setFileType("application/json");
        ticket.setAction("DOWNLOAD");
        ticket.setObjectPath("/sub-recipe/download/" + entity.getSubRecipeCode());
        ticket.setExpireAt(LocalDateTime.now().plusMinutes(15));
        return ticket;
    }

    @Override
    public SubRecipeFileTicketVo buildExportTicket(Long subRecipeId, SubRecipeExportRequest request) {
        SubRecipe entity = findById(subRecipeId);
        SubRecipeFileTicketVo ticket = new SubRecipeFileTicketVo();
        ticket.setSubRecipeId(entity.getId());
        ticket.setFileName(entity.getSubRecipeCode() + "." + request.getExportFormat().toLowerCase());
        ticket.setFileType("application/octet-stream");
        ticket.setAction("EXPORT");
        ticket.setObjectPath("/sub-recipe/export/" + entity.getSubRecipeCode() + "/" + request.getExportFormat().toLowerCase());
        ticket.setExpireAt(LocalDateTime.now().plusMinutes(15));
        return ticket;
    }

    private SubRecipeSummaryVo toSummaryVo(SubRecipe item) {
        SubRecipeSummaryVo vo = new SubRecipeSummaryVo();
        vo.setId(item.getId());
        vo.setSubRecipeCode(item.getSubRecipeCode());
        vo.setRecipeVersionId(item.getRecipeVersionId());
        vo.setSourceTaskId(item.getSourceTaskId());
        vo.setLotId(item.getLotId());
        vo.setWaferId(item.getWaferId());
        vo.setStatus(item.getStatus());
        vo.setGenerationType(item.getGenerationType());
        vo.setExportFormat(item.getExportFormat());
        vo.setDataScope(item.getCreatedBy() != null && item.getCreatedBy() == 0L ? "DEMO" : "MINE");
        return vo;
    }

    private SubRecipe findById(Long subRecipeId) {
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        SubRecipe entity = subRecipeMapper.selectOne(new LambdaQueryWrapper<SubRecipe>()
            .eq(SubRecipe::getId, subRecipeId)
            .and(wrapper -> wrapper.eq(SubRecipe::getCreatedBy, 0L).or().eq(SubRecipe::getCreatedBy, currentUserId))
            .eq(SubRecipe::getDeleted, 0));
        if (entity == null) {
            throw new BusinessException("SUB_RECIPE_NOT_FOUND", "Sub-recipe 不存在");
        }
        return entity;
    }
}
