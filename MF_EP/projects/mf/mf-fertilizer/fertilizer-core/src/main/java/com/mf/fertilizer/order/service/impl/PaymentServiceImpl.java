package com.mf.fertilizer.order.service.impl;

import com.mf.fertilizer.constant.OrderStatus;
import com.mf.fertilizer.constant.ResultCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.exception.BusinessException;
import com.mf.fertilizer.order.entity.OrderEntity;
import com.mf.fertilizer.order.entity.OrderItem;
import com.mf.fertilizer.order.entity.Payment;
import com.mf.fertilizer.order.mapper.PaymentMapper;
import com.mf.fertilizer.order.service.OrderEntityService;
import com.mf.fertilizer.order.service.OrderItemService;
import com.mf.fertilizer.order.service.OrderStatusService;
import com.mf.fertilizer.order.service.PaymentService;
import com.mf.fertilizer.platform.service.PlatformConfigService;
import com.mf.fertilizer.product.service.ProductService;
import com.mf.fertilizer.product.service.StockService;
import com.mf.fertilizer.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService {

    private static final String PAYMENT_ENABLED_KEY = "payment_enabled";
    private static final String PAYMENT_TIMEOUT_MINUTES_KEY = "payment_timeout_minutes";
    private static final String DEFAULT_PAYMENT_METHOD = "mock";
    private static final String PAYMENT_SUCCESS = "success";
    private static final String PAYMENT_REFUNDED = "refunded";
    private static final Set<String> SUPPORTED_PAYMENT_METHODS = Set.of("wechat", "alipay", "mock");

    private final OrderEntityService orderService;
    private final OrderItemService orderItemService;
    private final OrderStatusService orderStatusService;
    private final ProductService productService;
    private final StockService stockService;
    private final PlatformConfigService configService;
    private final ReentrantLock stockLock = new ReentrantLock();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long userId, Long orderId, String paymentMethod) {
        if (!configService.getBoolean(PAYMENT_ENABLED_KEY, false)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "支付功能暂未开放");
        }

        var now = LocalDateTime.now();
        var paymentTimeoutMinutes = getPaymentTimeoutMinutes();
        var order = getUserOrder(userId, orderId);
        assertCanPay(order, now, paymentTimeoutMinutes);

        var method = normalizePaymentMethod(paymentMethod);
        var updated = orderService.lambdaUpdate()
                .eq(OrderEntity::getId, orderId)
                .eq(OrderEntity::getUserId, userId)
                .eq(OrderEntity::getStatus, OrderStatus.PENDING_PAY)
                .set(OrderEntity::getStatus, OrderStatus.PENDING_SHIP)
                .set(OrderEntity::getPaymentMethod, method)
                .set(OrderEntity::getPayTime, now)
                .update();
        if (!updated) {
            assertCanPay(getUserOrder(userId, orderId), now, paymentTimeoutMinutes);
        }

        saveOrUpdatePayment(order, method, now);
    }

    @Override
    public PageVO<Payment> listAdminPayments(PageDTO page, String status, String orderNo, String tradeNo) {
        var wrapper = new LambdaQueryWrapper<Payment>()
                .eq(status != null && !status.isBlank(), Payment::getStatus, status)
                .eq(orderNo != null && !orderNo.isBlank(), Payment::getOrderNo, orderNo)
                .eq(tradeNo != null && !tradeNo.isBlank(), Payment::getTradeNo, tradeNo)
                .orderByDesc(Payment::getPayTime)
                .orderByDesc(Payment::getCreateTime);
        var result = page(new Page<>(page.getPage(), page.getSize()), wrapper);
        return PageVO.of(page, result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(Long orderId, String reason) {
        var order = orderService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        orderStatusService.checkCanChangeTo(order, OrderStatus.REFUNDED);

        var payment = lambdaQuery()
                .eq(Payment::getOrderId, orderId)
                .last("limit 1")
                .one();
        if (payment == null || !PAYMENT_SUCCESS.equals(payment.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单没有可退款的成功支付记录");
        }

        var now = LocalDateTime.now();
        order.setStatus(OrderStatus.REFUNDED);
        order.setAdminRemark(buildRefundRemark(reason));
        restoreOrderStock(order);
        orderService.updateById(order);

        payment.setStatus(PAYMENT_REFUNDED);
        payment.setRefundAmount(payment.getAmount());
        payment.setRefundTime(now);
        payment.setRawResponse("{\"channel\":\"mock\",\"refund\":true}");
        saveOrUpdate(payment);
    }

    private OrderEntity getUserOrder(Long userId, Long orderId) {
        var order = orderService.lambdaQuery()
                .eq(OrderEntity::getId, orderId)
                .eq(OrderEntity::getUserId, userId)
                .one();
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private void assertCanPay(OrderEntity order, LocalDateTime now, long paymentTimeoutMinutes) {
        var paid = lambdaQuery()
                .eq(Payment::getOrderId, order.getId())
                .eq(Payment::getStatus, PAYMENT_SUCCESS)
                .last("limit 1")
                .one();
        if (paid != null || OrderStatus.PENDING_SHIP.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单已支付，请勿重复支付");
        }
        if (!OrderStatus.PENDING_PAY.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前订单状态不允许支付");
        }
        if (OrderPaymentRules.isExpired(order, now, paymentTimeoutMinutes)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单已超时，请重新下单");
        }
        orderStatusService.checkCanPay(order);
    }

    private long getPaymentTimeoutMinutes() {
        return OrderPaymentRules.normalizeTimeoutMinutes(
                configService.getInt(PAYMENT_TIMEOUT_MINUTES_KEY, (int) OrderPaymentRules.defaultTimeoutMinutes())
        );
    }

    private void saveOrUpdatePayment(OrderEntity order, String method, LocalDateTime payTime) {
        var payment = lambdaQuery()
                .eq(Payment::getOrderId, order.getId())
                .last("limit 1")
                .one();
        if (payment == null) {
            payment = new Payment();
        }
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setUserId(order.getUserId());
        payment.setAmount(order.getPayAmount());
        payment.setPayMethod(method);
        payment.setTradeNo("MOCK" + order.getId() + payTime.toString().replaceAll("[-:.T]", ""));
        payment.setStatus(PAYMENT_SUCCESS);
        payment.setPayTime(payTime);
        payment.setRawResponse("{\"channel\":\"mock\",\"success\":true}");
        saveOrUpdate(payment);
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return DEFAULT_PAYMENT_METHOD;
        }
        var method = paymentMethod.trim();
        if (!SUPPORTED_PAYMENT_METHODS.contains(method)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "暂不支持该支付方式");
        }
        return method;
    }

    private String buildRefundRemark(String reason) {
        if (reason == null || reason.isBlank()) {
            return "模拟退款";
        }
        return "模拟退款：" + reason.trim();
    }

    private void restoreOrderStock(OrderEntity order) {
        var items = orderItemService.lambdaQuery()
                .eq(OrderItem::getOrderId, order.getId())
                .list();
        stockLock.lock();
        try {
            for (var item : items) {
                stockService.rollbackProductStock(item.getProductId(), item.getQuantity());
                var product = productService.getById(item.getProductId());
                if (product != null) {
                    int currentStock = product.getStock() == null ? 0 : product.getStock();
                    product.setStock(currentStock + item.getQuantity());
                    int currentSales = product.getSalesCount() == null ? 0 : product.getSalesCount();
                    product.setSalesCount(Math.max(0, currentSales - item.getQuantity()));
                    productService.updateById(product);
                }
            }
        } finally {
            stockLock.unlock();
        }
    }
}
