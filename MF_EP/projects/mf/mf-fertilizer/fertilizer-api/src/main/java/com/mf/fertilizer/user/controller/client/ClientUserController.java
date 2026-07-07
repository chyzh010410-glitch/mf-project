package com.mf.fertilizer.user.controller.client;

import com.mf.fertilizer.context.UserContext;
import com.mf.fertilizer.user.entity.User;
import com.mf.fertilizer.user.entity.UserAddress;
import com.mf.fertilizer.user.service.UserApplicationService;
import com.mf.fertilizer.vo.ResultVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
public class ClientUserController {

    private final UserApplicationService userApplicationService;

    @GetMapping("/user/profile")
    public ResultVO<User> profile() {
        return ResultVO.success(userApplicationService.getProfile(UserContext.getUserId()));
    }

    @PutMapping("/user/profile")
    public ResultVO<?> updateProfile(@RequestBody User form) {
        userApplicationService.updateProfile(UserContext.getUserId(), form);
        return ResultVO.success();
    }

    @PutMapping("/user/password")
    public ResultVO<?> changePassword(@RequestBody PasswordForm form) {
        userApplicationService.changePassword(UserContext.getUserId(), form.getOldPassword(), form.getNewPassword());
        return ResultVO.success();
    }

    @GetMapping("/addresses")
    public ResultVO<List<UserAddress>> addresses() {
        return ResultVO.success(userApplicationService.listAddresses(UserContext.getUserId()));
    }

    @PostMapping("/addresses")
    public ResultVO<?> addAddress(@RequestBody UserAddress address) {
        userApplicationService.addAddress(UserContext.getUserId(), address);
        return ResultVO.success();
    }

    @PutMapping("/addresses/{id}")
    public ResultVO<?> updateAddress(@PathVariable Long id, @RequestBody UserAddress address) {
        userApplicationService.updateAddress(UserContext.getUserId(), id, address);
        return ResultVO.success();
    }

    @DeleteMapping("/addresses/{id}")
    public ResultVO<?> deleteAddress(@PathVariable Long id) {
        userApplicationService.deleteAddress(UserContext.getUserId(), id);
        return ResultVO.success();
    }

    @PutMapping("/addresses/{id}/default")
    public ResultVO<?> setDefault(@PathVariable Long id) {
        userApplicationService.setDefaultAddress(UserContext.getUserId(), id);
        return ResultVO.success();
    }

    @Data
    public static class PasswordForm {
        private String oldPassword;
        private String newPassword;
    }
}
