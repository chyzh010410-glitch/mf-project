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
import com.mf.fertilizer.product.service.ProductAdminApplicationService;
import com.mf.fertilizer.product.service.ProductCategoryService;
import com.mf.fertilizer.product.service.ProductDetailService;
import com.mf.fertilizer.product.service.ProductService;
import com.mf.fertilizer.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductAdminApplicationServiceImpl implements ProductAdminApplicationService {

    private final ProductService productService;
    private final ProductDetailService detailService;
    private final ProductCategoryService categoryService;

    @Override
    public PageVO<Product> listProducts(PageDTO page, String name, String productType, Long categoryId, Integer status) {
        var wrapper = new LambdaQueryWrapper<Product>()
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
    public Map<String, Object> getProductDetail(Long id) {
        Product product = requireProduct(id);
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
    public void createProduct(ProductSaveDTO dto) {
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        applyProductDefaults(product);
        productService.save(product);
        saveProductDetailIfPresent(product.getId(), dto);
    }

    @Override
    public void updateProduct(Long id, ProductSaveDTO dto) {
        Product product = requireProduct(id);
        BeanUtils.copyProperties(dto, product);
        product.setId(id);
        productService.updateById(product);
        saveProductDetailIfPresent(id, dto);
    }

    @Override
    public void deleteProduct(Long id) {
        productService.removeById(id);
        detailService.lambdaUpdate().eq(ProductDetail::getProductId, id).remove();
    }

    @Override
    public void updateProductStatus(Long id, Integer status) {
        Product product = requireProduct(id);
        product.setStatus(status);
        productService.updateById(product);
    }

    @Override
    public void toggleProductRecommend(Long id) {
        Product product = requireProduct(id);
        product.setIsRecommend(Integer.valueOf(1).equals(product.getIsRecommend()) ? 0 : 1);
        productService.updateById(product);
    }

    @Override
    public void toggleProductNew(Long id) {
        Product product = requireProduct(id);
        product.setIsNew(Integer.valueOf(1).equals(product.getIsNew()) ? 0 : 1);
        productService.updateById(product);
    }

    @Override
    public PageVO<ProductCategory> listCategories(PageDTO page, String name, String type) {
        var wrapper = new LambdaQueryWrapper<ProductCategory>()
                .like(name != null, ProductCategory::getName, name)
                .eq(type != null, ProductCategory::getType, type)
                .orderByAsc(ProductCategory::getSortOrder)
                .orderByDesc(ProductCategory::getCreateTime);
        var result = categoryService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        return PageVO.of(page, result);
    }

    @Override
    public ProductCategory getCategory(Long id) {
        return requireCategory(id);
    }

    @Override
    public void createCategory(ProductCategory dto) {
        if (dto.getSortOrder() == null) {
            dto.setSortOrder(0);
        }
        categoryService.save(dto);
    }

    @Override
    public void updateCategory(Long id, ProductCategory dto) {
        ProductCategory category = requireCategory(id);
        BeanUtils.copyProperties(dto, category);
        category.setId(id);
        categoryService.updateById(category);
    }

    @Override
    public void deleteCategory(Long id) {
        categoryService.removeById(id);
    }

    private Product requireProduct(Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        return product;
    }

    private ProductCategory requireCategory(Long id) {
        ProductCategory category = categoryService.getById(id);
        if (category == null) {
            throw new BusinessException(404, "分类不存在");
        }
        return category;
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
