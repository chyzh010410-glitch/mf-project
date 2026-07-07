package com.mf.fertilizer.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.user.entity.User;
import com.mf.fertilizer.user.mapper.UserMapper;
import com.mf.fertilizer.user.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
