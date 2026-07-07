package com.mf.fertilizer.user.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminLoginDTO {
    @NotBlank private String username;
    @NotBlank private String password;
}
