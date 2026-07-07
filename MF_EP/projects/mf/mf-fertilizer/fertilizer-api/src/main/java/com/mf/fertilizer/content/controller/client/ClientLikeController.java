package com.mf.fertilizer.content.controller.client;

import com.mf.fertilizer.content.service.ContentInteractionApplicationService;
import com.mf.fertilizer.context.UserContext;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/client/likes")
@RequiredArgsConstructor
public class ClientLikeController {

    private final ContentInteractionApplicationService contentInteractionApplicationService;

    @GetMapping("/check")
    public ResultVO<?> check(@RequestParam String targetType, @RequestParam String targetId) {
        return ResultVO.success(contentInteractionApplicationService.checkLike(UserContext.getUserId(), targetType, targetId));
    }

    @PostMapping
    public ResultVO<?> toggle(@RequestBody Map<String, Object> body) {
        return ResultVO.success(contentInteractionApplicationService.toggleLike(UserContext.getUserId(), body));
    }
}
