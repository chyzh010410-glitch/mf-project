package com.mf.fertilizer.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.order.entity.OrderItem;
import com.mf.fertilizer.order.mapper.OrderItemMapper;
import com.mf.fertilizer.order.service.OrderItemService;
import org.springframework.stereotype.Service;

@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements OrderItemService {
}
