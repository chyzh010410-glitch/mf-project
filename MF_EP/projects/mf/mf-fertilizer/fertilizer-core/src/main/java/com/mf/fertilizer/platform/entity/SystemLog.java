package com.mf.fertilizer.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("system_log")
public class SystemLog {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long operatorId;
    private String operatorName;
    private String module;
    private String action;
    private String target;
    private String requestParams;
    private String ip;
    private String userAgent;
    private Long costTime;
    private String result;
    private String errorMsg;
    private LocalDateTime createTime;
}
