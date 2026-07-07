package com.mf.fertilizer.user.dto.client;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserLoginDTO {
    private String username;
    private String phone;
    @NotBlank private String password;
}
