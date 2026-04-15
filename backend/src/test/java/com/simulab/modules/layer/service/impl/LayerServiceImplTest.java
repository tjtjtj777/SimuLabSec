package com.simulab.modules.layer.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simulab.common.security.SecurityUser;
import com.simulab.modules.layer.dto.LayerQueryDto;
import com.simulab.modules.layer.entity.Layer;
import com.simulab.modules.layer.mapper.LayerMapper;
import com.simulab.modules.layer.vo.LayerSummaryVo;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class LayerServiceImplTest {

    @Mock
    private LayerMapper layerMapper;

    @BeforeEach
    void setupAuth() {
        SecurityUser securityUser = SecurityUser.builder().userId(1001L).username("u1").build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities()));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void pageLayersShouldReturnMappedPage() {
        LayerServiceImpl service = new LayerServiceImpl(layerMapper);
        LayerQueryDto dto = new LayerQueryDto();
        dto.setPageNo(1L);
        dto.setPageSize(10L);

        Layer layer = new Layer();
        layer.setId(11L);
        layer.setLayerCode("LAYER-M1");
        layer.setLayerName("Metal 1");
        layer.setLayerType("METAL");
        layer.setSequenceNo(10);
        layer.setStatus("ACTIVE");

        Page<Layer> dbPage = new Page<>(1, 10, 1);
        dbPage.setRecords(List.of(layer));
        when(layerMapper.selectPage(any(Page.class), any())).thenReturn(dbPage);

        Page<LayerSummaryVo> result = service.pageLayers(dto);

        assertEquals(1, result.getRecords().size());
        assertEquals("LAYER-M1", result.getRecords().get(0).getLayerCode());
        assertEquals(1L, result.getTotal());
        assertEquals("MINE", result.getRecords().get(0).getDataScope());
    }

    @Test
    void pageLayersShouldClampTooLargePageSize() {
        LayerServiceImpl service = new LayerServiceImpl(layerMapper);
        LayerQueryDto dto = new LayerQueryDto();
        dto.setPageNo(1L);
        dto.setPageSize(999L);

        when(layerMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<Layer> argPage = invocation.getArgument(0);
            assertEquals(200L, argPage.getSize());
            return new Page<Layer>(argPage.getCurrent(), argPage.getSize(), 0);
        });

        Page<LayerSummaryVo> result = service.pageLayers(dto);
        assertEquals(200L, result.getSize());
        assertEquals(0L, result.getTotal());
    }
}
