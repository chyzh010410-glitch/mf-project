package com.mf.fertilizer.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shopping_cart_item")
public class ShoppingCartItem extends BaseEntity {
    private Long userId;
    private Long productId;
    private Integer quantity;
    private Integer selected;
}
