package com.mf.datacenter.source.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dc_source_table_contract")
public class SourceTableContractEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sourceName;
    private String schemaName;
    private String tableName;
    private String businessName;
    private String description;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
