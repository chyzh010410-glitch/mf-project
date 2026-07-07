package com.mf.fertilizer.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.user.entity.UserAddress;
import com.mf.fertilizer.user.mapper.UserAddressMapper;
import com.mf.fertilizer.user.service.UserAddressService;
import org.springframework.stereotype.Service;

@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements UserAddressService {
}
