package com.mf.fertilizer.user.controller.admin;

import com.mf.fertilizer.user.dto.LoginDTO;
import com.mf.fertilizer.user.service.AdminAuthApplicationService;
import com.mf.fertilizer.vo.ResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LoginController {

    private final AdminAuthApplicationService adminAuthApplicationService;

    @PostMapping("/login")
    public ResultVO<?> login(@Valid @RequestBody LoginDTO dto) {
        var result = adminAuthApplicationService.login(dto);
        return ResultVO.success(result);
    }

    @PostMapping("/logout")
    public ResultVO<?> logout(@RequestHeader("Authorization") String auth) {
        adminAuthApplicationService.logout(auth);
        return ResultVO.success();
    }
}
