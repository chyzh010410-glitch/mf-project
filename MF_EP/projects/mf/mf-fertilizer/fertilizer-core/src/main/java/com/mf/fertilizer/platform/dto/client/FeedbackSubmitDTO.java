package com.mf.fertilizer.platform.dto.client;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class FeedbackSubmitDTO {
    @NotBlank private String type;
    @NotBlank private String content;
    private List<String> images;
    private String contact;
}
