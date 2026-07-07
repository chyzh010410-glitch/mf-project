package com.mf.fertilizer.merchant.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantLoginResultVO implements Serializable {

    private String token;
    private Long merchantId;
    private String username;
    private String shopName;
    private String status;
}
