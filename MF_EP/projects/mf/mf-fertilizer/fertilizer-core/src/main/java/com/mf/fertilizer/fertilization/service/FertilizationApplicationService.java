package com.mf.fertilizer.fertilization.service;

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
import com.mf.fertilizer.fertilization.vo.RecommendResultVO;
import com.mf.fertilizer.fertilization.vo.StatsVO;
import com.mf.fertilizer.vo.PageVO;

import java.util.List;

public interface FertilizationApplicationService {

    PageVO<Fertilizer> listFertilizers(FertilizerQueryDTO dto);

    List<Fertilizer> getFertilizerList();

    Fertilizer getFertilizer(Long id);

    void createFertilizer(Fertilizer fertilizer);

    void updateFertilizer(Fertilizer fertilizer);

    void deleteFertilizer(Long id);

    PageVO<Tree> listTrees(TreeQueryDTO dto);

    Tree getTree(Long id);

    void createTree(Tree tree);

    void updateTree(Tree tree);

    void deleteTree(Long id);

    List<String> getTreeSpecies();

    PageVO<FertilizationRule> listRules(PageDTO dto);

    FertilizationRule getRule(Long id);

    void createRule(FertilizationRuleDTO dto);

    void updateRule(FertilizationRule rule);

    void deleteRule(Long id);

    List<RecommendResultVO> recommendRules(String species, Integer age, String season);

    PageVO<FertilizationRecord> listRecords(RecordQueryDTO dto);

    FertilizationRecord getRecord(Long id);

    void createRecord(FertilizationRecordDTO dto);

    void deleteRecord(Long id);

    StatsVO getRecordStats();
}
