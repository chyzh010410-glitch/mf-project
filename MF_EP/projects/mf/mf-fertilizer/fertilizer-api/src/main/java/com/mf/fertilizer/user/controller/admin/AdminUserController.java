package com.mf.fertilizer.user.controller.admin;

import com.mf.fertilizer.annotation.OperationLog;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.user.entity.User;
import com.mf.fertilizer.user.service.AdminUserApplicationService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserApplicationService adminUserApplicationService;

    @GetMapping
    public ResultVO<PageVO<User>> list(@ModelAttribute PageDTO page,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Integer status) {
        return ResultVO.success(adminUserApplicationService.listUsers(page, keyword, status));
    }

    @PutMapping("/{id}/status")
    @OperationLog(module = "用户管理", action = "禁用/启用")
    public ResultVO<?> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        adminUserApplicationService.updateUserStatus(id, body.get("status"));
        return ResultVO.success();
    }
}
