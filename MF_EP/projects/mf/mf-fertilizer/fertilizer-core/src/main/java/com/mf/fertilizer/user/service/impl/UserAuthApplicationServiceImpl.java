package com.mf.fertilizer.user.service.impl;

import com.mf.fertilizer.constant.RedisKey;
import com.mf.fertilizer.constant.RoleEnum;
import com.mf.fertilizer.constant.VerificationCodeType;
import com.mf.fertilizer.exception.BusinessException;
import com.mf.fertilizer.infra.service.CacheService;
import com.mf.fertilizer.user.dto.client.PasswordResetDTO;
import com.mf.fertilizer.user.dto.client.UserLoginDTO;
import com.mf.fertilizer.user.dto.client.UserRegisterDTO;
import com.mf.fertilizer.user.entity.User;
import com.mf.fertilizer.user.entity.VerificationCode;
import com.mf.fertilizer.user.service.UserAuthApplicationService;
import com.mf.fertilizer.user.service.UserService;
import com.mf.fertilizer.user.service.VerificationCodeService;
import com.mf.fertilizer.user.vo.client.UserLoginResultVO;
import com.mf.fertilizer.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class UserAuthApplicationServiceImpl implements UserAuthApplicationService {

    private final UserService userService;
    private final VerificationCodeService verificationCodeService;
    private final CacheService cacheService;

    @Override
    public void register(UserRegisterDTO dto) {
        VerificationCode code = requireLatestValidCode(dto.getPhone(), VerificationCodeType.REGISTER);
        if (!code.getCode().equals(dto.getCode())) {
            throw new BusinessException("验证码错误或已过期");
        }
        code.setUsed(1);
        verificationCodeService.updateById(code);

        if (userService.lambdaQuery().eq(User::getUsername, dto.getUsername()).count() > 0) {
            throw new BusinessException("用户名已存在");
        }
        if (userService.lambdaQuery().eq(User::getPhone, dto.getPhone()).count() > 0) {
            throw new BusinessException("手机号已注册");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setPassword(md5(dto.getPassword()));
        user.setNickname("用户" + System.currentTimeMillis() % 100000);
        user.setStatus(1);
        user.setPoints(0);
        userService.save(user);
    }

    @Override
    public UserLoginResultVO login(UserLoginDTO dto) {
        User user = userService.lambdaQuery()
                .eq(StringUtils.hasText(dto.getUsername()), User::getUsername, dto.getUsername())
                .eq(StringUtils.hasText(dto.getPhone()), User::getPhone, dto.getPhone())
                .one();
        if (user == null) {
            throw new BusinessException(401, "用户名或手机号不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(401, "账号已被禁用");
        }
        if (!md5(dto.getPassword()).equals(user.getPassword())) {
            throw new BusinessException(401, "密码错误");
        }

        String token = JwtUtil.generateWithUserType(user.getId(), user.getUsername(), RoleEnum.CONSUMER, RoleEnum.CONSUMER);
        cacheService.set(RedisKey.clientToken(token), String.valueOf(user.getId()), Duration.ofDays(RedisKey.TOKEN_EXPIRE_DAYS));
        return new UserLoginResultVO(token, user.getId(), user.getUsername(), user.getNickname(), user.getAvatar(), user.getPoints());
    }

    @Override
    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            cacheService.delete(RedisKey.clientToken(authHeader.substring(7)));
        }
    }

    @Override
    public String createCaptcha(String target, String type) {
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setTarget(target);
        verificationCode.setCode(code);
        verificationCode.setType(type);
        verificationCode.setUsed(0);
        verificationCode.setExpireTime(LocalDateTime.now().plusMinutes(5));
        verificationCode.setCreateTime(LocalDateTime.now());
        verificationCodeService.save(verificationCode);
        cacheService.set(RedisKey.verificationCode(target, type), code, Duration.ofMinutes(5));
        return code;
    }

    @Override
    public void resetPassword(PasswordResetDTO dto) {
        String cachedCode = cacheService.get(RedisKey.verificationCode(dto.getPhone(), VerificationCodeType.RESET_PASSWORD));
        if (cachedCode == null || !cachedCode.equals(dto.getCode())) {
            throw new BusinessException("验证码错误或已过期");
        }
        User user = userService.lambdaQuery().eq(User::getPhone, dto.getPhone()).one();
        if (user == null) {
            throw new BusinessException("手机号未注册");
        }
        user.setPassword(md5(dto.getNewPassword()));
        userService.updateById(user);
    }

    private VerificationCode requireLatestValidCode(String target, String type) {
        return verificationCodeService.lambdaQuery()
                .eq(VerificationCode::getTarget, target)
                .eq(VerificationCode::getType, type)
                .eq(VerificationCode::getUsed, 0)
                .ge(VerificationCode::getExpireTime, LocalDateTime.now())
                .orderByDesc(VerificationCode::getCreateTime)
                .last("limit 1")
                .oneOpt()
                .orElseThrow(() -> new BusinessException("验证码错误或已过期"));
    }

    private String md5(String value) {
        return DigestUtils.md5DigestAsHex(value.getBytes(StandardCharsets.UTF_8));
    }
}
