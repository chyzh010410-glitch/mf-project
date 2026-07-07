package com.mf.fertilizer.content.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mf.fertilizer.content.entity.BrowsingHistory;
import com.mf.fertilizer.content.service.BrowsingHistoryService;
import com.mf.fertilizer.content.service.ContentHistoryApplicationService;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentHistoryApplicationServiceImpl implements ContentHistoryApplicationService {

    private final BrowsingHistoryService browsingHistoryService;

    @Override
    public PageVO<BrowsingHistory> listHistory(Long userId, PageDTO page) {
        var result = browsingHistoryService.lambdaQuery()
                .eq(BrowsingHistory::getUserId, userId)
                .orderByDesc(BrowsingHistory::getCreateTime)
                .page(new Page<>(page.getPage(), page.getSize()));
        return PageVO.of(page, result);
    }

    @Override
    public void clearHistory(Long userId) {
        browsingHistoryService.lambdaUpdate()
                .eq(BrowsingHistory::getUserId, userId)
                .remove();
    }
}
