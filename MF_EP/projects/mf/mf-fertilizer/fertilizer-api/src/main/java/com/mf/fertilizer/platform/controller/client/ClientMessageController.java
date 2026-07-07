package com.mf.fertilizer.platform.controller.client;

import com.mf.fertilizer.context.UserContext;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.platform.entity.Message;
import com.mf.fertilizer.platform.service.PlatformMessageApplicationService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client/messages")
@RequiredArgsConstructor
public class ClientMessageController {

    private final PlatformMessageApplicationService platformMessageApplicationService;

    @GetMapping
    public ResultVO<PageVO<Message>> list(@ModelAttribute PageDTO page,
                                          @RequestParam(name = "type", required = false) String type) {
        return ResultVO.success(platformMessageApplicationService.listMessages(UserContext.getUserId(), page, type));
    }

    @GetMapping("/unread-count")
    public ResultVO<Long> unread() {
        return ResultVO.success(platformMessageApplicationService.countUnread(UserContext.getUserId()));
    }

    @PutMapping("/{id}/read")
    public ResultVO<?> read(@PathVariable Long id) {
        platformMessageApplicationService.markRead(UserContext.getUserId(), id);
        return ResultVO.success();
    }
}
