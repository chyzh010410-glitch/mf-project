package com.mf.fertilizer.product.service;

import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.product.dto.admin.ProductSaveDTO;
import com.mf.fertilizer.product.entity.Product;
import com.mf.fertilizer.product.entity.ProductCategory;
import com.mf.fertilizer.vo.PageVO;

import java.util.Map;

public interface ProductAdminApplicationService {

    PageVO<Product> listProducts(PageDTO page, String name, String productType, Long categoryId, Integer status);

    Map<String, Object> getProductDetail(Long id);

    void createProduct(ProductSaveDTO dto);

    void updateProduct(Long id, ProductSaveDTO dto);

    void deleteProduct(Long id);

    void updateProductStatus(Long id, Integer status);

    void toggleProductRecommend(Long id);

    void toggleProductNew(Long id);

    PageVO<ProductCategory> listCategories(PageDTO page, String name, String type);

    ProductCategory getCategory(Long id);

    void createCategory(ProductCategory dto);

    void updateCategory(Long id, ProductCategory dto);

    void deleteCategory(Long id);
}
