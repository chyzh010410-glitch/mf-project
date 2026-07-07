package com.mf.fertilizer.fertilization.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.constant.CacheNames;
import com.mf.fertilizer.fertilization.entity.Tree;
import com.mf.fertilizer.fertilization.mapper.TreeMapper;
import com.mf.fertilizer.fertilization.service.TreeService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TreeServiceImpl extends ServiceImpl<TreeMapper, Tree> implements TreeService {

    @Override
    @Cacheable(value = CacheNames.TREE_SPECIES, key = "'all'")
    public List<String> getCachedSpecies() {
        return lambdaQuery()
                .select(Tree::getSpecies)
                .groupBy(Tree::getSpecies)
                .list()
                .stream()
                .map(Tree::getSpecies)
                .distinct()
                .toList();
    }

    @Override
    @CacheEvict(value = CacheNames.TREE_SPECIES, allEntries = true)
    public void evictCache() {
    }
}
