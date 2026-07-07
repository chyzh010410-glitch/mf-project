package com.mf.fertilizer.order.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.mf.fertilizer.constant.OrderStatus;
import com.mf.fertilizer.order.entity.OrderEntity;
import com.mf.fertilizer.order.entity.OrderItem;
import com.mf.fertilizer.order.service.OrderEntityService;
import com.mf.fertilizer.order.service.OrderItemService;
import com.mf.fertilizer.order.service.OrderStatusService;
import com.mf.fertilizer.order.service.ShoppingCartItemService;
import com.mf.fertilizer.platform.service.NotificationService;
import com.mf.fertilizer.platform.service.PlatformConfigService;
import com.mf.fertilizer.product.entity.Product;
import com.mf.fertilizer.product.service.ProductService;
import com.mf.fertilizer.product.service.StockService;
import com.mf.fertilizer.user.service.UserAddressService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderApplicationServiceImplTest {

    private final OrderEntityService orderService = mock(OrderEntityService.class);
    private final OrderItemService orderItemService = mock(OrderItemService.class);
    private final ProductService productService = mock(ProductService.class);
    private final UserAddressService addressService = mock(UserAddressService.class);
    private final ShoppingCartItemService cartService = mock(ShoppingCartItemService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final OrderStatusService orderStatusService = mock(OrderStatusService.class);
    private final StockService stockService = mock(StockService.class);
    private final PlatformConfigService configService = mock(PlatformConfigService.class);

    @Test
    void cancelUserOrderRestoresReservedStockAndSalesCount() {
        var service = service();
        var order = order();
        var item = item();
        var product = product();
        mockUserOrder(order);
        mockOrderItems(item);
        when(productService.getById(3L)).thenReturn(product);
        when(orderService.updateById(order)).thenReturn(true);

        service.cancelUserOrder(8L, 10L, "不想买了");

        verify(orderStatusService).checkCanCancel(order);
        verify(stockService).rollbackProductStock(3L, 2);
        verify(productService).updateById(product);
        verify(orderService).updateById(order);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals("不想买了", order.getCancelReason());
        assertEquals(7, product.getStock());
        assertEquals(3, product.getSalesCount());
    }

    @Test
    void changeStatusToPendingShipFillsManualPaymentInfo() {
        var service = service();
        var order = order();
        when(orderService.getById(10L)).thenReturn(order);
        when(orderService.updateById(order)).thenReturn(true);

        service.changeStatus(10L, OrderStatus.PENDING_SHIP);

        verify(orderStatusService).checkCanChangeTo(order, OrderStatus.PENDING_SHIP);
        verify(orderService).updateById(order);
        assertEquals(OrderStatus.PENDING_SHIP, order.getStatus());
        assertEquals("manual", order.getPaymentMethod());
    }

    @Test
    void changeStatusNoopDoesNotRestoreStockAgain() {
        var service = service();
        var order = order();
        order.setStatus(OrderStatus.CANCELLED);
        when(orderService.getById(10L)).thenReturn(order);

        service.changeStatus(10L, OrderStatus.CANCELLED);

        verify(orderStatusService).checkCanChangeTo(order, OrderStatus.CANCELLED);
        verify(stockService, never()).rollbackProductStock(any(), any());
        verify(orderService, never()).updateById(any());
    }

    @Test
    void requestRefundMarksOrderRefundRequestedWithReason() {
        var service = service();
        var order = order();
        order.setStatus(OrderStatus.PENDING_SHIP);
        mockUserOrder(order);
        when(orderService.updateById(order)).thenReturn(true);

        service.requestRefund(8L, 10L, "不想要了");

        verify(orderStatusService).checkCanChangeTo(order, OrderStatus.REFUND_REQUESTED);
        verify(orderService).updateById(order);
        assertEquals(OrderStatus.REFUND_REQUESTED, order.getStatus());
        assertEquals("用户申请退款：不想要了", order.getAdminRemark());
    }

    @Test
    void closeTimeoutOrdersRestoresReservedStockAndCancelsOrder() {
        var service = service();
        var order = order();
        var item = item();
        var product = product();
        mockTimeoutOrders(order);
        mockOrderItems(item);
        when(productService.getById(3L)).thenReturn(product);
        when(orderService.updateById(order)).thenReturn(true);

        service.closeTimeoutOrders(LocalDateTime.of(2026, 6, 18, 12, 0));

        verify(orderStatusService).checkCanCancel(order);
        verify(stockService).rollbackProductStock(3L, 2);
        verify(productService).updateById(product);
        verify(orderService).updateById(order);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals("超时未支付，系统自动取消", order.getCancelReason());
        assertEquals(7, product.getStock());
        assertEquals(3, product.getSalesCount());
    }

    @Test
    void getPaymentTimeoutDeadlineUsesSharedPaymentTimeout() {
        var service = service();
        var now = LocalDateTime.of(2026, 6, 18, 12, 1);

        assertEquals(LocalDateTime.of(2026, 6, 18, 12, 0), service.getPaymentTimeoutDeadline(now));
    }

    @Test
    void getPaymentTimeoutDeadlineUsesConfiguredPaymentTimeout() {
        var service = service();
        var now = LocalDateTime.of(2026, 6, 18, 12, 30);
        when(configService.getInt("payment_timeout_minutes", 1)).thenReturn(15);

        assertEquals(LocalDateTime.of(2026, 6, 18, 12, 15), service.getPaymentTimeoutDeadline(now));
    }

    @Test
    void getUserOrderDetailIncludesPaymentExpireTimeForPendingPayOrder() {
        var service = service();
        var order = order();
        var createTime = LocalDateTime.of(2026, 6, 18, 12, 0);
        order.setCreateTime(createTime);
        mockUserOrder(order);
        mockOrderItems(item());

        var detail = service.getUserOrderDetail(8L, 10L);

        assertEquals(createTime.plusMinutes(1), detail.getPaymentExpireTime());
    }

    @Test
    void getUserOrderDetailUsesConfiguredPaymentExpireTime() {
        var service = service();
        var order = order();
        var createTime = LocalDateTime.of(2026, 6, 18, 12, 0);
        order.setCreateTime(createTime);
        mockUserOrder(order);
        mockOrderItems(item());
        when(configService.getInt("payment_timeout_minutes", 1)).thenReturn(15);

        var detail = service.getUserOrderDetail(8L, 10L);

        assertEquals(createTime.plusMinutes(15), detail.getPaymentExpireTime());
    }

    @Test
    void getUserOrderDetailOmitsPaymentExpireTimeForPaidOrder() {
        var service = service();
        var order = order();
        order.setStatus(OrderStatus.PENDING_SHIP);
        order.setCreateTime(LocalDateTime.of(2026, 6, 18, 12, 0));
        mockUserOrder(order);
        mockOrderItems(item());

        var detail = service.getUserOrderDetail(8L, 10L);

        assertNull(detail.getPaymentExpireTime());
    }

    @Test
    void getUserOrderDetailIncludesAdminRemarkForProgressInfo() {
        var service = service();
        var order = order();
        order.setStatus(OrderStatus.SHIPPED);
        order.setAdminRemark("物流: 京东物流 JD001");
        mockUserOrder(order);
        mockOrderItems(item());

        var detail = service.getUserOrderDetail(8L, 10L);

        assertEquals("物流: 京东物流 JD001", detail.getAdminRemark());
        assertEquals("京东物流", detail.getLogisticsCompany());
        assertEquals("JD001", detail.getLogisticsNo());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockUserOrder(OrderEntity order) {
        var query = mock(LambdaQueryChainWrapper.class);
        when(orderService.lambdaQuery()).thenReturn(query);
        when(query.eq(any(), any())).thenReturn(query);
        when(query.one()).thenReturn(order);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockOrderItems(OrderItem item) {
        var query = mock(LambdaQueryChainWrapper.class);
        when(orderItemService.lambdaQuery()).thenReturn(query);
        when(query.eq(any(), any())).thenReturn(query);
        when(query.list()).thenReturn(List.of(item));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockTimeoutOrders(OrderEntity order) {
        var query = mock(LambdaQueryChainWrapper.class);
        when(orderService.lambdaQuery()).thenReturn(query);
        when(query.eq(any(), any())).thenReturn(query);
        when(query.le(any(), any())).thenReturn(query);
        when(query.list()).thenReturn(List.of(order));
    }

    private OrderApplicationServiceImpl service() {
        return new OrderApplicationServiceImpl(
                orderService,
                orderItemService,
                productService,
                addressService,
                cartService,
                notificationService,
                orderStatusService,
                stockService,
                configService
        );
    }

    private OrderEntity order() {
        var order = new OrderEntity();
        order.setId(10L);
        order.setUserId(8L);
        order.setStatus(OrderStatus.PENDING_PAY);
        return order;
    }

    private OrderItem item() {
        var item = new OrderItem();
        item.setOrderId(10L);
        item.setProductId(3L);
        item.setQuantity(2);
        return item;
    }

    private Product product() {
        var product = new Product();
        product.setId(3L);
        product.setStock(5);
        product.setSalesCount(5);
        return product;
    }
}
