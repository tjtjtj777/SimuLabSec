package com.simulab.modules.recipeversion.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulab.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.simulab.modules.recipeversion.dto.RecipeVersionQueryDto;
import com.simulab.modules.recipeversion.entity.RecipeVersion;
import com.simulab.modules.recipeversion.mapper.RecipeVersionMapper;
import com.simulab.modules.recipeversion.service.RecipeVersionService;
import com.simulab.modules.recipeversion.vo.RecipeParamDiffVo;
import com.simulab.modules.recipeversion.vo.RecipeVersionCompareVo;
import com.simulab.modules.recipeversion.vo.RecipeVersionSummaryVo;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RecipeVersionServiceImpl implements RecipeVersionService {

    private final RecipeVersionMapper recipeVersionMapper;
    private final ObjectMapper objectMapper;

    public RecipeVersionServiceImpl(RecipeVersionMapper recipeVersionMapper, ObjectMapper objectMapper) {
        this.recipeVersionMapper = recipeVersionMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<RecipeVersionSummaryVo> listVersions(RecipeVersionQueryDto queryDto) {
        return recipeVersionMapper.selectList(new LambdaQueryWrapper<RecipeVersion>()
                .eq(queryDto.getRecipeId() != null, RecipeVersion::getRecipeId, queryDto.getRecipeId())
                .eq(StringUtils.hasText(queryDto.getStatus()), RecipeVersion::getStatus, queryDto.getStatus())
                .eq(RecipeVersion::getDeleted, 0))
            .stream()
            .map(version -> {
                RecipeVersionSummaryVo vo = new RecipeVersionSummaryVo();
                vo.setId(version.getId());
                vo.setRecipeId(version.getRecipeId());
                vo.setVersionNo(version.getVersionNo());
                vo.setVersionLabel(version.getVersionLabel());
                vo.setStatus(version.getStatus());
                vo.setChangeSummary(version.getChangeSummary());
                return vo;
            })
            .toList();
    }

    @Override
    public RecipeVersionCompareVo compareVersions(Long leftVersionId, Long rightVersionId) {
        RecipeVersion left = recipeVersionMapper.selectOne(Wrappers.<RecipeVersion>lambdaQuery()
            .eq(RecipeVersion::getId, leftVersionId)
            .eq(RecipeVersion::getDeleted, 0));
        RecipeVersion right = recipeVersionMapper.selectOne(Wrappers.<RecipeVersion>lambdaQuery()
            .eq(RecipeVersion::getId, rightVersionId)
            .eq(RecipeVersion::getDeleted, 0));
        if (left == null || right == null) {
            throw new BusinessException("RECIPE_VERSION_NOT_FOUND", "配方版本不存在");
        }
        Map<String, Object> leftParams = parseJsonObject(left.getParamsJson());
        Map<String, Object> rightParams = parseJsonObject(right.getParamsJson());
        LinkedHashSet<String> keys = new LinkedHashSet<>(leftParams.keySet());
        keys.addAll(rightParams.keySet());
        List<RecipeParamDiffVo> diffs = keys.stream()
            .filter(key -> !Objects.equals(leftParams.get(key), rightParams.get(key)))
            .map(key -> {
                RecipeParamDiffVo diffVo = new RecipeParamDiffVo();
                diffVo.setParamName(key);
                diffVo.setLeftValue(leftParams.get(key) == null ? null : String.valueOf(leftParams.get(key)));
                diffVo.setRightValue(rightParams.get(key) == null ? null : String.valueOf(rightParams.get(key)));
                return diffVo;
            })
            .toList();

        RecipeVersionCompareVo compareVo = new RecipeVersionCompareVo();
        compareVo.setLeftVersionId(leftVersionId);
        compareVo.setRightVersionId(rightVersionId);
        compareVo.setLeftVersionLabel(left.getVersionLabel());
        compareVo.setRightVersionLabel(right.getVersionLabel());
        compareVo.setDiffs(diffs);
        return compareVo;
    }

    private Map<String, Object> parseJsonObject(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new BusinessException("RECIPE_VERSION_PARAMS_INVALID", "配方参数 JSON 解析失败");
        }
    }
}
