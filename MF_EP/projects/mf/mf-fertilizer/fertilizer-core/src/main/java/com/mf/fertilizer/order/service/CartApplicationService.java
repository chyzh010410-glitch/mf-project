package com.mf.fertilizer.order.service;

import com.mf.fertilizer.order.dto.client.CartAddDTO;
import com.mf.fertilizer.order.dto.client.CartUpdateDTO;
import com.mf.fertilizer.order.vo.client.CartVO;

public interface CartApplicationService {

    CartVO listCart(Long userId);

    void addCartItem(Long userId, CartAddDTO dto);

    void updateCartItem(Long userId, Long itemId, CartUpdateDTO dto);

    void deleteCartItem(Long userId, Long itemId);

    void clearCart(Long userId);
}
