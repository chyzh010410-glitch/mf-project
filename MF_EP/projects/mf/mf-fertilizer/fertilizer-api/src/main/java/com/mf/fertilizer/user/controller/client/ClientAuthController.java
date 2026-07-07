package com.mf.fertilizer.user.controller.client;

import com.mf.fertilizer.user.dto.client.PasswordResetDTO;
import com.mf.fertilizer.user.dto.client.UserLoginDTO;
import com.mf.fertilizer.user.dto.client.UserRegisterDTO;
import com.mf.fertilizer.user.service.UserAuthApplicationService;
import com.mf.fertilizer.user.vo.client.UserLoginResultVO;
import com.mf.fertilizer.vo.ResultVO;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client/auth")
@RequiredArgsConstructor
public class ClientAuthController {

    private final UserAuthApplicationService userAuthApplicationService;

    @PostMapping("/register")
    public ResultVO<?> register(@Valid @RequestBody UserRegisterDTO dto) {
        userAuthApplicationService.register(dto);
        return ResultVO.success();
    }

    @PostMapping("/login")
    public ResultVO<UserLoginResultVO> login(@Valid @RequestBody UserLoginDTO dto) {
        return ResultVO.success(userAuthApplicationService.login(dto));
    }

    @PostMapping("/logout")
    public ResultVO<?> logout(@RequestHeader("Authorization") String authHeader) {
        userAuthApplicationService.logout(authHeader);
        return ResultVO.success();
    }

    @PostMapping("/captcha")
    public ResultVO<String> captcha(@RequestBody CaptchaRequest dto) {
        return ResultVO.success(userAuthApplicationService.createCaptcha(dto.getTarget(), dto.getType()));
    }

    @PostMapping("/reset-password")
    public ResultVO<?> resetPassword(@Valid @RequestBody PasswordResetDTO dto) {
        userAuthApplicationService.resetPassword(dto);
        return ResultVO.success();
    }

    @Data
    public static class CaptchaRequest {
        private String target;
        private String type;
    }
}
