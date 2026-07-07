package com.mf.fertilizer.user.service;

import com.mf.fertilizer.user.entity.User;
import com.mf.fertilizer.user.entity.UserAddress;

import java.util.List;

public interface UserApplicationService {

    User getProfile(Long userId);

    void updateProfile(Long userId, User form);

    void changePassword(Long userId, String oldPassword, String newPassword);

    List<UserAddress> listAddresses(Long userId);

    void addAddress(Long userId, UserAddress address);

    void updateAddress(Long userId, Long addressId, UserAddress address);

    void deleteAddress(Long userId, Long addressId);

    void setDefaultAddress(Long userId, Long addressId);
}
