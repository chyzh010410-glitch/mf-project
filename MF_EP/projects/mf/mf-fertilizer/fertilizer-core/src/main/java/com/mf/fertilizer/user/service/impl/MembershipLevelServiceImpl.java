package com.mf.fertilizer.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.user.entity.MembershipLevel;
import com.mf.fertilizer.user.mapper.MembershipLevelMapper;
import com.mf.fertilizer.user.service.MembershipLevelService;
import org.springframework.stereotype.Service;

@Service
public class MembershipLevelServiceImpl extends ServiceImpl<MembershipLevelMapper, MembershipLevel> implements MembershipLevelService {
}
