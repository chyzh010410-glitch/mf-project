package com.mf.fertilizer.platform.controller.admin;

import com.mf.fertilizer.annotation.OperationLog;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.platform.entity.Feedback;
import com.mf.fertilizer.platform.service.PlatformAdminApplicationService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/feedbacks")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final PlatformAdminApplicationService platformAdminApplicationService;

    @GetMapping
    public ResultVO<PageVO<Feedback>> list(@ModelAttribute PageDTO page,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String type) {
        return ResultVO.success(platformAdminApplicationService.listFeedbacks(page, status, type));
    }

    @PutMapping("/{id}/reply")
    @OperationLog(module = "反馈管理", action = "回复")
    public ResultVO<?> reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        platformAdminApplicationService.replyFeedback(id, body);
        return ResultVO.success();
    }
}
