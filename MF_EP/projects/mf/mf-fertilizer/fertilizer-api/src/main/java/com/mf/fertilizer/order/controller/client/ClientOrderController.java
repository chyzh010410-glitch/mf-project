package com.mf.fertilizer.order.controller.client;

import com.mf.fertilizer.context.UserContext;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.order.dto.client.OrderCreateDTO;
import com.mf.fertilizer.order.dto.client.PayOrderDTO;
import com.mf.fertilizer.order.service.OrderApplicationService;
import com.mf.fertilizer.order.service.PaymentService;
import com.mf.fertilizer.order.vo.client.OrderVO;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client/orders")
@RequiredArgsConstructor
public class ClientOrderController {

    private final OrderApplicationService orderApplicationService;
    private final PaymentService paymentService;

    @PostMapping
    public ResultVO<?> create(@Valid @RequestBody OrderCreateDTO dto) {
        return ResultVO.success(orderApplicationService.createOrder(UserContext.getUserId(), dto));
    }

    @GetMapping
    public ResultVO<PageVO<OrderVO>> list(@ModelAttribute PageDTO page,
                                          @RequestParam(required = false) String status) {
        return ResultVO.success(orderApplicationService.listUserOrders(UserContext.getUserId(), page, status));
    }

    @GetMapping("/{id}")
    public ResultVO<OrderVO> detail(@PathVariable Long id) {
        return ResultVO.success(orderApplicationService.getUserOrderDetail(UserContext.getUserId(), id));
    }

    @PostMapping("/{id}/cancel")
    public ResultVO<?> cancel(@PathVariable Long id, @RequestBody(required = false) CancelRequest body) {
        orderApplicationService.cancelUserOrder(UserContext.getUserId(), id, body != null ? body.getReason() : null);
        return ResultVO.success();
    }

    @PostMapping("/{id}/refund-request")
    public ResultVO<?> requestRefund(@PathVariable Long id, @RequestBody(required = false) CancelRequest body) {
        orderApplicationService.requestRefund(UserContext.getUserId(), id, body != null ? body.getReason() : null);
        return ResultVO.success();
    }

    @PostMapping("/{id}/pay")
    public ResultVO<?> pay(@PathVariable Long id, @Valid @RequestBody(required = false) PayOrderDTO body) {
        paymentService.payOrder(UserContext.getUserId(), id, body != null ? body.getPaymentMethod() : null);
        return ResultVO.success();
    }

    @PostMapping("/{id}/confirm")
    public ResultVO<?> confirm(@PathVariable Long id) {
        orderApplicationService.completeUserOrder(UserContext.getUserId(), id);
        return ResultVO.success();
    }

    @Data
    public static class CancelRequest {
        private String reason;
    }
}
