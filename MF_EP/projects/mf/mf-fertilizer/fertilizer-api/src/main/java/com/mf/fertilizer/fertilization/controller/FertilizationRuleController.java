package com.mf.fertilizer.fertilization.controller;

import com.mf.fertilizer.annotation.RequireRole;
import com.mf.fertilizer.constant.RoleEnum;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.fertilization.dto.FertilizationRuleDTO;
import com.mf.fertilizer.fertilization.dto.RecommendRequestDTO;
import com.mf.fertilizer.fertilization.entity.FertilizationRule;
import com.mf.fertilizer.fertilization.service.FertilizationApplicationService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FertilizationRuleController {

    private final FertilizationApplicationService fertilizationApplicationService;

    @GetMapping({"/rule/page", "/client/fertilization/rules/page"})
    @RequireRole(RoleEnum.PUBLIC)
    public ResultVO<PageVO<FertilizationRule>> page(@Valid PageDTO dto) {
        return ResultVO.success(fertilizationApplicationService.listRules(dto));
    }

    @GetMapping({"/rule/{id}", "/client/fertilization/rules/{id}"})
    @RequireRole(RoleEnum.PUBLIC)
    public ResultVO<FertilizationRule> getById(@PathVariable Long id) {
        return ResultVO.success(fertilizationApplicationService.getRule(id));
    }

    @PostMapping({"/rule", "/admin/fertilization/rules"})
    public ResultVO<?> save(@Valid @RequestBody FertilizationRuleDTO dto) {
        fertilizationApplicationService.createRule(dto);
        return ResultVO.success();
    }

    @PutMapping({"/rule", "/admin/fertilization/rules"})
    public ResultVO<?> update(@RequestBody FertilizationRule rule) {
        fertilizationApplicationService.updateRule(rule);
        return ResultVO.success();
    }

    @DeleteMapping({"/rule/{id}", "/admin/fertilization/rules/{id}"})
    public ResultVO<?> delete(@PathVariable Long id) {
        fertilizationApplicationService.deleteRule(id);
        return ResultVO.success();
    }

    @PostMapping({"/rule/recommend", "/client/fertilization/rules/recommend"})
    @RequireRole(RoleEnum.PUBLIC)
    public ResultVO<?> recommend(@Valid @RequestBody RecommendRequestDTO dto) {
        return ResultVO.success(fertilizationApplicationService.recommendRules(
                dto.getSpecies(), dto.getAge(), dto.getSeason()));
    }
}
