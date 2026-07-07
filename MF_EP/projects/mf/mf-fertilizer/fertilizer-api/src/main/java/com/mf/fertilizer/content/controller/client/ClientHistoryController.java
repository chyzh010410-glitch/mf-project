package com.mf.fertilizer.content.controller.client;

import com.mf.fertilizer.context.UserContext;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.content.entity.BrowsingHistory;
import com.mf.fertilizer.content.service.ContentHistoryApplicationService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client/history")
@RequiredArgsConstructor
public class ClientHistoryController {
    private final ContentHistoryApplicationService contentHistoryApplicationService;

    @GetMapping
    public ResultVO<PageVO<BrowsingHistory>> list(@ModelAttribute PageDTO page) {
        return ResultVO.success(contentHistoryApplicationService.listHistory(UserContext.getUserId(), page));
    }

    @DeleteMapping
    public ResultVO<?> clear() {
        contentHistoryApplicationService.clearHistory(UserContext.getUserId());
        return ResultVO.success();
    }
}
