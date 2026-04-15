package com.simulab.modules.measurement.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class WaferAnalysisImportResultVo {

    private boolean imported;
    private String status;
    private String message;
    private Long lotId;
    private Long waferId;
    private Long measurementRunId;
    private String measurementRunNo;
    private int totalRows;
    private int insertedRows;
    private int skippedOutsideRows;
    private int failedRows;
    private long elapsedMs;
    private List<String> errors = new ArrayList<>();
}
