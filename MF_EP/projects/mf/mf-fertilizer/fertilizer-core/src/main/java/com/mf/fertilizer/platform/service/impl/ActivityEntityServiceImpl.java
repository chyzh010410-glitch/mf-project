package com.mf.fertilizer.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.platform.entity.ActivityEntity;
import com.mf.fertilizer.platform.mapper.ActivityEntityMapper;
import com.mf.fertilizer.platform.service.ActivityEntityService;
import org.springframework.stereotype.Service;

@Service
public class ActivityEntityServiceImpl extends ServiceImpl<ActivityEntityMapper, ActivityEntity> implements ActivityEntityService {
}
