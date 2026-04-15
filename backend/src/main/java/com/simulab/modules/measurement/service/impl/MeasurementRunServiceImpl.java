package com.simulab.modules.measurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulab.common.security.SecurityContextUtils;
import com.simulab.modules.measurement.dto.MeasurementRunQueryDto;
import com.simulab.modules.measurement.entity.MeasurementRun;
import com.simulab.modules.measurement.mapper.MeasurementRunMapper;
import com.simulab.modules.measurement.service.MeasurementRunService;
import com.simulab.modules.measurement.vo.MeasurementRunSummaryVo;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MeasurementRunServiceImpl implements MeasurementRunService {

    private static final Logger log = LoggerFactory.getLogger(MeasurementRunServiceImpl.class);
    private static final String CACHE_PREFIX = "simulab:measurement:runs:";
    private final MeasurementRunMapper measurementRunMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public MeasurementRunServiceImpl(MeasurementRunMapper measurementRunMapper) {
        this(measurementRunMapper, null, null);
    }

    @Autowired
    public MeasurementRunServiceImpl(
        MeasurementRunMapper measurementRunMapper,
        StringRedisTemplate stringRedisTemplate,
        ObjectMapper objectMapper
    ) {
        this.measurementRunMapper = measurementRunMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<MeasurementRunSummaryVo> listRuns(MeasurementRunQueryDto queryDto) {
        long startedAt = System.currentTimeMillis();
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        String cacheKey = CACHE_PREFIX + currentUserId + ":" + queryDto.getLotId() + ":" + queryDto.getWaferId() + ":"
            + queryDto.getLayerId() + ":" + queryDto.getMeasurementType() + ":" + queryDto.getStage() + ":" + queryDto.getStatus()
            + ":" + queryDto.getSourceType();
        List<MeasurementRunSummaryVo> cached = readCachedRuns(cacheKey);
        if (cached != null) {
            log.info(
                "[measurement-run] list userId={} lotId={} waferId={} layerId={} measurementType={} stage={} size={} cacheHit=true elapsedMs={}",
                currentUserId, queryDto.getLotId(), queryDto.getWaferId(), queryDto.getLayerId(),
                queryDto.getMeasurementType(), queryDto.getStage(), cached.size(),
                System.currentTimeMillis() - startedAt
            );
            return cached;
        }
        List<MeasurementRunSummaryVo> result = measurementRunMapper.selectList(new LambdaQueryWrapper<MeasurementRun>()
                .eq(queryDto.getLotId() != null, MeasurementRun::getLotId, queryDto.getLotId())
                .eq(queryDto.getWaferId() != null, MeasurementRun::getWaferId, queryDto.getWaferId())
                .eq(queryDto.getLayerId() != null, MeasurementRun::getLayerId, queryDto.getLayerId())
                .eq(StringUtils.hasText(queryDto.getMeasurementType()), MeasurementRun::getMeasurementType, queryDto.getMeasurementType())
                .eq(StringUtils.hasText(queryDto.getStage()), MeasurementRun::getStage, queryDto.getStage())
                .eq(StringUtils.hasText(queryDto.getStatus()), MeasurementRun::getStatus, queryDto.getStatus())
                .eq(StringUtils.hasText(queryDto.getSourceType()), MeasurementRun::getSourceType, queryDto.getSourceType())
                .and(wrapper -> wrapper.eq(MeasurementRun::getCreatedBy, 0L)
                    .or().eq(MeasurementRun::getCreatedBy, currentUserId)
                    .or().isNotNull(MeasurementRun::getAnalysisFingerprint))
                .eq(MeasurementRun::getDeleted, 0)
                .orderByAsc(MeasurementRun::getCreatedBy)
                .orderByDesc(MeasurementRun::getSamplingCount)
                .orderByDesc(MeasurementRun::getId))
            .stream()
            .map(run -> {
                MeasurementRunSummaryVo vo = new MeasurementRunSummaryVo();
                vo.setId(run.getId());
                vo.setRunNo(run.getRunNo());
                vo.setLotId(run.getLotId());
                vo.setWaferId(run.getWaferId());
                vo.setLayerId(run.getLayerId());
                vo.setMeasurementType(run.getMeasurementType());
                vo.setStage(run.getStage());
                vo.setSourceType(run.getSourceType());
                vo.setSamplingCount(run.getSamplingCount());
                vo.setStatus(run.getStatus());
                vo.setDataScope(run.getCreatedBy() != null && run.getCreatedBy() == 0L ? "DEMO"
                    : (run.getCreatedBy() != null && run.getCreatedBy().equals(currentUserId) ? "MINE" : "SHARED"));
                return vo;
            })
            .toList();
        writeCachedRuns(cacheKey, result);
        log.info(
            "[measurement-run] list userId={} lotId={} waferId={} layerId={} measurementType={} stage={} size={} cacheHit=false elapsedMs={}",
            currentUserId, queryDto.getLotId(), queryDto.getWaferId(), queryDto.getLayerId(),
            queryDto.getMeasurementType(), queryDto.getStage(), result.size(),
            System.currentTimeMillis() - startedAt
        );
        return result;
    }

    private List<MeasurementRunSummaryVo> readCachedRuns(String cacheKey) {
        if (stringRedisTemplate == null || objectMapper == null) {
            return null;
        }
        try {
            String raw = stringRedisTemplate.opsForValue().get(cacheKey);
            if (raw == null || raw.isBlank()) {
                return null;
            }
            JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, MeasurementRunSummaryVo.class);
            return objectMapper.readValue(raw, type);
        } catch (Exception ex) {
            log.warn("[measurement-run] cache read failed key={} reason={}", cacheKey, ex.getMessage());
            return null;
        }
    }

    private void writeCachedRuns(String cacheKey, List<MeasurementRunSummaryVo> result) {
        if (stringRedisTemplate == null || objectMapper == null) {
            return;
        }
        try {
            int ttl = 45 + ThreadLocalRandom.current().nextInt(0, 21);
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), java.time.Duration.ofSeconds(ttl));
        } catch (Exception ex) {
            log.warn("[measurement-run] cache write failed key={} reason={}", cacheKey, ex.getMessage());
        }
    }
}
