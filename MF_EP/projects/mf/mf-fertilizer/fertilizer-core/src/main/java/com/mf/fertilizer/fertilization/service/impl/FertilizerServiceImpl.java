package com.mf.fertilizer.fertilization.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.constant.CacheNames;
import com.mf.fertilizer.fertilization.entity.Fertilizer;
import com.mf.fertilizer.fertilization.mapper.FertilizerMapper;
import com.mf.fertilizer.fertilization.service.FertilizerService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FertilizerServiceImpl extends ServiceImpl<FertilizerMapper, Fertilizer> implements FertilizerService {

    @Override
    @Cacheable(value = CacheNames.FERTILIZER_LIST, key = "'all'")
    public List<Fertilizer> getCachedList() {
        return list();
    }

    @Override
    @CacheEvict(value = CacheNames.FERTILIZER_LIST, allEntries = true)
    public void evictCache() {
    }
}
