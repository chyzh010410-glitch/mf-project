package com.mf.fertilizer.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.order.entity.ShoppingCartItem;
import com.mf.fertilizer.order.mapper.ShoppingCartItemMapper;
import com.mf.fertilizer.order.service.ShoppingCartItemService;
import org.springframework.stereotype.Service;

@Service
public class ShoppingCartItemServiceImpl extends ServiceImpl<ShoppingCartItemMapper, ShoppingCartItem> implements ShoppingCartItemService {
}
