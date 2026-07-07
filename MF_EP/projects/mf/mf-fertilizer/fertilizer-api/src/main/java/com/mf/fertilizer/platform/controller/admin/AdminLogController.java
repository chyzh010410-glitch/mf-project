package com.mf.fertilizer.platform.controller.admin;

import com.mf.fertilizer.annotation.RequireRole;
import com.mf.fertilizer.constant.RoleEnum;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.platform.entity.SystemLog;
import com.mf.fertilizer.platform.service.PlatformAdminApplicationService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/logs")
@RequireRole(RoleEnum.SUPER_ADMIN)
@RequiredArgsConstructor
public class AdminLogController {

    private final PlatformAdminApplicationService platformAdminApplicationService;

    @GetMapping
    public ResultVO<PageVO<SystemLog>> list(@ModelAttribute PageDTO page,
                                            @RequestParam(required = false) String module,
                                            @RequestParam(required = false) String keyword) {
        return ResultVO.success(platformAdminApplicationService.listLogs(page, module, keyword));
    }
}
