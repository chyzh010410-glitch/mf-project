package com.mf.fertilizer.fertilization.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mf.fertilizer.fertilization.entity.Fertilizer;

import java.util.List;

public interface FertilizerService extends IService<Fertilizer> {

    /** Get cached fertilizer list. */
    List<Fertilizer> getCachedList();

    void evictCache();
}
