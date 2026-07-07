package com.mf.fertilizer.platform.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.platform.entity.Message;
import com.mf.fertilizer.platform.service.MessageService;
import com.mf.fertilizer.platform.service.PlatformMessageApplicationService;
import com.mf.fertilizer.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PlatformMessageApplicationServiceImpl implements PlatformMessageApplicationService {

    private final MessageService messageService;

    @Override
    public PageVO<Message> listMessages(Long userId, PageDTO page, String type) {
        var result = messageService.lambdaQuery()
                .eq(Message::getUserId, userId)
                .eq(type != null, Message::getType, type)
                .orderByDesc(Message::getCreateTime)
                .page(new Page<>(page.getPage(), page.getSize()));
        return PageVO.of(page, result);
    }

    @Override
    public Long countUnread(Long userId) {
        return messageService.lambdaQuery()
                .eq(Message::getUserId, userId)
                .eq(Message::getIsRead, 0)
                .count();
    }

    @Override
    public void markRead(Long userId, Long messageId) {
        Message message = messageService.lambdaQuery()
                .eq(Message::getId, messageId)
                .eq(Message::getUserId, userId)
                .one();
        if (message != null) {
            message.setIsRead(1);
            message.setReadTime(LocalDateTime.now());
            messageService.updateById(message);
        }
    }
}
