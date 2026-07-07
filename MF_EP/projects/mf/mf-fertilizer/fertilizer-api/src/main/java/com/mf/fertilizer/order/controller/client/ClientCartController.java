package com.mf.fertilizer.order.controller.client;

import com.mf.fertilizer.context.UserContext;
import com.mf.fertilizer.order.dto.client.CartAddDTO;
import com.mf.fertilizer.order.dto.client.CartUpdateDTO;
import com.mf.fertilizer.order.service.CartApplicationService;
import com.mf.fertilizer.order.vo.client.CartVO;
import com.mf.fertilizer.vo.ResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client/cart")
@RequiredArgsConstructor
public class ClientCartController {

    private final CartApplicationService cartApplicationService;

    @GetMapping
    public ResultVO<CartVO> list() {
        return ResultVO.success(cartApplicationService.listCart(UserContext.getUserId()));
    }

    @PostMapping
    public ResultVO<?> add(@Valid @RequestBody CartAddDTO dto) {
        cartApplicationService.addCartItem(UserContext.getUserId(), dto);
        return ResultVO.success();
    }

    @PutMapping("/{id}")
    public ResultVO<?> update(@PathVariable Long id, @RequestBody CartUpdateDTO dto) {
        cartApplicationService.updateCartItem(UserContext.getUserId(), id, dto);
        return ResultVO.success();
    }

    @DeleteMapping("/{id}")
    public ResultVO<?> delete(@PathVariable Long id) {
        cartApplicationService.deleteCartItem(UserContext.getUserId(), id);
        return ResultVO.success();
    }

    @DeleteMapping
    public ResultVO<?> clear() {
        cartApplicationService.clearCart(UserContext.getUserId());
        return ResultVO.success();
    }
}
