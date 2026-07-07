package com.mf.fertilizer.product.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.exception.BusinessException;
import com.mf.fertilizer.product.dto.admin.ProductSaveDTO;
import com.mf.fertilizer.product.entity.Product;
import com.mf.fertilizer.product.entity.ProductCategory;
import com.mf.fertilizer.product.entity.ProductDetail;
import com.mf.fertilizer.product.service.ProductCategoryService;
import com.mf.fertilizer.product.service.ProductDetailService;
import com.mf.fertilizer.product.service.ProductMerchantApplicationService;
import com.mf.fertilizer.product.service.ProductService;
import com.mf.fertilizer.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductMerchantApplicationServiceImpl implements ProductMerchantApplicationService {

    private final ProductService productService;
    private final ProductDetailService detailService;
    private final ProductCategoryService categoryService;

    @Override
    public PageVO<Product> listProducts(Long merchantId, PageDTO page, String name, String productType, Long categoryId, Integer status) {
        var wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getMerchantId, merchantId)
                .like(StrUtil.isNotBlank(name), Product::getName, name)
                .eq(StrUtil.isNotBlank(productType), Product::getProductType, productType)
                .eq(categoryId != null, Product::getCategoryId, categoryId)
                .eq(status != null, Product::getStatus, status)
                .orderByDesc(Product::getSortOrder)
                .orderByDesc(Product::getCreateTime);
        var result = productService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        return PageVO.of(page, result);
    }

    @Override
    public Map<String, Object> getProductDetail(Long merchantId, Long id) {
        Product product = requireMerchantProduct(merchantId, id);
        ProductDetail detail = detailService.lambdaQuery().eq(ProductDetail::getProductId, id).one();
        String categoryName = "";
        if (product.getCategoryId() != null) {
            ProductCategory category = categoryService.getById(product.getCategoryId());
            if (category != null) {
                categoryName = category.getName();
            }
        }
        return Map.of(
                "product", product,
                "detail", detail != null ? detail : new ProductDetail(),
                "categoryName", categoryName
        );
    }

    @Override
    public void createProduct(Long merchantId, ProductSaveDTO dto) {
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setMerchantId(merchantId);
        applyProductDefaults(product);
        productService.save(product);
        saveProductDetailIfPresent(product.getId(), dto);
    }

    @Override
    public void updateProduct(Long merchantId, Long id, ProductSaveDTO dto) {
        Product product = requireMerchantProduct(merchantId, id);
        BeanUtils.copyProperties(dto, product);
        product.setId(id);
        product.setMerchantId(merchantId);
        productService.updateById(product);
        saveProductDetailIfPresent(id, dto);
    }

    @Override
    public void deleteProduct(Long merchantId, Long id) {
        requireMerchantProduct(merchantId, id);
        productService.removeById(id);
        detailService.lambdaUpdate().eq(ProductDetail::getProductId, id).remove();
    }

    private Product requireMerchantProduct(Long merchantId, Long id) {
        Product product = productService.lambdaQuery()
                .eq(Product::getId, id)
                .eq(Product::getMerchantId, merchantId)
                .one();
        if (product == null) {
            throw new BusinessException(404, "商品不存在或无权操作");
        }
        return product;
    }

    private void applyProductDefaults(Product product) {
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        if (product.getStock() == null) {
            product.setStock(0);
        }
        if (product.getSalesCount() == null) {
            product.setSalesCount(0);
        }
        if (product.getIsRecommend() == null) {
            product.setIsRecommend(0);
        }
        if (product.getIsNew() == null) {
            product.setIsNew(0);
        }
    }

    private void saveProductDetailIfPresent(Long productId, ProductSaveDTO dto) {
        if (StrUtil.isBlank(dto.getAttrsJson()) && StrUtil.isBlank(dto.getDetailType())) {
            return;
        }
        ProductDetail detail = detailService.lambdaQuery().eq(ProductDetail::getProductId, productId).one();
        if (detail == null) {
            detail = new ProductDetail();
            detail.setProductId(productId);
        }
        detail.setDetailType(dto.getDetailType());
        detail.setAttrsJson(dto.getAttrsJson());
        detailService.saveOrUpdate(detail);
    }
}
