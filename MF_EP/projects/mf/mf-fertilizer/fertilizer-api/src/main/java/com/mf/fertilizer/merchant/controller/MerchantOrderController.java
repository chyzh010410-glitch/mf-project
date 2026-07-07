package com.mf.fertilizer.merchant.controller;

import com.mf.fertilizer.context.UserContext;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.order.dto.admin.OrderShipDTO;
import com.mf.fertilizer.order.service.OrderApplicationService;
import com.mf.fertilizer.order.vo.client.OrderVO;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/merchant/orders")
@RequiredArgsConstructor
public class MerchantOrderController {

    private final OrderApplicationService orderApplicationService;

    @GetMapping
    public ResultVO<PageVO<OrderVO>> list(@ModelAttribute PageDTO page,
                                          @RequestParam(required = false) String status) {
        return ResultVO.success(orderApplicationService.listMerchantOrders(UserContext.requireUserId(), page, status));
    }

    @GetMapping("/{id}")
    public ResultVO<OrderVO> detail(@PathVariable Long id) {
        return ResultVO.success(orderApplicationService.getMerchantOrderDetail(UserContext.requireUserId(), id));
    }

    @PostMapping("/{id}/ship")
    public ResultVO<?> ship(@PathVariable Long id, @Valid @RequestBody OrderShipDTO dto) {
        orderApplicationService.shipMerchantOrder(
                UserContext.requireUserId(),
                id,
                dto.getLogisticsCompany(),
                dto.getLogisticsNo()
        );
        return ResultVO.success();
    }
}
