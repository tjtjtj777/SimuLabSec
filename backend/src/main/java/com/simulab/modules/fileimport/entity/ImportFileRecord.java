package com.simulab.modules.fileimport.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.simulab.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("import_file_record")
public class ImportFileRecord extends BaseEntity {

    private String fileNo;
    private Long datasetId;
    private String fileName;
    private String fileType;
    private String bizType;
    private String storagePath;
    private Long fileSize;
    private String checksum;
    private String status;
    private String validationSummaryJson;
    private String errorMessage;
    private Long uploadedBy;
}
