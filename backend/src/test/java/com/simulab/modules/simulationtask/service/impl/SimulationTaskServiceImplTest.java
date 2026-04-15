package com.simulab.modules.simulationtask.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.simulab.common.security.SecurityUser;
import com.simulab.modules.simulationtask.dto.SimulationTaskQueryDto;
import com.simulab.modules.simulationtask.entity.SimulationTask;
import com.simulab.modules.simulationtask.mapper.SimulationTaskMapper;
import com.simulab.modules.simulationtask.vo.SimulationTaskStatusSummaryVo;
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
class SimulationTaskServiceImplTest {

    @Mock
    private SimulationTaskMapper simulationTaskMapper;

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
    void summarizeByStatusShouldReturnGroupedCounts() {
        SimulationTaskServiceImpl service = new SimulationTaskServiceImpl(simulationTaskMapper);
        SimulationTask t1 = new SimulationTask();
        t1.setStatus("RUNNING");
        t1.setTaskNo("T1");
        t1.setTaskName("Task1");
        SimulationTask t2 = new SimulationTask();
        t2.setStatus("SUCCESS");
        t2.setTaskNo("T2");
        t2.setTaskName("Task2");
        SimulationTask t3 = new SimulationTask();
        t3.setStatus("RUNNING");
        t3.setTaskNo("T3");
        t3.setTaskName("Task3");
        when(simulationTaskMapper.selectList(any())).thenReturn(List.of(t1, t2, t3));

        List<SimulationTaskStatusSummaryVo> grouped = service.summarizeByStatus(new SimulationTaskQueryDto());

        assertEquals(2, grouped.size());
        assertEquals(2L, grouped.stream().filter(x -> "RUNNING".equals(x.getStatus())).findFirst().orElseThrow().getCount());
    }
}
