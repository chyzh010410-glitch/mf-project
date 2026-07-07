package com.mf.fertilizer.content.controller.admin;

import com.mf.fertilizer.annotation.OperationLog;
import com.mf.fertilizer.content.entity.CommunityComment;
import com.mf.fertilizer.content.service.ContentAdminApplicationService;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final ContentAdminApplicationService contentAdminApplicationService;

    @GetMapping
    public ResultVO<PageVO<CommunityComment>> list(@ModelAttribute PageDTO page,
                                                   @RequestParam(required = false) String targetType,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Integer isDeleted) {
        return ResultVO.success(contentAdminApplicationService.listComments(
                page, targetType, keyword, isDeleted));
    }

    @PutMapping("/{id}/hide")
    @OperationLog(module = "评论管理", action = "隐藏")
    public ResultVO<?> hide(@PathVariable Long id) {
        contentAdminApplicationService.hideComment(id);
        return ResultVO.success();
    }

    @PutMapping("/{id}/restore")
    @OperationLog(module = "评论管理", action = "恢复")
    public ResultVO<?> restore(@PathVariable Long id) {
        contentAdminApplicationService.restoreComment(id);
        return ResultVO.success();
    }
}
