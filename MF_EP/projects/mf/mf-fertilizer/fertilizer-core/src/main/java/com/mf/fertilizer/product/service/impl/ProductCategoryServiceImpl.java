package com.mf.fertilizer.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.product.entity.ProductCategory;
import com.mf.fertilizer.product.mapper.ProductCategoryMapper;
import com.mf.fertilizer.product.service.ProductCategoryService;
import org.springframework.stereotype.Service;

@Service
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory> implements ProductCategoryService {
}
