package com.mf.fertilizer.platform.controller.admin;

import com.mf.fertilizer.annotation.OperationLog;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.platform.dto.admin.ActivitySaveDTO;
import com.mf.fertilizer.platform.entity.ActivityEntity;
import com.mf.fertilizer.platform.service.PlatformAdminApplicationService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/activities")
@RequiredArgsConstructor
public class AdminActivityController {

    private final PlatformAdminApplicationService platformAdminApplicationService;

    @GetMapping
    public ResultVO<PageVO<ActivityEntity>> list(@ModelAttribute PageDTO page,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String type) {
        return ResultVO.success(platformAdminApplicationService.listActivities(page, keyword, status, type));
    }

    @GetMapping("/{id}")
    public ResultVO<ActivityEntity> detail(@PathVariable Long id) {
        return ResultVO.success(platformAdminApplicationService.getActivity(id));
    }

    @PostMapping
    @OperationLog(module = "活动管理", action = "新增")
    public ResultVO<?> save(@RequestBody ActivitySaveDTO dto) {
        platformAdminApplicationService.createActivity(dto);
        return ResultVO.success();
    }

    @PutMapping("/{id}")
    @OperationLog(module = "活动管理", action = "编辑")
    public ResultVO<?> update(@PathVariable Long id, @RequestBody ActivitySaveDTO dto) {
        platformAdminApplicationService.updateActivity(id, dto);
        return ResultVO.success();
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "活动管理", action = "删除")
    public ResultVO<?> delete(@PathVariable Long id) {
        platformAdminApplicationService.deleteActivity(id);
        return ResultVO.success();
    }

    @PutMapping("/{id}/status")
    @OperationLog(module = "活动管理", action = "状态切换")
    public ResultVO<?> toggleStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        platformAdminApplicationService.updateActivityStatus(id, body.get("status"));
        return ResultVO.success();
    }
}
