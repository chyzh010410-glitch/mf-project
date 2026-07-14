package com.mf.fertilizer.ai.controller.client;

import com.mf.fertilizer.ai.client.DataCenterAiHistoryClient;
import com.mf.fertilizer.context.UserContext;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/client/ai")
@RequiredArgsConstructor
public class ClientAiController {

    private final DataCenterAiHistoryClient historyClient;

    @GetMapping("/conversations")
    public ResultVO<?> conversations(@RequestParam String sessionId,
                                     @RequestParam(name = "page", defaultValue = "1") long page,
                                     @RequestParam(name = "pageSize", defaultValue = "50") long pageSize) {
        var userId = UserContext.requireUserId();
        if (!belongsToUser(userId, sessionId)) {
            return ResultVO.fail(403, "会话不属于当前用户");
        }
        try {
            return ResultVO.success(historyClient.getConversations(userId, sessionId, page, Math.min(pageSize, 50)));
        } catch (RuntimeException ex) {
            return ResultVO.fail(503, "历史记录暂未同步");
        }
    }

    @DeleteMapping("/conversations/{sessionId}")
    public ResultVO<?> deleteConversations(@PathVariable String sessionId) {
        var userId = UserContext.requireUserId();
        if (!belongsToUser(userId, sessionId)) {
            return ResultVO.fail(403, "会话不属于当前用户");
        }
        try {
            return ResultVO.success(historyClient.deleteConversations(userId, sessionId));
        } catch (RuntimeException ex) {
            return ResultVO.fail(503, "历史记录暂未同步");
        }
    }

    private boolean belongsToUser(Long userId, String sessionId) {
        return sessionId != null && sessionId.startsWith("mf-ep-client-" + userId + "-");
    }
}
