package com.mf.fertilizer.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.platform.entity.UserUpload;
import com.mf.fertilizer.platform.mapper.UserUploadMapper;
import com.mf.fertilizer.platform.service.UserUploadService;
import org.springframework.stereotype.Service;

@Service
public class UserUploadServiceImpl extends ServiceImpl<UserUploadMapper, UserUpload> implements UserUploadService {
}
