package com.mf.fertilizer.merchant.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mf.fertilizer.constant.RedisKey;
import com.mf.fertilizer.constant.RoleEnum;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.exception.BusinessException;
import com.mf.fertilizer.infra.service.CacheService;
import com.mf.fertilizer.merchant.dto.MerchantLoginDTO;
import com.mf.fertilizer.merchant.dto.MerchantProfileDTO;
import com.mf.fertilizer.merchant.dto.MerchantRegisterDTO;
import com.mf.fertilizer.merchant.entity.Merchant;
import com.mf.fertilizer.merchant.entity.MerchantStatus;
import com.mf.fertilizer.merchant.service.MerchantApplicationService;
import com.mf.fertilizer.merchant.service.MerchantService;
import com.mf.fertilizer.merchant.vo.MerchantLoginResultVO;
import com.mf.fertilizer.util.JwtUtil;
import com.mf.fertilizer.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MerchantApplicationServiceImpl implements MerchantApplicationService {

    private final MerchantService merchantService;
    private final CacheService cacheService;

    @Override
    public void register(MerchantRegisterDTO dto) {
        ensureUniqueUsername(dto.getUsername(), null);
        ensureUniquePhone(dto.getPhone(), null);

        var merchant = new Merchant();
        merchant.setUsername(dto.getUsername());
        merchant.setPassword(md5(dto.getPassword()));
        merchant.setShopName(dto.getShopName());
        merchant.setContactName(dto.getContactName());
        merchant.setPhone(dto.getPhone());
        merchant.setStatus(MerchantStatus.PENDING);
        merchantService.save(merchant);
    }

    @Override
    public MerchantLoginResultVO login(MerchantLoginDTO dto) {
        var merchant = merchantService.lambdaQuery()
                .eq(Merchant::getUsername, dto.getUsername())
                .one();
        if (merchant == null || !md5(dto.getPassword()).equals(merchant.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!MerchantStatus.APPROVED.equals(merchant.getStatus())) {
            throw new BusinessException(403, merchantLoginDeniedMessage(merchant.getStatus()));
        }

        merchant.setLastLoginTime(LocalDateTime.now());
        merchantService.updateById(merchant);

        String token = JwtUtil.generateWithUserType(
                merchant.getId(),
                merchant.getUsername(),
                RoleEnum.MERCHANT,
                RoleEnum.MERCHANT
        );
        cacheService.set(RedisKey.merchantToken(token), String.valueOf(merchant.getId()), Duration.ofDays(RedisKey.TOKEN_EXPIRE_DAYS));
        return new MerchantLoginResultVO(token, merchant.getId(), merchant.getUsername(), merchant.getShopName(), merchant.getStatus());
    }

    @Override
    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            cacheService.delete(RedisKey.merchantToken(authHeader.substring(7)));
        }
    }

    @Override
    public Merchant getProfile(Long merchantId) {
        return requireMerchant(merchantId);
    }

    @Override
    public void updateProfile(Long merchantId, MerchantProfileDTO dto) {
        ensureUniquePhone(dto.getPhone(), merchantId);
        var merchant = requireMerchant(merchantId);
        merchant.setShopName(dto.getShopName());
        merchant.setContactName(dto.getContactName());
        merchant.setPhone(dto.getPhone());
        merchantService.updateById(merchant);
    }

    @Override
    public PageVO<Merchant> listAdminMerchants(PageDTO page, String status, String keyword) {
        var wrapper = new LambdaQueryWrapper<Merchant>()
                .eq(StrUtil.isNotBlank(status), Merchant::getStatus, status)
                .and(StrUtil.isNotBlank(keyword), w -> w
                        .like(Merchant::getUsername, keyword)
                        .or()
                        .like(Merchant::getShopName, keyword)
                        .or()
                        .like(Merchant::getPhone, keyword))
                .orderByDesc(Merchant::getCreateTime);
        var result = merchantService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        return PageVO.of(page, result);
    }

    @Override
    public Merchant getAdminMerchant(Long id) {
        return requireMerchant(id);
    }

    @Override
    public void approve(Long id) {
        updateAuditStatus(id, MerchantStatus.APPROVED, null);
    }

    @Override
    public void reject(Long id, String auditRemark) {
        updateAuditStatus(id, MerchantStatus.REJECTED, auditRemark);
    }

    @Override
    public void disable(Long id) {
        updateAuditStatus(id, MerchantStatus.DISABLED, null);
    }

    private void updateAuditStatus(Long id, String status, String auditRemark) {
        var merchant = requireMerchant(id);
        merchant.setStatus(status);
        merchant.setAuditRemark(auditRemark);
        merchant.setAuditTime(LocalDateTime.now());
        merchantService.updateById(merchant);
    }

    private Merchant requireMerchant(Long id) {
        var merchant = merchantService.getById(id);
        if (merchant == null) {
            throw new BusinessException(404, "商家不存在");
        }
        return merchant;
    }

    private void ensureUniqueUsername(String username, Long excludeId) {
        var wrapper = new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUsername, username)
                .ne(excludeId != null, Merchant::getId, excludeId);
        if (merchantService.count(wrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }
    }

    private void ensureUniquePhone(String phone, Long excludeId) {
        var wrapper = new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getPhone, phone)
                .ne(excludeId != null, Merchant::getId, excludeId);
        if (merchantService.count(wrapper) > 0) {
            throw new BusinessException("手机号已注册");
        }
    }

    private String merchantLoginDeniedMessage(String status) {
        if (MerchantStatus.PENDING.equals(status)) {
            return "商家入驻申请待审核";
        }
        if (MerchantStatus.REJECTED.equals(status)) {
            return "商家入驻申请已被拒绝";
        }
        if (MerchantStatus.DISABLED.equals(status)) {
            return "商家账号已被禁用";
        }
        return "商家状态不允许登录";
    }

    private String md5(String value) {
        return DigestUtils.md5DigestAsHex(value.getBytes(StandardCharsets.UTF_8));
    }
}
