package com.mf.fertilizer.merchant.controller.admin;

import com.mf.fertilizer.annotation.OperationLog;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.merchant.dto.admin.MerchantRejectDTO;
import com.mf.fertilizer.merchant.entity.Merchant;
import com.mf.fertilizer.merchant.service.MerchantApplicationService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/merchants")
@RequiredArgsConstructor
public class AdminMerchantController {

    private final MerchantApplicationService merchantApplicationService;

    @GetMapping
    public ResultVO<PageVO<Merchant>> list(@ModelAttribute PageDTO page,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String keyword) {
        return ResultVO.success(merchantApplicationService.listAdminMerchants(page, status, keyword));
    }

    @GetMapping("/{id}")
    public ResultVO<Merchant> detail(@PathVariable Long id) {
        return ResultVO.success(merchantApplicationService.getAdminMerchant(id));
    }

    @PostMapping("/{id}/approve")
    @OperationLog(module = "商家管理", action = "审核通过")
    public ResultVO<?> approve(@PathVariable Long id) {
        merchantApplicationService.approve(id);
        return ResultVO.success();
    }

    @PostMapping("/{id}/reject")
    @OperationLog(module = "商家管理", action = "审核拒绝")
    public ResultVO<?> reject(@PathVariable Long id, @RequestBody(required = false) MerchantRejectDTO dto) {
        merchantApplicationService.reject(id, dto != null ? dto.getAuditRemark() : null);
        return ResultVO.success();
    }

    @PostMapping("/{id}/disable")
    @OperationLog(module = "商家管理", action = "禁用")
    public ResultVO<?> disable(@PathVariable Long id) {
        merchantApplicationService.disable(id);
        return ResultVO.success();
    }
}
