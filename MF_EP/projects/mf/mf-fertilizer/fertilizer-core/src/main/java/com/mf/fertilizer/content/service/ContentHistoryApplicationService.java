package com.mf.fertilizer.content.service;

import com.mf.fertilizer.content.entity.BrowsingHistory;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.vo.PageVO;

public interface ContentHistoryApplicationService {

    PageVO<BrowsingHistory> listHistory(Long userId, PageDTO page);

    void clearHistory(Long userId);
}
