package com.mf.fertilizer.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.product.entity.ProductDetail;
import com.mf.fertilizer.product.mapper.ProductDetailMapper;
import com.mf.fertilizer.product.service.ProductDetailService;
import org.springframework.stereotype.Service;

@Service
public class ProductDetailServiceImpl extends ServiceImpl<ProductDetailMapper, ProductDetail> implements ProductDetailService {
}
