package com.mf.fertilizer.content.controller.client;

import com.mf.fertilizer.annotation.RequireRole;
import com.mf.fertilizer.constant.RoleEnum;
import com.mf.fertilizer.content.entity.CommunityComment;
import com.mf.fertilizer.content.service.ContentInteractionApplicationService;
import com.mf.fertilizer.context.UserContext;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/client/comments")
@RequiredArgsConstructor
public class ClientCommentController {

    private final ContentInteractionApplicationService contentInteractionApplicationService;

    @GetMapping
    @RequireRole(RoleEnum.PUBLIC)
    public ResultVO<PageVO<CommunityComment>> list(@ModelAttribute PageDTO page,
                                                   @RequestParam String targetType,
                                                   @RequestParam String targetId) {
        return ResultVO.success(contentInteractionApplicationService.listComments(page, targetType, targetId));
    }

    @GetMapping("/{id}/replies")
    @RequireRole(RoleEnum.PUBLIC)
    public ResultVO<?> replies(@PathVariable Long id) {
        return ResultVO.success(contentInteractionApplicationService.listReplies(id));
    }

    @PostMapping
    public ResultVO<?> post(@RequestBody Map<String, Object> body) {
        return ResultVO.success(contentInteractionApplicationService.postComment(UserContext.getUserId(), body));
    }
}
