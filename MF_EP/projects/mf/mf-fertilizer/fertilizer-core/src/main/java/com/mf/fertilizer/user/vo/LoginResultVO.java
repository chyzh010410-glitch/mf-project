package com.mf.fertilizer.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultVO implements Serializable {

    private String token;
    private String username;
    private String realName;
    private String role;
}
