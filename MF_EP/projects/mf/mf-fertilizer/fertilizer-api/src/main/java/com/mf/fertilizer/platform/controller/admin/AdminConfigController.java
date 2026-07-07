package com.mf.fertilizer.platform.controller.admin;

import com.mf.fertilizer.annotation.OperationLog;
import com.mf.fertilizer.annotation.RequireRole;
import com.mf.fertilizer.constant.RoleEnum;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.platform.dto.admin.PlatformConfigSaveDTO;
import com.mf.fertilizer.platform.entity.PlatformConfig;
import com.mf.fertilizer.platform.service.PlatformAdminApplicationService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/configs")
@RequireRole(RoleEnum.SUPER_ADMIN)
@RequiredArgsConstructor
public class AdminConfigController {

    private final PlatformAdminApplicationService platformAdminApplicationService;

    @GetMapping
    public ResultVO<PageVO<PlatformConfig>> list(@ModelAttribute PageDTO page,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String configGroup) {
        return ResultVO.success(platformAdminApplicationService.listConfigs(page, keyword, configGroup));
    }

    @GetMapping("/{id}")
    public ResultVO<PlatformConfig> detail(@PathVariable Long id) {
        return ResultVO.success(platformAdminApplicationService.getConfig(id));
    }

    @PostMapping
    @OperationLog(module = "平台配置", action = "新增")
    public ResultVO<?> save(@RequestBody PlatformConfigSaveDTO dto) {
        platformAdminApplicationService.createConfig(dto);
        return ResultVO.success();
    }

    @PutMapping("/{id}")
    @OperationLog(module = "平台配置", action = "编辑")
    public ResultVO<?> update(@PathVariable Long id, @RequestBody PlatformConfigSaveDTO dto) {
        platformAdminApplicationService.updateConfig(id, dto);
        return ResultVO.success();
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "平台配置", action = "删除")
    public ResultVO<?> delete(@PathVariable Long id) {
        platformAdminApplicationService.deleteConfig(id);
        return ResultVO.success();
    }
}
