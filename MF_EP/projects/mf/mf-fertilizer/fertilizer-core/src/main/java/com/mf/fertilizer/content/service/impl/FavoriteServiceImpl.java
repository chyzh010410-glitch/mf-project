package com.mf.fertilizer.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.content.entity.Favorite;
import com.mf.fertilizer.content.mapper.FavoriteMapper;
import com.mf.fertilizer.content.service.FavoriteService;
import org.springframework.stereotype.Service;

@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {
}
