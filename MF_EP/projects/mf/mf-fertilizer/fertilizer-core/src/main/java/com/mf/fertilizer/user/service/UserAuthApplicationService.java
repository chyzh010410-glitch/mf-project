package com.mf.fertilizer.user.service;

import com.mf.fertilizer.user.dto.client.PasswordResetDTO;
import com.mf.fertilizer.user.dto.client.UserLoginDTO;
import com.mf.fertilizer.user.dto.client.UserRegisterDTO;
import com.mf.fertilizer.user.vo.client.UserLoginResultVO;

public interface UserAuthApplicationService {

    void register(UserRegisterDTO dto);

    UserLoginResultVO login(UserLoginDTO dto);

    void logout(String authHeader);

    String createCaptcha(String target, String type);

    void resetPassword(PasswordResetDTO dto);
}
