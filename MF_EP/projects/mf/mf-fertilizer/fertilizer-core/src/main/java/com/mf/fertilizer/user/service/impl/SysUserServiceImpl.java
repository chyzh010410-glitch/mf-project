package com.mf.fertilizer.user.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.constant.RedisKey;
import com.mf.fertilizer.constant.ResultCode;
import com.mf.fertilizer.exception.BusinessException;
import com.mf.fertilizer.infra.service.CacheService;
import com.mf.fertilizer.user.entity.SysUser;
import com.mf.fertilizer.user.mapper.SysUserMapper;
import com.mf.fertilizer.user.service.SysUserService;
import com.mf.fertilizer.user.vo.LoginResultVO;
import com.mf.fertilizer.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final CacheService cacheService;

    @Override
    public LoginResultVO login(String username, String password) {
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名或密码不能为空");
        }
        SysUser user = getByUsername(username);
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("账户已被禁用");
        }
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = JwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
        cacheService.set(RedisKey.loginToken(token), "1", Duration.ofDays(RedisKey.TOKEN_EXPIRE_DAYS));

        LoginResultVO result = new LoginResultVO();
        result.setToken(token);
        result.setUsername(user.getUsername());
        result.setRealName(user.getRealName());
        result.setRole(user.getRole());
        return result;
    }

    @Override
    public void logout(String token) {
        cacheService.delete(RedisKey.loginToken(token));
    }

    @Override
    public SysUser getByUsername(String username) {
        return lambdaQuery().eq(SysUser::getUsername, username).one();
    }
}
