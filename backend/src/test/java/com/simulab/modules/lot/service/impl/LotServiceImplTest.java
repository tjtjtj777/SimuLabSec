package com.simulab.modules.lot.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simulab.common.exception.BusinessException;
import com.simulab.common.security.SecurityUser;
import com.simulab.modules.lot.dto.LotCreateRequest;
import com.simulab.modules.lot.dto.LotQueryDto;
import com.simulab.modules.lot.dto.LotUpdateRequest;
import com.simulab.modules.lot.entity.Lot;
import com.simulab.modules.lot.mapper.LotMapper;
import com.simulab.modules.lot.vo.LotSummaryVo;
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
class LotServiceImplTest {

    @Mock
    private LotMapper lotMapper;

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
    void pageLotsShouldReturnMappedData() {
        LotServiceImpl service = new LotServiceImpl(lotMapper);
        LotQueryDto dto = new LotQueryDto();
        dto.setPageNo(1L);
        dto.setPageSize(10L);

        Lot lot = new Lot();
        lot.setId(21L);
        lot.setLotNo("LOT-DEMO-001");
        lot.setLotStatus("READY");
        lot.setSourceType("DEMO");
        lot.setPriorityLevel("NORMAL");
        lot.setWaferCount(3);
        lot.setIsDemo(1);
        lot.setCreatedBy(0L);

        Page<Lot> dbPage = new Page<>(1, 10, 1);
        dbPage.setRecords(List.of(lot));
        when(lotMapper.selectPage(any(Page.class), any())).thenReturn(dbPage);

        Page<LotSummaryVo> result = service.pageLots(dto);

        assertEquals(1, result.getRecords().size());
        assertEquals("LOT-DEMO-001", result.getRecords().get(0).getLotNo());
        assertEquals("DEMO", result.getRecords().get(0).getDataScope());
        assertEquals(0, result.getRecords().get(0).getDeletable());
    }

    @Test
    void createLotShouldSetUserOwnership() {
        LotServiceImpl service = new LotServiceImpl(lotMapper);
        when(lotMapper.selectCount(any())).thenReturn(0L);
        when(lotMapper.insert(any(Lot.class))).thenAnswer(invocation -> {
            Lot entity = invocation.getArgument(0);
            entity.setId(88L);
            return 1;
        });
        LotCreateRequest request = new LotCreateRequest();
        request.setLotNo("LOT-USER-001");
        request.setLotStatus("READY");
        request.setPriorityLevel("HIGH");
        LotSummaryVo vo = service.createLot(request);
        assertEquals("MINE", vo.getDataScope());
        assertEquals(1, vo.getDeletable());
    }

    @Test
    void deleteLotShouldRejectDemoData() {
        LotServiceImpl service = new LotServiceImpl(lotMapper);
        Lot demoLot = new Lot();
        demoLot.setId(1L);
        demoLot.setIsDemo(1);
        when(lotMapper.selectOne(any())).thenReturn(demoLot);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.deleteLot(1L));
        assertEquals("DEMO_LOT_DELETE_FORBIDDEN", ex.getCode());
    }

    @Test
    void updateLotShouldRejectOtherUsersPrivateData() {
        LotServiceImpl service = new LotServiceImpl(lotMapper);
        Lot lot = new Lot();
        lot.setId(1L);
        lot.setIsDemo(0);
        lot.setOwnerUserId(2002L);
        when(lotMapper.selectOne(any())).thenReturn(lot);
        LotUpdateRequest request = new LotUpdateRequest();
        request.setLotStatus("READY");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateLot(1L, request));
        assertEquals("LOT_FORBIDDEN", ex.getCode());
    }
}
