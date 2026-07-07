package com.mf.fertilizer.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mf.fertilizer.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_upload")
public class FileUpload extends BaseEntity {
    private String originalName;
    private String storedName;
    private String filePath;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private String fileExt;
    private Long uploaderId;
    private String uploaderType;
    private String purpose;
    private Integer width;
    private Integer height;
}
