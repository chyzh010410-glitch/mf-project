package com.mf.fertilizer.fertilization.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.constant.CacheNames;
import com.mf.fertilizer.fertilization.entity.FertilizationRule;
import com.mf.fertilizer.fertilization.entity.Fertilizer;
import com.mf.fertilizer.fertilization.mapper.FertilizationRuleMapper;
import com.mf.fertilizer.fertilization.service.FertilizationRuleService;
import com.mf.fertilizer.fertilization.service.FertilizerService;
import com.mf.fertilizer.util.SeasonUtil;
import com.mf.fertilizer.fertilization.vo.RecommendResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FertilizationRuleServiceImpl extends ServiceImpl<FertilizationRuleMapper, FertilizationRule>
        implements FertilizationRuleService {

    private final FertilizerService fertilizerService;

    @Override
    public List<RecommendResultVO> recommend(String species, Integer age, String season) {
        var resolvedSeason = StrUtil.isNotBlank(season) ? season : SeasonUtil.currentSeason();
        return doRecommend(species, age, resolvedSeason);
    }

    @Cacheable(value = CacheNames.RECOMMEND, key = "#species + ':' + #age + ':' + #season")
    public List<RecommendResultVO> doRecommend(String species, Integer age, String season) {
        var rules = lambdaQuery()
                .eq(FertilizationRule::getSpecies, species)
                .le(FertilizationRule::getAgeMin, age)
                .ge(FertilizationRule::getAgeMax, age)
                .and(w -> w.eq(FertilizationRule::getSeason, season)
                        .or().eq(FertilizationRule::getSeason, "all"))
                .orderByDesc(FertilizationRule::getPriority)
                .list();

        var fertMap = fertilizerService.getCachedList().stream()
                .collect(java.util.stream.Collectors.toMap(Fertilizer::getId, f -> f));

        var result = new ArrayList<RecommendResultVO>();
        for (var rule : rules) {
            var fert = fertMap.get(rule.getFertilizerId());
            if (fert == null) continue;
            var vo = new RecommendResultVO();
            vo.setFertilizerId(fert.getId());
            vo.setFertilizerName(fert.getName());
            vo.setFertilizerType(fert.getType());
            vo.setNutrientContent(fert.getNutrientContent());
            vo.setRecommendAmount(rule.getRecommendAmount());
            vo.setUnitPrice(fert.getUnitPrice());
            vo.setMethod(rule.getMethod());
            vo.setRuleRemark(rule.getRemark());
            vo.setPriority(rule.getPriority());
            result.add(vo);
        }
        result.sort(Comparator.comparingInt(RecommendResultVO::getPriority).reversed());
        return result;
    }
}
