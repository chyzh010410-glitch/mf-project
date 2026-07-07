package com.mf.fertilizer.user.service.impl;

import com.mf.fertilizer.user.dto.LoginDTO;
import com.mf.fertilizer.user.service.AdminAuthApplicationService;
import com.mf.fertilizer.user.service.SysUserService;
import com.mf.fertilizer.user.vo.LoginResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthApplicationServiceImpl implements AdminAuthApplicationService {

    private final SysUserService sysUserService;

    @Override
    public LoginResultVO login(LoginDTO dto) {
        return sysUserService.login(dto.getUsername(), dto.getPassword());
    }

    @Override
    public void logout(String authorization) {
        String token = authorization.substring(7);
        sysUserService.logout(token);
    }
}
