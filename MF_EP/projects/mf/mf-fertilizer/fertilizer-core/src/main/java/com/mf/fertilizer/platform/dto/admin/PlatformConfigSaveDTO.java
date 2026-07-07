package com.mf.fertilizer.platform.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlatformConfigSaveDTO {
    @NotBlank private String configKey;
    private String configValue;
    private String configGroup;
    private String description;
}
