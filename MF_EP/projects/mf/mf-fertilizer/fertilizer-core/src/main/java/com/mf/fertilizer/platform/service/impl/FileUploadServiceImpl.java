package com.mf.fertilizer.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.platform.entity.FileUpload;
import com.mf.fertilizer.platform.mapper.FileUploadMapper;
import com.mf.fertilizer.platform.service.FileUploadService;
import org.springframework.stereotype.Service;

@Service
public class FileUploadServiceImpl extends ServiceImpl<FileUploadMapper, FileUpload> implements FileUploadService {
}
