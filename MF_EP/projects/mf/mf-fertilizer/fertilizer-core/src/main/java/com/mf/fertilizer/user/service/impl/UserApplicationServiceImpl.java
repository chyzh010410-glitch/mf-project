package com.mf.fertilizer.user.service.impl;

import com.mf.fertilizer.exception.BusinessException;
import com.mf.fertilizer.user.entity.User;
import com.mf.fertilizer.user.entity.UserAddress;
import com.mf.fertilizer.user.service.UserAddressService;
import com.mf.fertilizer.user.service.UserApplicationService;
import com.mf.fertilizer.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserApplicationServiceImpl implements UserApplicationService {

    private final UserService userService;
    private final UserAddressService addressService;

    @Override
    public User getProfile(Long userId) {
        return requireUser(userId);
    }

    @Override
    public void updateProfile(Long userId, User form) {
        User user = requireUser(userId);
        if (form.getNickname() != null) {
            user.setNickname(form.getNickname());
        }
        if (form.getAvatar() != null) {
            user.setAvatar(form.getAvatar());
        }
        if (form.getEmail() != null) {
            user.setEmail(form.getEmail());
        }
        if (form.getGender() != null) {
            user.setGender(form.getGender());
        }
        userService.updateById(user);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new BusinessException(400, "密码不能为空");
        }
        User user = requireUser(userId);
        if (!md5(oldPassword).equals(user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }
        user.setPassword(md5(newPassword));
        userService.updateById(user);
    }

    @Override
    public List<UserAddress> listAddresses(Long userId) {
        return addressService.lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .list();
    }

    @Override
    public void addAddress(Long userId, UserAddress address) {
        address.setUserId(userId);
        if (isDefault(address)) {
            clearDefault(userId);
        }
        addressService.save(address);
    }

    @Override
    public void updateAddress(Long userId, Long addressId, UserAddress address) {
        UserAddress existing = requireAddress(userId, addressId);
        if (isDefault(address)) {
            clearDefault(userId);
        }
        address.setId(addressId);
        address.setUserId(existing.getUserId());
        addressService.updateById(address);
    }

    @Override
    public void deleteAddress(Long userId, Long addressId) {
        addressService.lambdaUpdate()
                .eq(UserAddress::getId, addressId)
                .eq(UserAddress::getUserId, userId)
                .remove();
    }

    @Override
    public void setDefaultAddress(Long userId, Long addressId) {
        UserAddress address = requireAddress(userId, addressId);
        clearDefault(userId);
        address.setIsDefault(1);
        addressService.updateById(address);
    }

    private User requireUser(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private UserAddress requireAddress(Long userId, Long addressId) {
        UserAddress address = addressService.lambdaQuery()
                .eq(UserAddress::getId, addressId)
                .eq(UserAddress::getUserId, userId)
                .one();
        if (address == null) {
            throw new BusinessException(404, "地址不存在");
        }
        return address;
    }

    private void clearDefault(Long userId) {
        addressService.lambdaUpdate()
                .eq(UserAddress::getUserId, userId)
                .set(UserAddress::getIsDefault, 0)
                .update();
    }

    private boolean isDefault(UserAddress address) {
        return address.getIsDefault() != null && address.getIsDefault() == 1;
    }

    private String md5(String value) {
        return DigestUtils.md5DigestAsHex(value.getBytes(StandardCharsets.UTF_8));
    }
}
