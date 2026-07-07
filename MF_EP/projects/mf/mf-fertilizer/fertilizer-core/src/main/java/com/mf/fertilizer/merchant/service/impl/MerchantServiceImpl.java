package com.mf.fertilizer.merchant.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.merchant.entity.Merchant;
import com.mf.fertilizer.merchant.mapper.MerchantMapper;
import com.mf.fertilizer.merchant.service.MerchantService;
import org.springframework.stereotype.Service;

@Service
public class MerchantServiceImpl extends ServiceImpl<MerchantMapper, Merchant> implements MerchantService {
}
