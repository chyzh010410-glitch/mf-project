package com.mf.fertilizer.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.product.entity.Product;
import com.mf.fertilizer.product.mapper.ProductMapper;
import com.mf.fertilizer.product.service.ProductService;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
}
