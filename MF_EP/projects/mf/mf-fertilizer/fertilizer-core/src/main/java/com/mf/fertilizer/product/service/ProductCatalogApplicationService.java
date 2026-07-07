package com.mf.fertilizer.product.service;

import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.product.entity.ProductCategory;
import com.mf.fertilizer.product.vo.client.ProductVO;
import com.mf.fertilizer.vo.PageVO;

import java.math.BigDecimal;
import java.util.List;

public interface ProductCatalogApplicationService {

    PageVO<ProductVO> listProducts(PageDTO page, Long categoryId, String keyword, String sort, String productType,
                                   BigDecimal minPrice, BigDecimal maxPrice);

    ProductVO getProductDetail(Long id);

    List<ProductCategory> listCategories(String type);
}
