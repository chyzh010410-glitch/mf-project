package com.mf.fertilizer.merchant.controller;

import com.mf.fertilizer.merchant.dto.MerchantLoginDTO;
import com.mf.fertilizer.merchant.dto.MerchantRegisterDTO;
import com.mf.fertilizer.merchant.service.MerchantApplicationService;
import com.mf.fertilizer.merchant.vo.MerchantLoginResultVO;
import com.mf.fertilizer.vo.ResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/merchant/auth")
@RequiredArgsConstructor
public class MerchantAuthController {

    private final MerchantApplicationService merchantApplicationService;

    @PostMapping("/register")
    public ResultVO<?> register(@Valid @RequestBody MerchantRegisterDTO dto) {
        merchantApplicationService.register(dto);
        return ResultVO.success();
    }

    @PostMapping("/login")
    public ResultVO<MerchantLoginResultVO> login(@Valid @RequestBody MerchantLoginDTO dto) {
        return ResultVO.success(merchantApplicationService.login(dto));
    }

    @PostMapping("/logout")
    public ResultVO<?> logout(@RequestHeader("Authorization") String authHeader) {
        merchantApplicationService.logout(authHeader);
        return ResultVO.success();
    }
}
