package com.simulab.modules.simulationtask.service;

import com.simulab.modules.simulationtask.dto.SimulationTaskQueryDto;
import com.simulab.modules.simulationtask.vo.SimulationTaskDetailVo;
import com.simulab.modules.simulationtask.vo.SimulationTaskStatusSummaryVo;
import com.simulab.modules.simulationtask.vo.SimulationTaskSummaryVo;
import java.util.List;

public interface SimulationTaskService {

    List<SimulationTaskSummaryVo> listTasks(SimulationTaskQueryDto queryDto);

    SimulationTaskDetailVo getTaskDetail(Long taskId);

    List<SimulationTaskStatusSummaryVo> summarizeByStatus(SimulationTaskQueryDto queryDto);
}
