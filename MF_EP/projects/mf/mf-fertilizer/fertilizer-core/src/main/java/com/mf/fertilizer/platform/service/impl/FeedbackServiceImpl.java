package com.mf.fertilizer.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.platform.entity.Feedback;
import com.mf.fertilizer.platform.mapper.FeedbackMapper;
import com.mf.fertilizer.platform.service.FeedbackService;
import org.springframework.stereotype.Service;

@Service
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback> implements FeedbackService {
}
