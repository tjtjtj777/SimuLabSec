package com.simulab.modules.subrecipe.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubRecipeExportRequest {

    @NotBlank
    private String exportFormat;
}
