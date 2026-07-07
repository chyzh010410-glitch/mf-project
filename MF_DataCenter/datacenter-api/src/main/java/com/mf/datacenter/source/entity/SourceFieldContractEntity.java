package com.mf.datacenter.source.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dc_source_field_contract")
public class SourceFieldContractEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tableContractId;
    private String fieldName;
    private String fieldType;
    private Boolean required;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
