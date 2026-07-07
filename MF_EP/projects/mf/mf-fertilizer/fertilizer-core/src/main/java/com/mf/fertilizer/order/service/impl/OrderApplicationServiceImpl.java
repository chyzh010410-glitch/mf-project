package com.mf.fertilizer.order.service.impl;

import com.mf.fertilizer.constant.OrderStatus;
import com.mf.fertilizer.constant.ResultCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.exception.BusinessException;
import com.mf.fertilizer.order.dto.client.OrderCreateDTO;
import com.mf.fertilizer.order.entity.OrderEntity;
import com.mf.fertilizer.order.entity.OrderItem;
import com.mf.fertilizer.order.entity.ShoppingCartItem;
import com.mf.fertilizer.order.service.OrderApplicationService;
import com.mf.fertilizer.order.service.OrderEntityService;
import com.mf.fertilizer.order.service.OrderItemService;
import com.mf.fertilizer.order.service.OrderStatusService;
import com.mf.fertilizer.order.service.ShoppingCartItemService;
import com.mf.fertilizer.order.vo.client.OrderCreateResultVO;
import com.mf.fertilizer.order.vo.client.OrderVO;
import com.mf.fertilizer.platform.service.NotificationService;
import com.mf.fertilizer.platform.service.PlatformConfigService;
import com.mf.fertilizer.product.entity.Product;
import com.mf.fertilizer.product.service.ProductService;
import com.mf.fertilizer.product.service.StockService;
import com.mf.fertilizer.user.entity.UserAddress;
import com.mf.fertilizer.user.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.mf.fertilizer.vo.PageVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApplicationServiceImpl implements OrderApplicationService {

    private static final String MANUAL_PAYMENT_METHOD = "manual";
    private static final String PAYMENT_TIMEOUT_MINUTES_KEY = "payment_timeout_minutes";

    private final OrderEntityService orderService;
    private final OrderItemService orderItemService;
    private final ProductService productService;
    private final UserAddressService addressService;
    private final ShoppingCartItemService cartService;
    private final NotificationService notificationService;
    private final OrderStatusService orderStatusService;
    private final StockService stockService;
    private final PlatformConfigService configService;
    private final ReentrantLock stockLock = new ReentrantLock();

    @Override
    public PageVO<OrderVO> listUserOrders(Long userId, PageDTO page, String status) {
        var wrapper = new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, userId)
                .eq(status != null, OrderEntity::getStatus, status)
                .orderByDesc(OrderEntity::getCreateTime);
        var result = orderService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        var records = new ArrayList<OrderVO>();
        for (OrderEntity order : result.getRecords()) {
            records.add(toOrderVO(order));
        }
        return PageVO.of(page, result.getTotal(), records);
    }

    @Override
    public OrderVO getUserOrderDetail(Long userId, Long orderId) {
        return toOrderVO(getUserOrder(userId, orderId));
    }

    @Override
    public PageVO<OrderEntity> listAdminOrders(PageDTO page, String status, String orderNo) {
        var wrapper = new LambdaQueryWrapper<OrderEntity>()
                .eq(status != null, OrderEntity::getStatus, status)
                .eq(orderNo != null, OrderEntity::getOrderNo, orderNo)
                .orderByDesc(OrderEntity::getCreateTime);
        var result = orderService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        return PageVO.of(page, result);
    }

    @Override
    public Map<String, Object> getAdminOrderDetail(Long orderId) {
        var order = getOrder(orderId);
        var items = orderItemService.lambdaQuery().eq(OrderItem::getOrderId, orderId).list();
        return Map.of("order", order, "items", items);
    }

    @Override
    public Map<String, Long> getAdminOrderStatistics() {
        long total = orderService.count();
        long pendingPay = orderService.lambdaQuery().eq(OrderEntity::getStatus, OrderStatus.PENDING_PAY).count();
        long pendingShip = orderService.lambdaQuery().eq(OrderEntity::getStatus, OrderStatus.PENDING_SHIP).count();
        long shipped = orderService.lambdaQuery().eq(OrderEntity::getStatus, OrderStatus.SHIPPED).count();
        long completed = orderService.lambdaQuery().eq(OrderEntity::getStatus, OrderStatus.COMPLETED).count();
        long refundRequested = orderService.lambdaQuery().eq(OrderEntity::getStatus, OrderStatus.REFUND_REQUESTED).count();
        long refunded = orderService.lambdaQuery().eq(OrderEntity::getStatus, OrderStatus.REFUNDED).count();
        return Map.of(
                "total", total,
                "pendingPay", pendingPay,
                "pendingShip", pendingShip,
                "shipped", shipped,
                "completed", completed,
                "refundRequested", refundRequested,
                "refunded", refunded
        );
    }

    @Override
    public PageVO<OrderVO> listMerchantOrders(Long merchantId, PageDTO page, String status) {
        var items = orderItemService.lambdaQuery()
                .eq(OrderItem::getMerchantId, merchantId)
                .orderByDesc(OrderItem::getCreateTime)
                .list();
        var orderIds = new ArrayList<Long>(new LinkedHashSet<>(items.stream().map(OrderItem::getOrderId).toList()));
        if (status != null) {
            orderIds.removeIf(orderId -> {
                var order = orderService.getById(orderId);
                return order == null || !status.equals(order.getStatus());
            });
        }
        var records = new ArrayList<OrderVO>();
        for (Long orderId : pageIds(orderIds, page)) {
            var order = orderService.getById(orderId);
            if (order != null) {
                records.add(toMerchantOrderVO(order, merchantId));
            }
        }
        return PageVO.of((long) orderIds.size(), page.getPage(), page.getSize(), records);
    }

    @Override
    public OrderVO getMerchantOrderDetail(Long merchantId, Long orderId) {
        return toMerchantOrderVO(getMerchantOrder(merchantId, orderId), merchantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipMerchantOrder(Long merchantId, Long orderId, String logisticsCompany, String logisticsNo) {
        var order = getMerchantOrder(merchantId, orderId);
        orderStatusService.checkCanShip(order);
        // V1 simplification: the current order model has no item-level shipment state,
        // so merchant shipment updates the main order once this merchant ships its items.
        order.setStatus(OrderStatus.SHIPPED);
        order.setShipTime(LocalDateTime.now());
        order.setAdminRemark("商家发货: " + logisticsCompany + " " + logisticsNo);
        orderService.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateResultVO createOrder(Long userId, OrderCreateDTO dto) {
        var reservedStocks = new ArrayList<OrderCreateDTO.OrderItemDTO>();
        try {
            String orderNo = "MF" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
            var address = getValidAddress(userId, dto.getAddressId());
            var order = buildPendingOrder(userId, orderNo, address, dto.getRemark());
            var orderItems = buildOrderItems(orderNo, dto.getItems(), reservedStocks);

            fillOrderAmount(order, orderItems.totalAmount(), orderItems.freightAmount());
            orderService.save(order);
            saveOrderItems(order.getId(), orderItems.items());
            removePurchasedCartItems(userId, dto.getItems());
            sendOrderCreatedNotificationAfterCommit(userId, orderNo);
            return buildCreateResult(order, orderNo);
        } catch (RuntimeException e) {
            for (var reservedStock : reservedStocks) {
                stockService.rollbackProductStock(reservedStock.getProductId(), reservedStock.getQuantity());
            }
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, String reason) {
        cancelOrder(getOrder(orderId), reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelUserOrder(Long userId, Long orderId, String reason) {
        cancelOrder(getUserOrder(userId, orderId), reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void requestRefund(Long userId, Long orderId, String reason) {
        var order = getUserOrder(userId, orderId);
        orderStatusService.checkCanChangeTo(order, OrderStatus.REFUND_REQUESTED);
        order.setStatus(OrderStatus.REFUND_REQUESTED);
        order.setAdminRemark(buildRefundRequestRemark(reason));
        orderService.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId) {
        var order = getOrder(orderId);
        orderStatusService.checkCanPay(order);
        order.setStatus(OrderStatus.PENDING_SHIP);
        order.setPayTime(LocalDateTime.now());
        orderService.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId, String logisticsCompany, String logisticsNo) {
        var order = getOrder(orderId);
        orderStatusService.checkCanShip(order);
        order.setStatus(OrderStatus.SHIPPED);
        order.setShipTime(LocalDateTime.now());
        order.setAdminRemark("物流: " + logisticsCompany + " " + logisticsNo);
        orderService.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(Long orderId) {
        completeOrder(getOrder(orderId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeUserOrder(Long userId, Long orderId) {
        completeOrder(getUserOrder(userId, orderId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long orderId, String targetStatus) {
        var order = getOrder(orderId);
        orderStatusService.checkCanChangeTo(order, targetStatus);
        if (targetStatus.equals(order.getStatus())) {
            return;
        }
        order.setStatus(targetStatus);
        if (OrderStatus.PENDING_SHIP.equals(targetStatus)) {
            order.setPayTime(LocalDateTime.now());
            order.setPaymentMethod(MANUAL_PAYMENT_METHOD);
        } else if (OrderStatus.CANCELLED.equals(targetStatus)) {
            order.setCancelTime(LocalDateTime.now());
            order.setCancelReason("管理员取消订单");
            restoreOrderStock(order);
        }
        orderService.updateById(order);
    }

    @Override
    public LocalDateTime getPaymentTimeoutDeadline(LocalDateTime now) {
        return OrderPaymentRules.timeoutDeadline(now, getPaymentTimeoutMinutes());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeTimeoutOrders(LocalDateTime deadline) {
        var orders = orderService.lambdaQuery()
                .eq(OrderEntity::getStatus, OrderStatus.PENDING_PAY)
                .le(OrderEntity::getCreateTime, deadline)
                .list();
        for (var order : orders) {
            orderStatusService.checkCanCancel(order);
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelTime(LocalDateTime.now());
            order.setCancelReason("超时未支付，系统自动取消");
            restoreOrderStock(order);
            orderService.updateById(order);
            log.info("订单 {} 超时自动取消", order.getOrderNo());
        }
    }

    private void completeOrder(OrderEntity order) {
        orderStatusService.checkCanComplete(order);
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompleteTime(LocalDateTime.now());
        orderService.updateById(order);
    }

    private void cancelOrder(OrderEntity order, String reason) {
        orderStatusService.checkCanCancel(order);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason);
        restoreOrderStock(order);
        orderService.updateById(order);
    }

    private String buildRefundRequestRemark(String reason) {
        if (reason == null || reason.isBlank()) {
            return "用户申请退款";
        }
        return "用户申请退款：" + reason.trim();
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

    private OrderEntity getOrder(Long orderId) {
        var order = orderService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
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

    private UserAddress getValidAddress(Long userId, Long addressId) {
        var address = addressService.getById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "收货地址无效");
        }
        return address;
    }

    private OrderEntity buildPendingOrder(Long userId, String orderNo, UserAddress address, String userRemark) {
        var order = new OrderEntity();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setAddressSnapshot(buildAddressSnapshot(address));
        order.setStatus(OrderStatus.PENDING_PAY);
        order.setUserRemark(userRemark);
        return order;
    }

    private String buildAddressSnapshot(UserAddress address) {
        return "{\"receiverName\":\"" + address.getReceiverName()
                + "\",\"receiverPhone\":\"" + address.getReceiverPhone()
                + "\",\"province\":\"" + address.getProvince()
                + "\",\"city\":\"" + address.getCity()
                + "\",\"district\":\"" + address.getDistrict()
                + "\",\"detail\":\"" + address.getDetail() + "\"}";
    }

    private OrderItemsResult buildOrderItems(String orderNo, List<OrderCreateDTO.OrderItemDTO> itemDtos,
                                             List<OrderCreateDTO.OrderItemDTO> reservedStocks) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal freight = BigDecimal.ZERO;
        var items = new ArrayList<OrderItem>();

        stockLock.lock();
        try {
            for (var itemDto : itemDtos) {
                var product = getAvailableProduct(itemDto);
                deductStock(product, itemDto, reservedStocks);

                var item = buildOrderItem(orderNo, product, itemDto.getQuantity());
                items.add(item);
                total = total.add(item.getTotalPrice());
                freight = freight.add(product.getFreight() == null ? BigDecimal.ZERO : product.getFreight());

                product.setStock(product.getStock() - itemDto.getQuantity());
                product.setSalesCount(product.getSalesCount() + itemDto.getQuantity());
                productService.updateById(product);
            }
        } finally {
            stockLock.unlock();
        }

        return new OrderItemsResult(items, total, freight);
    }

    private Product getAvailableProduct(OrderCreateDTO.OrderItemDTO itemDto) {
        var product = productService.getById(itemDto.getProductId());
        if (product == null || product.getStatus() == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "商品 " + itemDto.getProductId() + " 不存在或已下架");
        }
        if (product.getStock() < itemDto.getQuantity()) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "商品 " + product.getName() + " 库存不足");
        }
        return product;
    }

    private void deductStock(Product product, OrderCreateDTO.OrderItemDTO itemDto,
                             List<OrderCreateDTO.OrderItemDTO> reservedStocks) {
        if (!stockService.deductProductStock(product.getId(), product.getStock(), itemDto.getQuantity())) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "商品 " + product.getName() + " 库存不足");
        }
        reservedStocks.add(itemDto);
    }

    private OrderItem buildOrderItem(String orderNo, Product product, Integer quantity) {
        var item = new OrderItem();
        item.setOrderNo(orderNo);
        item.setMerchantId(product.getMerchantId());
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setProductImage(product.getCoverImage());
        item.setPrice(product.getPrice());
        item.setQuantity(quantity);
        item.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        return item;
    }

    private void fillOrderAmount(OrderEntity order, BigDecimal total, BigDecimal freight) {
        order.setTotalAmount(total);
        order.setFreightAmount(freight);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(total.add(freight));
    }

    private void saveOrderItems(Long orderId, List<OrderItem> items) {
        for (var item : items) {
            item.setOrderId(orderId);
            orderItemService.save(item);
        }
    }

    private void removePurchasedCartItems(Long userId, List<OrderCreateDTO.OrderItemDTO> itemDtos) {
        for (var itemDto : itemDtos) {
            cartService.lambdaUpdate()
                    .eq(ShoppingCartItem::getUserId, userId)
                    .eq(ShoppingCartItem::getProductId, itemDto.getProductId())
                    .remove();
        }
    }

    private OrderCreateResultVO buildCreateResult(OrderEntity order, String orderNo) {
        var result = new OrderCreateResultVO();
        result.setOrderId(order.getId());
        result.setOrderNo(orderNo);
        result.setPayAmount(order.getPayAmount());
        result.setPaymentExpireTime(getPaymentExpireTime(order));
        return result;
    }

    private OrderVO toOrderVO(OrderEntity order) {
        var vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setCreateTime(order.getCreateTime());
        vo.setPaymentExpireTime(getPaymentExpireTime(order));
        vo.setTotalAmount(order.getTotalAmount());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setStatus(order.getStatus());
        vo.setPaymentMethod(order.getPaymentMethod());
        vo.setPayTime(order.getPayTime());
        vo.setShipTime(order.getShipTime());
        vo.setCompleteTime(order.getCompleteTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCancelReason(order.getCancelReason());
        vo.setAdminRemark(order.getAdminRemark());
        fillLogisticsInfo(vo, order.getAdminRemark());
        vo.setAddressSnapshot(order.getAddressSnapshot());

        var items = orderItemService.lambdaQuery()
                .eq(OrderItem::getOrderId, order.getId())
                .list();
        var itemVOs = new ArrayList<OrderVO.OrderItemVO>();
        for (OrderItem item : items) {
            var itemVO = new OrderVO.OrderItemVO();
            itemVO.setMerchantId(item.getMerchantId());
            itemVO.setProductId(item.getProductId());
            itemVO.setProductName(item.getProductName());
            itemVO.setProductImage(item.getProductImage());
            itemVO.setPrice(item.getPrice());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setTotalPrice(item.getTotalPrice());
            itemVOs.add(itemVO);
        }
        vo.setItems(itemVOs);
        return vo;
    }

    private OrderVO toMerchantOrderVO(OrderEntity order, Long merchantId) {
        var vo = toOrderBaseVO(order);
        var items = orderItemService.lambdaQuery()
                .eq(OrderItem::getOrderId, order.getId())
                .eq(OrderItem::getMerchantId, merchantId)
                .list();
        var itemVOs = new ArrayList<OrderVO.OrderItemVO>();
        for (OrderItem item : items) {
            var itemVO = new OrderVO.OrderItemVO();
            itemVO.setMerchantId(item.getMerchantId());
            itemVO.setProductId(item.getProductId());
            itemVO.setProductName(item.getProductName());
            itemVO.setProductImage(item.getProductImage());
            itemVO.setPrice(item.getPrice());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setTotalPrice(item.getTotalPrice());
            itemVOs.add(itemVO);
        }
        vo.setItems(itemVOs);
        return vo;
    }

    private OrderVO toOrderBaseVO(OrderEntity order) {
        var vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setCreateTime(order.getCreateTime());
        vo.setPaymentExpireTime(getPaymentExpireTime(order));
        vo.setTotalAmount(order.getTotalAmount());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setStatus(order.getStatus());
        vo.setPaymentMethod(order.getPaymentMethod());
        vo.setPayTime(order.getPayTime());
        vo.setShipTime(order.getShipTime());
        vo.setCompleteTime(order.getCompleteTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCancelReason(order.getCancelReason());
        vo.setAdminRemark(order.getAdminRemark());
        fillLogisticsInfo(vo, order.getAdminRemark());
        vo.setAddressSnapshot(order.getAddressSnapshot());
        return vo;
    }

    private List<Long> pageIds(List<Long> orderIds, PageDTO page) {
        int from = Math.max(0, (page.getPage() - 1) * page.getSize());
        if (from >= orderIds.size()) {
            return List.of();
        }
        int to = Math.min(orderIds.size(), from + page.getSize());
        return orderIds.subList(from, to);
    }

    private OrderEntity getMerchantOrder(Long merchantId, Long orderId) {
        long itemCount = orderItemService.lambdaQuery()
                .eq(OrderItem::getOrderId, orderId)
                .eq(OrderItem::getMerchantId, merchantId)
                .count();
        if (itemCount == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在或无权查看");
        }
        return getOrder(orderId);
    }

    private LocalDateTime getPaymentExpireTime(OrderEntity order) {
        return OrderPaymentRules.expireTime(order, getPaymentTimeoutMinutes());
    }

    private void fillLogisticsInfo(OrderVO vo, String adminRemark) {
        if (adminRemark == null || !adminRemark.startsWith("物流: ")) {
            return;
        }
        var logisticsText = adminRemark.substring("物流: ".length()).trim();
        if (logisticsText.isEmpty()) {
            return;
        }
        var splitIndex = logisticsText.lastIndexOf(' ');
        if (splitIndex <= 0 || splitIndex >= logisticsText.length() - 1) {
            vo.setLogisticsCompany(logisticsText);
            return;
        }
        vo.setLogisticsCompany(logisticsText.substring(0, splitIndex).trim());
        vo.setLogisticsNo(logisticsText.substring(splitIndex + 1).trim());
    }

    private long getPaymentTimeoutMinutes() {
        return OrderPaymentRules.normalizeTimeoutMinutes(
                configService.getInt(PAYMENT_TIMEOUT_MINUTES_KEY, (int) OrderPaymentRules.defaultTimeoutMinutes())
        );
    }

    private void sendOrderCreatedNotificationAfterCommit(Long userId, String orderNo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notificationService.sendOrderCreatedNotification(userId, orderNo);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationService.sendOrderCreatedNotification(userId, orderNo);
            }
        });
    }

    private record OrderItemsResult(List<OrderItem> items, BigDecimal totalAmount, BigDecimal freightAmount) {
    }
}
