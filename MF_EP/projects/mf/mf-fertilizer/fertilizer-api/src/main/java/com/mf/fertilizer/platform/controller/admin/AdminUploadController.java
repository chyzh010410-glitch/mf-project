package com.mf.fertilizer.platform.controller.admin;

import com.mf.fertilizer.annotation.OperationLog;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.platform.dto.admin.UploadReviewDTO;
import com.mf.fertilizer.platform.entity.UserUpload;
import com.mf.fertilizer.platform.service.PlatformAdminApplicationService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/uploads")
@RequiredArgsConstructor
public class AdminUploadController {

    private final PlatformAdminApplicationService platformAdminApplicationService;

    @GetMapping
    public ResultVO<PageVO<UserUpload>> list(@ModelAttribute PageDTO page,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String keyword) {
        return ResultVO.success(platformAdminApplicationService.listUploads(page, status, keyword));
    }

    @PutMapping("/{id}/review")
    @OperationLog(module = "上传审核", action = "审核")
    public ResultVO<?> review(@PathVariable Long id, @RequestBody UploadReviewDTO dto) {
        platformAdminApplicationService.reviewUpload(id, dto);
        return ResultVO.success();
    }
}
