package com.simulab.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "simulab.analysis")
public class AnalysisExecutionProperties {

    @Min(1)
    @Max(32)
    private int overlayExecutorPoolSize = 8;

    @Min(1)
    @Max(32)
    private int waferGenerateExecutorPoolSize = 8;

    @Min(1)
    @Max(32)
    private int overlayDensifyParallelism = 8;

    @Min(1)
    @Max(32)
    private int waferGenerateParallelism = 8;
}
