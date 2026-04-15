package com.simulab.modules.recipeversion.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulab.modules.recipeversion.entity.RecipeVersion;
import com.simulab.modules.recipeversion.mapper.RecipeVersionMapper;
import com.simulab.modules.recipeversion.vo.RecipeVersionCompareVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecipeVersionServiceImplTest {

    @Mock
    private RecipeVersionMapper recipeVersionMapper;

    @Test
    void compareVersionsShouldReturnChangedParams() {
        RecipeVersionServiceImpl service = new RecipeVersionServiceImpl(recipeVersionMapper, new ObjectMapper());
        RecipeVersion left = new RecipeVersion();
        left.setId(11L);
        left.setVersionLabel("v1.0.0");
        left.setParamsJson("{\"dose\":42.5,\"focus\":0.12}");
        RecipeVersion right = new RecipeVersion();
        right.setId(12L);
        right.setVersionLabel("v1.1.0");
        right.setParamsJson("{\"dose\":43.1,\"focus\":0.12,\"scanSpeed\":1.05}");

        when(recipeVersionMapper.selectOne(any())).thenReturn(left, right);

        RecipeVersionCompareVo compareVo = service.compareVersions(11L, 12L);

        assertEquals(2, compareVo.getDiffs().size());
        assertEquals("v1.0.0", compareVo.getLeftVersionLabel());
        assertEquals("v1.1.0", compareVo.getRightVersionLabel());
    }
}
