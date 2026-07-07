package com.mf.fertilizer.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.content.entity.EncyclopediaEntry;
import com.mf.fertilizer.content.mapper.EncyclopediaEntryMapper;
import com.mf.fertilizer.content.service.EncyclopediaEntryService;
import org.springframework.stereotype.Service;

@Service
public class EncyclopediaEntryServiceImpl extends ServiceImpl<EncyclopediaEntryMapper, EncyclopediaEntry> implements EncyclopediaEntryService {
}
