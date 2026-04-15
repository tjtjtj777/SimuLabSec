package com.simulab.modules.measurement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulab.common.security.SecurityUser;
import com.simulab.modules.measurement.dto.MeasurementRunQueryDto;
import com.simulab.modules.measurement.entity.MeasurementRun;
import com.simulab.modules.measurement.mapper.MeasurementRunMapper;
import com.simulab.modules.measurement.vo.MeasurementRunSummaryVo;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class MeasurementRunServiceImplTest {

    @Mock
    private MeasurementRunMapper measurementRunMapper;

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
    void listRunsShouldHitRedisCacheOnSecondRequest() {
        StringRedisTemplate redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = org.mockito.Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Map<String, String> cache = new ConcurrentHashMap<>();
        when(valueOperations.get(anyString())).thenAnswer(invocation -> cache.get(invocation.getArgument(0, String.class)));
        doAnswer(invocation -> {
            cache.put(invocation.getArgument(0, String.class), invocation.getArgument(1, String.class));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        MeasurementRun run = new MeasurementRun();
        run.setId(9001L);
        run.setRunNo("RUN-9001");
        run.setCreatedBy(0L);
        run.setDeleted(0);
        when(measurementRunMapper.selectList(any())).thenReturn(List.of(run));

        MeasurementRunServiceImpl service = new MeasurementRunServiceImpl(measurementRunMapper, redisTemplate, new ObjectMapper());
        MeasurementRunQueryDto queryDto = new MeasurementRunQueryDto();

        long coldStarted = System.nanoTime();
        List<MeasurementRunSummaryVo> first = service.listRuns(queryDto);
        long coldElapsedMs = (System.nanoTime() - coldStarted) / 1_000_000;

        long warmStarted = System.nanoTime();
        List<MeasurementRunSummaryVo> second = service.listRuns(queryDto);
        long warmElapsedMs = (System.nanoTime() - warmStarted) / 1_000_000;

        assertEquals(1, first.size());
        assertEquals(1, second.size());
        assertEquals("RUN-9001", second.get(0).getRunNo());
        verify(measurementRunMapper, times(1)).selectList(any());
        verify(valueOperations, times(2)).get(anyString());
        verify(valueOperations, times(1)).set(anyString(), anyString(), any(Duration.class));

        System.out.println("[perf][measurement-runs] coldMs=" + coldElapsedMs + ", warmMs=" + warmElapsedMs);
    }
}
