package com.mf.fertilizer.fertilization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecommendRequestDTO {

    @NotBlank(message = "树种不能为空")
    private String species;

    @NotNull(message = "树龄不能为空")
    private Integer age;

    /** If omitted, the current season is used. */
    private String season;
}
