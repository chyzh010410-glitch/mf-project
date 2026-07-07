package com.mf.fertilizer.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MerchantProfileDTO {

    @NotBlank(message = "店铺名称不能为空")
    private String shopName;

    @NotBlank(message = "联系人不能为空")
    private String contactName;

    @NotBlank(message = "手机号不能为空")
    private String phone;
}
