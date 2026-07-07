package com.mf.fertilizer.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.exception.BusinessException;
import com.mf.fertilizer.product.entity.Product;
import com.mf.fertilizer.product.entity.ProductCategory;
import com.mf.fertilizer.product.entity.ProductDetail;
import com.mf.fertilizer.product.service.ProductCatalogApplicationService;
import com.mf.fertilizer.product.service.ProductCategoryService;
import com.mf.fertilizer.product.service.ProductDetailService;
import com.mf.fertilizer.product.service.ProductService;
import com.mf.fertilizer.product.vo.client.ProductVO;
import com.mf.fertilizer.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductCatalogApplicationServiceImpl implements ProductCatalogApplicationService {

    private final ProductService productService;
    private final ProductDetailService productDetailService;
    private final ProductCategoryService categoryService;

    @Override
    public PageVO<ProductVO> listProducts(PageDTO page, Long categoryId, String keyword, String sort, String productType,
                                          BigDecimal minPrice, BigDecimal maxPrice) {
        var wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .gt(Product::getStock, 0)
                .eq(categoryId != null, Product::getCategoryId, categoryId)
                .eq(productType != null, Product::getProductType, productType)
                .like(keyword != null, Product::getName, keyword)
                .ge(minPrice != null, Product::getPrice, minPrice)
                .le(maxPrice != null, Product::getPrice, maxPrice);

        if ("sales".equals(sort)) {
            wrapper.orderByDesc(Product::getSalesCount);
        } else if ("price_asc".equals(sort)) {
            wrapper.orderByAsc(Product::getPrice);
        } else if ("price_desc".equals(sort)) {
            wrapper.orderByDesc(Product::getPrice);
        } else {
            wrapper.orderByDesc(Product::getCreateTime);
        }

        var result = productService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        var records = result.getRecords().stream().map(this::toVO).toList();
        return PageVO.of(page, result.getTotal(), records);
    }

    @Override
    public ProductVO getProductDetail(Long id) {
        Product product = productService.getById(id);
        if (product == null || product.getStatus() == 0) {
            throw new BusinessException(404, "商品不存在");
        }

        ProductDetail detail = productDetailService.lambdaQuery()
                .eq(ProductDetail::getProductId, id)
                .one();
        ProductVO vo = toVO(product);
        if (detail != null) {
            vo.setDetailType(detail.getDetailType());
            vo.setAttrsJson(detail.getAttrsJson());
        }
        return vo;
    }

    @Override
    public List<ProductCategory> listCategories(String type) {
        return categoryService.lambdaQuery()
                .eq(type != null, ProductCategory::getType, type)
                .eq(ProductCategory::getParentId, 0L)
                .orderByAsc(ProductCategory::getSortOrder)
                .list();
    }

    private ProductVO toVO(Product product) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setProductType(product.getProductType());
        vo.setBrand(product.getBrand());
        vo.setCoverImage(product.getCoverImage());
        vo.setImages(product.getImages());
        vo.setPrice(product.getPrice());
        vo.setOriginalPrice(product.getOriginalPrice());
        vo.setStock(product.getStock());
        vo.setUnit(product.getUnit());
        vo.setSalesCount(product.getSalesCount());
        vo.setStatus(product.getStatus());
        vo.setIsRecommend(product.getIsRecommend());
        vo.setIsNew(product.getIsNew());
        vo.setDescription(product.getDescription());
        vo.setFreight(product.getFreight());
        return vo;
    }
}
