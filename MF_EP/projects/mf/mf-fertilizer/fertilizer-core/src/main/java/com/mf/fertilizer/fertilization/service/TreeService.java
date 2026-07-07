package com.mf.fertilizer.fertilization.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mf.fertilizer.fertilization.entity.Tree;

import java.util.List;

public interface TreeService extends IService<Tree> {

    /** Get cached tree species list. */
    List<String> getCachedSpecies();

    void evictCache();
}
