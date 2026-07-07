package com.mf.fertilizer.merchant.service;

import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.merchant.dto.MerchantLoginDTO;
import com.mf.fertilizer.merchant.dto.MerchantProfileDTO;
import com.mf.fertilizer.merchant.dto.MerchantRegisterDTO;
import com.mf.fertilizer.merchant.entity.Merchant;
import com.mf.fertilizer.merchant.vo.MerchantLoginResultVO;
import com.mf.fertilizer.vo.PageVO;

public interface MerchantApplicationService {

    void register(MerchantRegisterDTO dto);

    MerchantLoginResultVO login(MerchantLoginDTO dto);

    void logout(String authHeader);

    Merchant getProfile(Long merchantId);

    void updateProfile(Long merchantId, MerchantProfileDTO dto);

    PageVO<Merchant> listAdminMerchants(PageDTO page, String status, String keyword);

    Merchant getAdminMerchant(Long id);

    void approve(Long id);

    void reject(Long id, String auditRemark);

    void disable(Long id);
}
