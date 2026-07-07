package com.mf.fertilizer.order.controller.admin;

import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.order.entity.Payment;
import com.mf.fertilizer.order.service.PaymentService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public ResultVO<PageVO<Payment>> list(@ModelAttribute PageDTO page,
                                          @RequestParam(name = "status", required = false) String status,
                                          @RequestParam(name = "orderNo", required = false) String orderNo,
                                          @RequestParam(name = "tradeNo", required = false) String tradeNo) {
        return ResultVO.success(paymentService.listAdminPayments(page, status, orderNo, tradeNo));
    }
}
