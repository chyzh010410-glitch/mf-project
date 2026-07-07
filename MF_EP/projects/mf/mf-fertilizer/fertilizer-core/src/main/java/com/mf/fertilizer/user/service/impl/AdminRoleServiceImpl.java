package com.mf.fertilizer.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.user.entity.AdminRole;
import com.mf.fertilizer.user.mapper.AdminRoleMapper;
import com.mf.fertilizer.user.service.AdminRoleService;
import org.springframework.stereotype.Service;

@Service
public class AdminRoleServiceImpl extends ServiceImpl<AdminRoleMapper, AdminRole> implements AdminRoleService {
}
