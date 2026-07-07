package com.mf.fertilizer.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant")
public class Merchant extends BaseEntity {

    private String username;
    private String password;
    private String shopName;
    private String contactName;
    private String phone;
    private String status;
    private String auditRemark;
    private LocalDateTime auditTime;
    private LocalDateTime lastLoginTime;
}
