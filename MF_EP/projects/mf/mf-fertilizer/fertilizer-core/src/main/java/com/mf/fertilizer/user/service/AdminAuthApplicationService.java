package com.mf.fertilizer.user.service;

import com.mf.fertilizer.user.dto.LoginDTO;
import com.mf.fertilizer.user.vo.LoginResultVO;

public interface AdminAuthApplicationService {

    LoginResultVO login(LoginDTO dto);

    void logout(String authorization);
}
