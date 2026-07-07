package com.mf.fertilizer.product.service;

import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.product.dto.admin.ProductSaveDTO;
import com.mf.fertilizer.product.entity.Product;
import com.mf.fertilizer.vo.PageVO;

import java.util.Map;

public interface ProductMerchantApplicationService {

    PageVO<Product> listProducts(Long merchantId, PageDTO page, String name, String productType, Long categoryId, Integer status);

    Map<String, Object> getProductDetail(Long merchantId, Long id);

    void createProduct(Long merchantId, ProductSaveDTO dto);

    void updateProduct(Long merchantId, Long id, ProductSaveDTO dto);

    void deleteProduct(Long merchantId, Long id);
}
