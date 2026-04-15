package com.simulab.modules.simulationtask.controller;

import com.simulab.common.api.ApiResponse;
import com.simulab.modules.simulationtask.dto.SimulationTaskQueryDto;
import com.simulab.modules.simulationtask.service.SimulationTaskService;
import com.simulab.modules.simulationtask.vo.SimulationTaskDetailVo;
import com.simulab.modules.simulationtask.vo.SimulationTaskStatusSummaryVo;
import com.simulab.modules.simulationtask.vo.SimulationTaskSummaryVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulation-tasks")
@Validated
public class SimulationTaskController {

    private final SimulationTaskService simulationTaskService;

    public SimulationTaskController(SimulationTaskService simulationTaskService) {
        this.simulationTaskService = simulationTaskService;
    }

    @GetMapping
    public ApiResponse<List<SimulationTaskSummaryVo>> list(@Valid @ModelAttribute SimulationTaskQueryDto queryDto) {
        return ApiResponse.success(simulationTaskService.listTasks(queryDto));
    }

    @GetMapping("/status-summary")
    public ApiResponse<List<SimulationTaskStatusSummaryVo>> statusSummary(@Valid @ModelAttribute SimulationTaskQueryDto queryDto) {
        return ApiResponse.success(simulationTaskService.summarizeByStatus(queryDto));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<SimulationTaskDetailVo> detail(@PathVariable Long taskId) {
        return ApiResponse.success(simulationTaskService.getTaskDetail(taskId));
    }
}
