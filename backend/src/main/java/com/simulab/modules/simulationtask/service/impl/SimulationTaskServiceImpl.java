package com.simulab.modules.simulationtask.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.simulab.common.exception.BusinessException;
import com.simulab.common.security.SecurityContextUtils;
import com.simulab.modules.simulationtask.dto.SimulationTaskQueryDto;
import com.simulab.modules.simulationtask.entity.SimulationTask;
import com.simulab.modules.simulationtask.mapper.SimulationTaskMapper;
import com.simulab.modules.simulationtask.service.SimulationTaskService;
import com.simulab.modules.simulationtask.vo.SimulationTaskDetailVo;
import com.simulab.modules.simulationtask.vo.SimulationTaskStatusSummaryVo;
import com.simulab.modules.simulationtask.vo.SimulationTaskSummaryVo;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SimulationTaskServiceImpl implements SimulationTaskService {

    private final SimulationTaskMapper simulationTaskMapper;

    public SimulationTaskServiceImpl(SimulationTaskMapper simulationTaskMapper) {
        this.simulationTaskMapper = simulationTaskMapper;
    }

    @Override
    public List<SimulationTaskSummaryVo> listTasks(SimulationTaskQueryDto queryDto) {
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        return simulationTaskMapper.selectList(new LambdaQueryWrapper<SimulationTask>()
                .and(StringUtils.hasText(queryDto.getKeyword()), wrapper -> wrapper
                    .like(SimulationTask::getTaskNo, queryDto.getKeyword())
                    .or()
                    .like(SimulationTask::getTaskName, queryDto.getKeyword()))
                .eq(StringUtils.hasText(queryDto.getStatus()), SimulationTask::getStatus, queryDto.getStatus())
                .eq(StringUtils.hasText(queryDto.getScenarioType()), SimulationTask::getScenarioType, queryDto.getScenarioType())
                .eq(queryDto.getLotId() != null, SimulationTask::getLotId, queryDto.getLotId())
                .eq(queryDto.getLayerId() != null, SimulationTask::getLayerId, queryDto.getLayerId())
                .eq(queryDto.getRecipeVersionId() != null, SimulationTask::getRecipeVersionId, queryDto.getRecipeVersionId())
                .and(wrapper -> wrapper.eq(SimulationTask::getCreatedBy, 0L).or().eq(SimulationTask::getCreatedBy, currentUserId))
                .eq(SimulationTask::getDeleted, 0)
                .orderByAsc(SimulationTask::getCreatedBy)
                .orderByDesc(SimulationTask::getCreatedAt)
                .orderByDesc(SimulationTask::getId))
            .stream()
            .map(this::toSummaryVo)
            .toList();
    }

    @Override
    public SimulationTaskDetailVo getTaskDetail(Long taskId) {
        Long currentUserId = SecurityContextUtils.currentUserIdOrThrow();
        SimulationTask task = simulationTaskMapper.selectOne(new LambdaQueryWrapper<SimulationTask>()
            .eq(SimulationTask::getId, taskId)
            .and(wrapper -> wrapper.eq(SimulationTask::getCreatedBy, 0L).or().eq(SimulationTask::getCreatedBy, currentUserId))
            .eq(SimulationTask::getDeleted, 0));
        if (task == null) {
            throw new BusinessException("TASK_NOT_FOUND", "任务不存在");
        }
        SimulationTaskDetailVo vo = new SimulationTaskDetailVo();
        vo.setId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setTaskName(task.getTaskName());
        vo.setLotId(task.getLotId());
        vo.setLayerId(task.getLayerId());
        vo.setRecipeVersionId(task.getRecipeVersionId());
        vo.setScenarioType(task.getScenarioType());
        vo.setStatus(task.getStatus());
        vo.setPriorityLevel(task.getPriorityLevel());
        vo.setIdempotencyKey(task.getIdempotencyKey());
        vo.setInputSnapshotJson(task.getInputSnapshotJson());
        vo.setExecutionContextJson(task.getExecutionContextJson());
        vo.setResultSummaryJson(task.getResultSummaryJson());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setRequestedBy(task.getRequestedBy());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());
        return vo;
    }

    @Override
    public List<SimulationTaskStatusSummaryVo> summarizeByStatus(SimulationTaskQueryDto queryDto) {
        Map<String, Long> grouped = listTasks(queryDto).stream()
            .collect(java.util.stream.Collectors.groupingBy(SimulationTaskSummaryVo::getStatus, java.util.stream.Collectors.counting()));
        return grouped.entrySet().stream()
            .map(entry -> {
                SimulationTaskStatusSummaryVo vo = new SimulationTaskStatusSummaryVo();
                vo.setStatus(entry.getKey());
                vo.setCount(entry.getValue());
                return vo;
            })
            .sorted(Comparator.comparing(SimulationTaskStatusSummaryVo::getStatus))
            .toList();
    }

    private SimulationTaskSummaryVo toSummaryVo(SimulationTask task) {
        SimulationTaskSummaryVo vo = new SimulationTaskSummaryVo();
        vo.setId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setTaskName(task.getTaskName());
        vo.setScenarioType(task.getScenarioType());
        vo.setStatus(task.getStatus());
        vo.setLotId(task.getLotId());
        vo.setLayerId(task.getLayerId());
        vo.setRecipeVersionId(task.getRecipeVersionId());
        vo.setPriorityLevel(task.getPriorityLevel());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setDataScope(task.getCreatedBy() != null && task.getCreatedBy() == 0L ? "DEMO" : "MINE");
        return vo;
    }
}
