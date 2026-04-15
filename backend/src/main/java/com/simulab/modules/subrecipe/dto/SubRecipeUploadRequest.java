package com.simulab.modules.subrecipe.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubRecipeUploadRequest {

    @NotBlank
    private String fileName;

    @NotBlank
    private String fileType;
}
