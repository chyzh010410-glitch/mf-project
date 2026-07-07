package com.mf.fertilizer.product.service;

public interface StockService {

    boolean deductProductStock(Long productId, Integer currentStock, Integer quantity);

    void rollbackProductStock(Long productId, Integer quantity);
}
