package com.mf.fertilizer.merchant.controller;

import com.mf.fertilizer.context.UserContext;
import com.mf.fertilizer.merchant.dto.MerchantProfileDTO;
import com.mf.fertilizer.merchant.entity.Merchant;
import com.mf.fertilizer.merchant.service.MerchantApplicationService;
import com.mf.fertilizer.vo.ResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/merchant/profile")
@RequiredArgsConstructor
public class MerchantProfileController {

    private final MerchantApplicationService merchantApplicationService;

    @GetMapping
    public ResultVO<Merchant> profile() {
        return ResultVO.success(merchantApplicationService.getProfile(UserContext.requireUserId()));
    }

    @PutMapping
    public ResultVO<?> update(@Valid @RequestBody MerchantProfileDTO dto) {
        merchantApplicationService.updateProfile(UserContext.requireUserId(), dto);
        return ResultVO.success();
    }
}
