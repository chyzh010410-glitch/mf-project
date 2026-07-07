package com.mf.fertilizer.user.dto.client;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRegisterDTO {
    @NotBlank private String username;
    @NotBlank private String password;
    @NotBlank private String phone;
    @NotBlank private String code;
}
