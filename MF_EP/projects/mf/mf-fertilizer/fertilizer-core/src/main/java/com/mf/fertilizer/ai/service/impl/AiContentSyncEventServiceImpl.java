package com.mf.fertilizer.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.ai.entity.AiContentSyncEvent;
import com.mf.fertilizer.ai.mapper.AiContentSyncEventMapper;
import com.mf.fertilizer.ai.service.AiContentSyncEventService;
import org.springframework.stereotype.Service;

@Service
public class AiContentSyncEventServiceImpl extends ServiceImpl<AiContentSyncEventMapper, AiContentSyncEvent> implements AiContentSyncEventService {
}
