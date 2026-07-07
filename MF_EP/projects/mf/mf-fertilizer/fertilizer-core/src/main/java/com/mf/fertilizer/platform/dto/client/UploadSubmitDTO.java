package com.mf.fertilizer.platform.dto.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class UploadSubmitDTO {
    @NotBlank private String name;
    private String location;
    private String description;
    private String features;
    @NotNull private List<String> images;
    private String tags;
}
