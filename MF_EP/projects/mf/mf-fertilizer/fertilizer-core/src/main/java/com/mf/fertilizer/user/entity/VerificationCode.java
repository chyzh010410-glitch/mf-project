package com.mf.fertilizer.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("verification_code")
public class VerificationCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String target;
    private String code;
    private String type;
    private Integer used;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
}
