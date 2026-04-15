package com.simulab.modules.wafer.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simulab.common.security.SecurityUser;
import com.simulab.modules.wafer.dto.WaferQueryDto;
import com.simulab.modules.wafer.entity.Wafer;
import com.simulab.modules.wafer.mapper.WaferMapper;
import com.simulab.modules.wafer.vo.WaferSummaryVo;
import java.math.BigDecimal;
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
class WaferServiceImplTest {

    @Mock
    private WaferMapper waferMapper;

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
    void pageWafersShouldReturnMappedData() {
        WaferServiceImpl service = new WaferServiceImpl(waferMapper);
        WaferQueryDto dto = new WaferQueryDto();
        dto.setPageNo(1L);
        dto.setPageSize(10L);
        dto.setLotId(1L);

        Wafer wafer = new Wafer();
        wafer.setId(31L);
        wafer.setLotId(1L);
        wafer.setWaferNo("W01");
        wafer.setWaferStatus("READY");
        wafer.setSlotNo(1);
        wafer.setDiameterMm(new BigDecimal("300.00"));

        Page<Wafer> dbPage = new Page<>(1, 10, 1);
        dbPage.setRecords(List.of(wafer));
        when(waferMapper.selectPage(any(Page.class), any())).thenReturn(dbPage);

        Page<WaferSummaryVo> result = service.pageWafers(dto);

        assertEquals(1, result.getRecords().size());
        assertEquals("W01", result.getRecords().get(0).getWaferNo());
        assertEquals(1L, result.getTotal());
        assertEquals("MINE", result.getRecords().get(0).getDataScope());
    }
}
