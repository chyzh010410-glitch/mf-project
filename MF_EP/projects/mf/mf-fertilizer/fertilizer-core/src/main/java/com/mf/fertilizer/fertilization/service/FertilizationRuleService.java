package com.mf.fertilizer.fertilization.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mf.fertilizer.fertilization.entity.FertilizationRule;
import com.mf.fertilizer.fertilization.vo.RecommendResultVO;

import java.util.List;

public interface FertilizationRuleService extends IService<FertilizationRule> {

    List<RecommendResultVO> recommend(String species, Integer age, String season);
}
