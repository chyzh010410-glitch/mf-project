package com.mf.fertilizer.order.service.impl;

import com.mf.fertilizer.exception.BusinessException;
import com.mf.fertilizer.order.dto.client.CartAddDTO;
import com.mf.fertilizer.order.dto.client.CartUpdateDTO;
import com.mf.fertilizer.order.entity.ShoppingCartItem;
import com.mf.fertilizer.order.service.CartApplicationService;
import com.mf.fertilizer.order.service.ShoppingCartItemService;
import com.mf.fertilizer.order.vo.client.CartVO;
import com.mf.fertilizer.product.entity.Product;
import com.mf.fertilizer.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CartApplicationServiceImpl implements CartApplicationService {

    private final ShoppingCartItemService cartService;
    private final ProductService productService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public CartVO listCart(Long userId) {
        var items = cartService.lambdaQuery()
                .eq(ShoppingCartItem::getUserId, userId)
                .list();
        var itemVOs = new ArrayList<CartVO.CartItemVO>();
        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (ShoppingCartItem item : items) {
            Product product = productService.getById(item.getProductId());
            if (product == null || product.getStatus() == 0) {
                continue;
            }
            CartVO.CartItemVO itemVO = toCartItemVO(item, product);
            itemVOs.add(itemVO);
            if (item.getSelected() == 1) {
                totalCount += item.getQuantity();
                totalAmount = totalAmount.add(itemVO.getSubtotal());
            }
        }

        CartVO vo = new CartVO();
        vo.setItems(itemVOs);
        vo.setTotalCount(totalCount);
        vo.setTotalAmount(totalAmount);
        return vo;
    }

    @Override
    public void addCartItem(Long userId, CartAddDTO dto) {
        ShoppingCartItem existing = cartService.lambdaQuery()
                .eq(ShoppingCartItem::getUserId, userId)
                .eq(ShoppingCartItem::getProductId, dto.getProductId())
                .one();
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + dto.getQuantity());
            cartService.updateById(existing);
            return;
        }

        ShoppingCartItem restored = findDeletedCartItem(userId, dto.getProductId());
        if (restored != null) {
            jdbcTemplate.update(
                    "UPDATE shopping_cart_item SET deleted=0, quantity=?, selected=1 WHERE id=?",
                    dto.getQuantity(), restored.getId());
            return;
        }

        ShoppingCartItem item = new ShoppingCartItem();
        item.setUserId(userId);
        item.setProductId(dto.getProductId());
        item.setQuantity(dto.getQuantity());
        item.setSelected(1);
        cartService.save(item);
    }

    @Override
    public void updateCartItem(Long userId, Long itemId, CartUpdateDTO dto) {
        ShoppingCartItem item = requireCurrentUserCartItem(userId, itemId);
        if (dto.getQuantity() != null) {
            item.setQuantity(dto.getQuantity());
        }
        if (dto.getSelected() != null) {
            item.setSelected(dto.getSelected());
        }
        cartService.updateById(item);
    }

    @Override
    public void deleteCartItem(Long userId, Long itemId) {
        cartService.lambdaUpdate()
                .eq(ShoppingCartItem::getId, itemId)
                .eq(ShoppingCartItem::getUserId, userId)
                .remove();
    }

    @Override
    public void clearCart(Long userId) {
        cartService.lambdaUpdate()
                .eq(ShoppingCartItem::getUserId, userId)
                .remove();
    }

    private CartVO.CartItemVO toCartItemVO(ShoppingCartItem item, Product product) {
        CartVO.CartItemVO itemVO = new CartVO.CartItemVO();
        itemVO.setId(item.getId());
        itemVO.setProductId(item.getProductId());
        itemVO.setProductName(product.getName());
        itemVO.setProductImage(product.getCoverImage());
        itemVO.setProductType(product.getProductType());
        itemVO.setStock(product.getStock());
        itemVO.setPrice(product.getPrice());
        itemVO.setFreight(product.getFreight());
        itemVO.setQuantity(item.getQuantity());
        itemVO.setSelected(item.getSelected());
        itemVO.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        return itemVO;
    }

    private ShoppingCartItem requireCurrentUserCartItem(Long userId, Long itemId) {
        ShoppingCartItem item = cartService.lambdaQuery()
                .eq(ShoppingCartItem::getId, itemId)
                .eq(ShoppingCartItem::getUserId, userId)
                .one();
        if (item == null) {
            throw new BusinessException(404, "购物车商品不存在");
        }
        return item;
    }

    private ShoppingCartItem findDeletedCartItem(Long userId, Long productId) {
        var deletedList = jdbcTemplate.query(
                "SELECT * FROM shopping_cart_item WHERE user_id = ? AND product_id = ?",
                (rs, rowNum) -> {
                    ShoppingCartItem item = new ShoppingCartItem();
                    item.setId(rs.getLong("id"));
                    item.setUserId(rs.getLong("user_id"));
                    item.setProductId(rs.getLong("product_id"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setSelected(rs.getInt("selected"));
                    item.setDeleted(rs.getInt("deleted"));
                    return item;
                }, userId, productId);
        return deletedList.isEmpty() ? null : deletedList.get(0);
    }
}
