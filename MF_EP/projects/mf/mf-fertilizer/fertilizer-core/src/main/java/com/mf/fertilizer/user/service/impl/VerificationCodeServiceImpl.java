package com.mf.fertilizer.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.user.entity.VerificationCode;
import com.mf.fertilizer.user.mapper.VerificationCodeMapper;
import com.mf.fertilizer.user.service.VerificationCodeService;
import org.springframework.stereotype.Service;

@Service
public class VerificationCodeServiceImpl extends ServiceImpl<VerificationCodeMapper, VerificationCode> implements VerificationCodeService {
}
