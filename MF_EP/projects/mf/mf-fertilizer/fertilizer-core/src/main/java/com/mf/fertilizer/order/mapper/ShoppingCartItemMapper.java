package com.mf.fertilizer.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mf.fertilizer.order.entity.ShoppingCartItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShoppingCartItemMapper extends BaseMapper<ShoppingCartItem> {
}
