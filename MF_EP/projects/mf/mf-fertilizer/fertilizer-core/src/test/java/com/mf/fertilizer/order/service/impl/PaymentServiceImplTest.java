package com.mf.fertilizer.order.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.mf.fertilizer.constant.OrderStatus;
import com.mf.fertilizer.constant.ResultCode;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.exception.BusinessException;
import com.mf.fertilizer.order.entity.OrderEntity;
import com.mf.fertilizer.order.entity.OrderItem;
import com.mf.fertilizer.order.entity.Payment;
import com.mf.fertilizer.order.service.OrderEntityService;
import com.mf.fertilizer.order.service.OrderItemService;
import com.mf.fertilizer.order.service.OrderStatusService;
import com.mf.fertilizer.platform.service.PlatformConfigService;
import com.mf.fertilizer.product.entity.Product;
import com.mf.fertilizer.product.service.ProductService;
import com.mf.fertilizer.product.service.StockService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceImplTest {

    private final OrderEntityService orderService = mock(OrderEntityService.class);
    private final OrderItemService orderItemService = mock(OrderItemService.class);
    private final OrderStatusService orderStatusService = mock(OrderStatusService.class);
    private final ProductService productService = mock(ProductService.class);
    private final StockService stockService = mock(StockService.class);
    private final PlatformConfigService configService = mock(PlatformConfigService.class);

    @Test
    void payOrderRejectsWhenPaymentDisabled() {
        var service = service();
        when(configService.getBoolean("payment_enabled", false)).thenReturn(false);

        var exception = assertThrows(BusinessException.class,
                () -> service.payOrder(8L, 10L, "wechat"));

        assertEquals(ResultCode.BAD_REQUEST, exception.getCode());
        assertEquals("支付功能暂未开放", exception.getMessage());
    }

    @Test
    void payOrderUpdatesOrderAndCreatesPayment() {
        var service = spy(service());
        var order = pendingOrder();
        mockUserOrder(order);
        mockNoSuccessfulPayment(service);
        mockOrderUpdate(true);
        mockExistingPayment(service, null);
        doReturn(true).when(service).saveOrUpdate(any(Payment.class));
        when(configService.getBoolean("payment_enabled", false)).thenReturn(true);

        service.payOrder(8L, 10L, "alipay");

        verify(orderStatusService).checkCanPay(order);
        var captor = ArgumentCaptor.forClass(Payment.class);
        verify(service).saveOrUpdate(captor.capture());
        var payment = captor.getValue();
        assertEquals(10L, payment.getOrderId());
        assertEquals("MF001", payment.getOrderNo());
        assertEquals(8L, payment.getUserId());
        assertEquals(new BigDecimal("16.00"), payment.getAmount());
        assertEquals("alipay", payment.getPayMethod());
        assertEquals("success", payment.getStatus());
    }

    @Test
    void payOrderRejectsAlreadyPaidOrder() {
        var service = spy(service());
        var order = pendingOrder();
        mockUserOrder(order);
        mockSuccessfulPayment(service);
        when(configService.getBoolean("payment_enabled", false)).thenReturn(true);

        var exception = assertThrows(BusinessException.class,
                () -> service.payOrder(8L, 10L, "wechat"));

        assertEquals(ResultCode.BAD_REQUEST, exception.getCode());
        assertEquals("订单已支付，请勿重复支付", exception.getMessage());
    }

    @Test
    void payOrderRejectsUnsupportedPaymentMethod() {
        var service = spy(service());
        var order = pendingOrder();
        mockUserOrder(order);
        mockNoSuccessfulPayment(service);
        when(configService.getBoolean("payment_enabled", false)).thenReturn(true);

        var exception = assertThrows(BusinessException.class,
                () -> service.payOrder(8L, 10L, "bank_card"));

        assertEquals(ResultCode.BAD_REQUEST, exception.getCode());
        assertEquals("暂不支持该支付方式", exception.getMessage());
    }

    @Test
    void payOrderRejectsExpiredPendingPayOrder() {
        var service = spy(service());
        var order = pendingOrder();
        order.setCreateTime(LocalDateTime.now().minusMinutes(2));
        mockUserOrder(order);
        mockNoSuccessfulPayment(service);
        when(configService.getBoolean("payment_enabled", false)).thenReturn(true);

        var exception = assertThrows(BusinessException.class,
                () -> service.payOrder(8L, 10L, "wechat"));

        assertEquals(ResultCode.BAD_REQUEST, exception.getCode());
        assertEquals("订单已超时，请重新下单", exception.getMessage());
    }

    @Test
    void payOrderUsesConfiguredPaymentTimeout() {
        var service = spy(service());
        var order = pendingOrder();
        order.setCreateTime(LocalDateTime.now().minusMinutes(2));
        mockUserOrder(order);
        mockNoSuccessfulPayment(service);
        mockOrderUpdate(true);
        mockExistingPayment(service, null);
        doReturn(true).when(service).saveOrUpdate(any(Payment.class));
        when(configService.getBoolean("payment_enabled", false)).thenReturn(true);
        when(configService.getInt("payment_timeout_minutes", 1)).thenReturn(3);

        service.payOrder(8L, 10L, "wechat");

        verify(orderStatusService).checkCanPay(order);
    }

    @Test
    void listAdminPaymentsReturnsPagedPayments() {
        var service = spy(service());
        var page = new PageDTO();
        page.setPage(2);
        page.setSize(5);
        var payment = new Payment();
        payment.setOrderNo("MF001");
        var resultPage = new Page<Payment>(2, 5);
        resultPage.setTotal(1);
        resultPage.setRecords(java.util.List.of(payment));
        doReturn(resultPage).when(service).page(any(Page.class), any());

        var result = service.listAdminPayments(page, "success", "MF001", "MOCK001");

        assertEquals(1, result.getTotal());
        assertEquals("MF001", result.getRecords().get(0).getOrderNo());
    }

    @Test
    void refundOrderUpdatesOrderAndPayment() {
        var service = spy(service());
        var order = paidOrder();
        var payment = successPayment();
        var item = orderItem();
        var product = product();
        when(orderService.getById(10L)).thenReturn(order);
        when(orderService.updateById(order)).thenReturn(true);
        mockPaymentByOrder(service, payment);
        mockOrderItems(item);
        when(productService.getById(3L)).thenReturn(product);
        doReturn(true).when(service).saveOrUpdate(any(Payment.class));

        service.refundOrder(10L, "用户申请退款");

        verify(orderStatusService).checkCanChangeTo(order, OrderStatus.REFUNDED);
        verify(stockService).rollbackProductStock(3L, 2);
        verify(productService).updateById(product);
        verify(orderService).updateById(order);
        var captor = ArgumentCaptor.forClass(Payment.class);
        verify(service).saveOrUpdate(captor.capture());
        assertEquals(OrderStatus.REFUNDED, order.getStatus());
        assertEquals("模拟退款：用户申请退款", order.getAdminRemark());
        assertEquals(7, product.getStock());
        assertEquals(3, product.getSalesCount());
        assertEquals("refunded", captor.getValue().getStatus());
        assertEquals(new BigDecimal("16.00"), captor.getValue().getRefundAmount());
    }

    @Test
    void refundOrderRejectsWithoutSuccessfulPayment() {
        var service = spy(service());
        var order = paidOrder();
        var payment = successPayment();
        payment.setStatus("refunded");
        when(orderService.getById(10L)).thenReturn(order);
        mockPaymentByOrder(service, payment);

        var exception = assertThrows(BusinessException.class,
                () -> service.refundOrder(10L, "重复退款"));

        assertEquals(ResultCode.BAD_REQUEST, exception.getCode());
        assertEquals("订单没有可退款的成功支付记录", exception.getMessage());
    }

    private PaymentServiceImpl service() {
        return new PaymentServiceImpl(orderService, orderItemService, orderStatusService, productService, stockService, configService);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockUserOrder(OrderEntity order) {
        var query = mock(LambdaQueryChainWrapper.class);
        when(orderService.lambdaQuery()).thenReturn(query);
        when(query.eq(any(), any())).thenReturn(query);
        when(query.one()).thenReturn(order);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockOrderUpdate(boolean updated) {
        var update = mock(LambdaUpdateChainWrapper.class);
        when(orderService.lambdaUpdate()).thenReturn(update);
        when(update.eq(any(), any())).thenReturn(update);
        when(update.set(any(), any())).thenReturn(update);
        when(update.update()).thenReturn(updated);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockNoSuccessfulPayment(PaymentServiceImpl service) {
        var query = mock(LambdaQueryChainWrapper.class);
        doReturn(query).when(service).lambdaQuery();
        when(query.eq(any(), any())).thenReturn(query);
        when(query.last(eq("limit 1"))).thenReturn(query);
        when(query.one()).thenReturn(null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockSuccessfulPayment(PaymentServiceImpl service) {
        var query = mock(LambdaQueryChainWrapper.class);
        doReturn(query).when(service).lambdaQuery();
        when(query.eq(any(), any())).thenReturn(query);
        when(query.last(eq("limit 1"))).thenReturn(query);
        when(query.one()).thenReturn(new Payment());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockExistingPayment(PaymentServiceImpl service, Payment payment) {
        var query = mock(LambdaQueryChainWrapper.class);
        doReturn(query).when(service).lambdaQuery();
        when(query.eq(any(), any())).thenReturn(query);
        when(query.last(eq("limit 1"))).thenReturn(query);
        when(query.one()).thenReturn(null, payment);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockPaymentByOrder(PaymentServiceImpl service, Payment payment) {
        var query = mock(LambdaQueryChainWrapper.class);
        doReturn(query).when(service).lambdaQuery();
        when(query.eq(any(), any())).thenReturn(query);
        when(query.last(eq("limit 1"))).thenReturn(query);
        when(query.one()).thenReturn(payment);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockOrderItems(OrderItem item) {
        var query = mock(LambdaQueryChainWrapper.class);
        when(orderItemService.lambdaQuery()).thenReturn(query);
        when(query.eq(any(), any())).thenReturn(query);
        when(query.list()).thenReturn(java.util.List.of(item));
    }

    private OrderEntity pendingOrder() {
        var order = new OrderEntity();
        order.setId(10L);
        order.setOrderNo("MF001");
        order.setUserId(8L);
        order.setStatus(OrderStatus.PENDING_PAY);
        order.setPayAmount(new BigDecimal("16.00"));
        return order;
    }

    private OrderEntity paidOrder() {
        var order = pendingOrder();
        order.setStatus(OrderStatus.PENDING_SHIP);
        return order;
    }

    private Payment successPayment() {
        var payment = new Payment();
        payment.setOrderId(10L);
        payment.setOrderNo("MF001");
        payment.setUserId(8L);
        payment.setAmount(new BigDecimal("16.00"));
        payment.setStatus("success");
        return payment;
    }

    private OrderItem orderItem() {
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
