package com.mf.fertilizer.user.dto.client;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetDTO {
    @NotBlank private String phone;
    @NotBlank private String code;
    @NotBlank private String newPassword;
}
