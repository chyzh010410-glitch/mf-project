package com.mf.fertilizer.platform.service;

import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.platform.entity.Message;
import com.mf.fertilizer.vo.PageVO;

public interface PlatformMessageApplicationService {

    PageVO<Message> listMessages(Long userId, PageDTO page, String type);

    Long countUnread(Long userId);

    void markRead(Long userId, Long messageId);
}
