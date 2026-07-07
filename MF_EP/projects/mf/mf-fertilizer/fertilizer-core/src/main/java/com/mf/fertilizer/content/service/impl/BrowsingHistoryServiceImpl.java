package com.mf.fertilizer.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.content.entity.BrowsingHistory;
import com.mf.fertilizer.content.mapper.BrowsingHistoryMapper;
import com.mf.fertilizer.content.service.BrowsingHistoryService;
import org.springframework.stereotype.Service;

@Service
public class BrowsingHistoryServiceImpl extends ServiceImpl<BrowsingHistoryMapper, BrowsingHistory> implements BrowsingHistoryService {
}
