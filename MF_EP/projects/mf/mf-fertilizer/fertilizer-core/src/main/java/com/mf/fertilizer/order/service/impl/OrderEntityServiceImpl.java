package com.mf.fertilizer.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.order.entity.OrderEntity;
import com.mf.fertilizer.order.mapper.OrderEntityMapper;
import com.mf.fertilizer.order.service.OrderEntityService;
import org.springframework.stereotype.Service;

@Service
public class OrderEntityServiceImpl extends ServiceImpl<OrderEntityMapper, OrderEntity> implements OrderEntityService {
}
