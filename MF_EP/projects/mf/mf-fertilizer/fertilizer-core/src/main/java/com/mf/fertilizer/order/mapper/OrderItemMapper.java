package com.mf.fertilizer.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mf.fertilizer.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
