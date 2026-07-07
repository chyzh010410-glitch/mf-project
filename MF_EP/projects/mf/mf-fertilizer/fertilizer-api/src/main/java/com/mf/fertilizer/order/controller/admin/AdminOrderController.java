package com.mf.fertilizer.order.controller.admin;

import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.order.dto.admin.OrderRefundDTO;
import com.mf.fertilizer.order.dto.admin.OrderShipDTO;
import com.mf.fertilizer.order.entity.OrderEntity;
import com.mf.fertilizer.order.service.OrderApplicationService;
import com.mf.fertilizer.order.service.PaymentService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderApplicationService orderApplicationService;
    private final PaymentService paymentService;

    @GetMapping
    public ResultVO<PageVO<OrderEntity>> list(@ModelAttribute PageDTO page,
                                              @RequestParam(name = "status", required = false) String status,
                                              @RequestParam(name = "orderNo", required = false) String orderNo) {
        return ResultVO.success(orderApplicationService.listAdminOrders(page, status, orderNo));
    }

    @GetMapping("/{id}")
    public ResultVO<?> detail(@PathVariable Long id) {
        return ResultVO.success(orderApplicationService.getAdminOrderDetail(id));
    }

    @PostMapping("/{id}/ship")
    public ResultVO<?> ship(@PathVariable Long id, @RequestBody OrderShipDTO dto) {
        orderApplicationService.shipOrder(id, dto.getLogisticsCompany(), dto.getLogisticsNo());
        return ResultVO.success();
    }

    @GetMapping("/statistics")
    public ResultVO<?> statistics() {
        return ResultVO.success(orderApplicationService.getAdminOrderStatistics());
    }

    @PostMapping("/{id}/status")
    public ResultVO<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        orderApplicationService.changeStatus(id, body.get("status"));
        return ResultVO.success();
    }

    @PostMapping("/{id}/refund")
    public ResultVO<?> refund(@PathVariable Long id, @RequestBody(required = false) OrderRefundDTO dto) {
        paymentService.refundOrder(id, dto == null ? null : dto.getReason());
        return ResultVO.success();
    }
}
