package com.mf.fertilizer.user.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.exception.BusinessException;
import com.mf.fertilizer.user.entity.SysUser;
import com.mf.fertilizer.user.entity.User;
import com.mf.fertilizer.user.service.AdminUserApplicationService;
import com.mf.fertilizer.user.service.SysUserService;
import com.mf.fertilizer.user.service.UserService;
import com.mf.fertilizer.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminUserApplicationServiceImpl implements AdminUserApplicationService {

    private final UserService userService;
    private final SysUserService sysUserService;

    @Override
    public PageVO<User> listUsers(PageDTO page, String keyword, Integer status) {
        var wrapper = new LambdaQueryWrapper<User>()
                .and(StrUtil.isNotBlank(keyword), q -> q
                        .like(User::getUsername, keyword).or()
                        .like(User::getPhone, keyword).or()
                        .like(User::getNickname, keyword))
                .eq(status != null, User::getStatus, status)
                .orderByDesc(User::getCreateTime);
        var result = userService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        return PageVO.of(page, result);
    }

    @Override
    public void updateUserStatus(Long id, Integer status) {
        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setStatus(status);
        userService.updateById(user);
    }

    @Override
    public PageVO<SysUser> listAdmins(PageDTO page, String keyword, Integer status) {
        var wrapper = new LambdaQueryWrapper<SysUser>()
                .and(StrUtil.isNotBlank(keyword), q -> q
                        .like(SysUser::getUsername, keyword).or()
                        .like(SysUser::getRealName, keyword))
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getCreateTime);
        var result = sysUserService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        result.getRecords().forEach(this::hidePassword);
        return PageVO.of(page, result);
    }

    @Override
    public SysUser getAdmin(Long id) {
        SysUser admin = requireAdmin(id);
        hidePassword(admin);
        return admin;
    }

    @Override
    public void createAdmin(Map<String, Object> body) {
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        if (StrUtil.isBlank(username)) {
            throw new BusinessException(400, "用户名不能为空");
        }
        if (StrUtil.isBlank(password)) {
            throw new BusinessException(400, "密码不能为空");
        }
        if (sysUserService.getByUsername(username) != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        SysUser admin = new SysUser();
        admin.setUsername(username);
        admin.setPassword(BCrypt.hashpw(password));
        admin.setRealName((String) body.getOrDefault("realName", username));
        admin.setRole((String) body.getOrDefault("role", "operator"));
        admin.setStatus(1);
        sysUserService.save(admin);
    }

    @Override
    public void updateAdmin(Long id, Map<String, Object> body) {
        SysUser admin = requireAdmin(id);
        String password = (String) body.get("password");
        if (StrUtil.isNotBlank(password)) {
            admin.setPassword(BCrypt.hashpw(password));
        }
        if (body.containsKey("realName")) {
            admin.setRealName((String) body.get("realName"));
        }
        if (body.containsKey("role")) {
            admin.setRole((String) body.get("role"));
        }
        sysUserService.updateById(admin);
    }

    @Override
    public void updateAdminStatus(Long id, Integer status) {
        SysUser admin = requireAdmin(id);
        admin.setStatus(status);
        sysUserService.updateById(admin);
    }

    @Override
    public void disableAdmin(Long id) {
        SysUser admin = requireAdmin(id);
        admin.setStatus(0);
        sysUserService.updateById(admin);
    }

    private SysUser requireAdmin(Long id) {
        SysUser admin = sysUserService.getById(id);
        if (admin == null) {
            throw new BusinessException(404, "管理员不存在");
        }
        return admin;
    }

    private void hidePassword(SysUser admin) {
        admin.setPassword(null);
    }
}
