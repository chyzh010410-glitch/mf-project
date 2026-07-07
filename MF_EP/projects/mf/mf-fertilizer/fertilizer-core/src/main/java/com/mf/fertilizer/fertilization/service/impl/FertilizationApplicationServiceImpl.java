package com.mf.fertilizer.fertilization.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.fertilization.dto.FertilizationRecordDTO;
import com.mf.fertilizer.fertilization.dto.FertilizationRuleDTO;
import com.mf.fertilizer.fertilization.dto.FertilizerQueryDTO;
import com.mf.fertilizer.fertilization.dto.RecordQueryDTO;
import com.mf.fertilizer.fertilization.dto.TreeQueryDTO;
import com.mf.fertilizer.fertilization.entity.FertilizationRecord;
import com.mf.fertilizer.fertilization.entity.FertilizationRule;
import com.mf.fertilizer.fertilization.entity.Fertilizer;
import com.mf.fertilizer.fertilization.entity.Tree;
import com.mf.fertilizer.fertilization.service.FertilizationApplicationService;
import com.mf.fertilizer.fertilization.service.FertilizationRecordService;
import com.mf.fertilizer.fertilization.service.FertilizationRuleService;
import com.mf.fertilizer.fertilization.service.FertilizerService;
import com.mf.fertilizer.fertilization.service.TreeService;
import com.mf.fertilizer.fertilization.vo.RecommendResultVO;
import com.mf.fertilizer.fertilization.vo.StatsVO;
import com.mf.fertilizer.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FertilizationApplicationServiceImpl implements FertilizationApplicationService {

    private final FertilizerService fertilizerService;
    private final TreeService treeService;
    private final FertilizationRuleService ruleService;
    private final FertilizationRecordService recordService;

    @Override
    public PageVO<Fertilizer> listFertilizers(FertilizerQueryDTO dto) {
        var result = fertilizerService.page(
                new Page<>(dto.getPage(), dto.getSize()),
                new LambdaQueryWrapper<Fertilizer>()
                        .like(StrUtil.isNotBlank(dto.getName()), Fertilizer::getName, dto.getName())
                        .eq(StrUtil.isNotBlank(dto.getType()), Fertilizer::getType, dto.getType())
                        .orderByDesc(Fertilizer::getCreateTime)
        );
        return PageVO.of(dto, result);
    }

    @Override
    public List<Fertilizer> getFertilizerList() {
        return fertilizerService.getCachedList();
    }

    @Override
    public Fertilizer getFertilizer(Long id) {
        return fertilizerService.getById(id);
    }

    @Override
    public void createFertilizer(Fertilizer fertilizer) {
        fertilizerService.save(fertilizer);
        fertilizerService.evictCache();
    }

    @Override
    public void updateFertilizer(Fertilizer fertilizer) {
        fertilizerService.updateById(fertilizer);
        fertilizerService.evictCache();
    }

    @Override
    public void deleteFertilizer(Long id) {
        fertilizerService.removeById(id);
        fertilizerService.evictCache();
    }

    @Override
    public PageVO<Tree> listTrees(TreeQueryDTO dto) {
        var result = treeService.page(
                new Page<>(dto.getPage(), dto.getSize()),
                new LambdaQueryWrapper<Tree>()
                        .like(StrUtil.isNotBlank(dto.getSpecies()), Tree::getSpecies, dto.getSpecies())
                        .eq(StrUtil.isNotBlank(dto.getStatus()), Tree::getStatus, dto.getStatus())
                        .ge(dto.getAgeMin() != null, Tree::getAge, dto.getAgeMin())
                        .le(dto.getAgeMax() != null, Tree::getAge, dto.getAgeMax())
                        .orderByDesc(Tree::getCreateTime)
        );
        return PageVO.of(dto, result);
    }

    @Override
    public Tree getTree(Long id) {
        return treeService.getById(id);
    }

    @Override
    public void createTree(Tree tree) {
        treeService.save(tree);
        treeService.evictCache();
    }

    @Override
    public void updateTree(Tree tree) {
        treeService.updateById(tree);
        treeService.evictCache();
    }

    @Override
    public void deleteTree(Long id) {
        treeService.removeById(id);
        treeService.evictCache();
    }

    @Override
    public List<String> getTreeSpecies() {
        return treeService.getCachedSpecies();
    }

    @Override
    public PageVO<FertilizationRule> listRules(PageDTO dto) {
        var result = ruleService.page(
                new Page<>(dto.getPage(), dto.getSize()),
                new LambdaQueryWrapper<FertilizationRule>()
                        .orderByDesc(FertilizationRule::getPriority)
        );
        return PageVO.of(dto, result);
    }

    @Override
    public FertilizationRule getRule(Long id) {
        return ruleService.getById(id);
    }

    @Override
    public void createRule(FertilizationRuleDTO dto) {
        FertilizationRule rule = new FertilizationRule();
        BeanUtils.copyProperties(dto, rule);
        ruleService.save(rule);
    }

    @Override
    public void updateRule(FertilizationRule rule) {
        ruleService.updateById(rule);
    }

    @Override
    public void deleteRule(Long id) {
        ruleService.removeById(id);
    }

    @Override
    public List<RecommendResultVO> recommendRules(String species, Integer age, String season) {
        return ruleService.recommend(species, age, season);
    }

    @Override
    public PageVO<FertilizationRecord> listRecords(RecordQueryDTO dto) {
        var result = recordService.page(
                new Page<>(dto.getPage(), dto.getSize()),
                new LambdaQueryWrapper<FertilizationRecord>()
                        .eq(dto.getTreeId() != null, FertilizationRecord::getTreeId, dto.getTreeId())
                        .eq(dto.getFertilizerId() != null, FertilizationRecord::getFertilizerId, dto.getFertilizerId())
                        .ge(dto.getStartDate() != null, FertilizationRecord::getFertilizeDate, dto.getStartDate())
                        .le(dto.getEndDate() != null, FertilizationRecord::getFertilizeDate, dto.getEndDate())
                        .orderByDesc(FertilizationRecord::getFertilizeDate)
        );
        return PageVO.of(dto, result);
    }

    @Override
    public FertilizationRecord getRecord(Long id) {
        return recordService.getById(id);
    }

    @Override
    public void createRecord(FertilizationRecordDTO dto) {
        FertilizationRecord record = new FertilizationRecord();
        BeanUtils.copyProperties(dto, record);
        recordService.save(record);
    }

    @Override
    public void deleteRecord(Long id) {
        recordService.removeById(id);
    }

    @Override
    public StatsVO getRecordStats() {
        return recordService.getStats();
    }
}
