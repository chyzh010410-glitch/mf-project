package com.mf.fertilizer.platform.controller.admin;

import com.mf.fertilizer.annotation.OperationLog;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.platform.dto.admin.MessageSendDTO;
import com.mf.fertilizer.platform.entity.Message;
import com.mf.fertilizer.platform.service.PlatformAdminApplicationService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/messages")
@RequiredArgsConstructor
public class AdminMessageController {

    private final PlatformAdminApplicationService platformAdminApplicationService;

    @GetMapping
    public ResultVO<PageVO<Message>> list(@ModelAttribute PageDTO page) {
        return ResultVO.success(platformAdminApplicationService.listMessages(page));
    }

    @PostMapping
    @OperationLog(module = "消息管理", action = "发送")
    public ResultVO<?> send(@RequestBody MessageSendDTO dto) {
        platformAdminApplicationService.sendMessage(dto);
        return ResultVO.success();
    }
}
