package com.simulab.modules.subrecipe.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SubRecipeFileTicketVo {

    private Long subRecipeId;
    private String fileName;
    private String fileType;
    private String action;
    private String objectPath;
    private LocalDateTime expireAt;
}
