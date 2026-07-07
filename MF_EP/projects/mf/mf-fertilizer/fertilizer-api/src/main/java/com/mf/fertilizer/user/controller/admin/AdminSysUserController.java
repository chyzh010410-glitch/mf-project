package com.mf.fertilizer.user.controller.admin;

import com.mf.fertilizer.annotation.OperationLog;
import com.mf.fertilizer.annotation.RequireRole;
import com.mf.fertilizer.constant.RoleEnum;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.user.entity.SysUser;
import com.mf.fertilizer.user.service.AdminUserApplicationService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/admins")
@RequireRole(RoleEnum.SUPER_ADMIN)
@RequiredArgsConstructor
public class AdminSysUserController {

    private final AdminUserApplicationService adminUserApplicationService;

    @GetMapping
    public ResultVO<PageVO<SysUser>> list(@ModelAttribute PageDTO page,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Integer status) {
        return ResultVO.success(adminUserApplicationService.listAdmins(page, keyword, status));
    }

    @GetMapping("/{id}")
    public ResultVO<SysUser> detail(@PathVariable Long id) {
        return ResultVO.success(adminUserApplicationService.getAdmin(id));
    }

    @PostMapping
    @OperationLog(module = "管理员管理", action = "新增")
    public ResultVO<?> save(@RequestBody Map<String, Object> body) {
        adminUserApplicationService.createAdmin(body);
        return ResultVO.success();
    }

    @PutMapping("/{id}")
    @OperationLog(module = "管理员管理", action = "编辑")
    public ResultVO<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminUserApplicationService.updateAdmin(id, body);
        return ResultVO.success();
    }

    @PutMapping("/{id}/status")
    @OperationLog(module = "管理员管理", action = "禁用/启用")
    public ResultVO<?> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        adminUserApplicationService.updateAdminStatus(id, body.get("status"));
        return ResultVO.success();
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "管理员管理", action = "删除")
    public ResultVO<?> delete(@PathVariable Long id) {
        adminUserApplicationService.disableAdmin(id);
        return ResultVO.success();
    }
}
