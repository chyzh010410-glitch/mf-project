package com.mf.fertilizer.fertilization.controller;

import com.mf.fertilizer.annotation.RequireRole;
import com.mf.fertilizer.constant.RoleEnum;
import com.mf.fertilizer.fertilization.dto.TreeQueryDTO;
import com.mf.fertilizer.fertilization.entity.Tree;
import com.mf.fertilizer.fertilization.service.FertilizationApplicationService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TreeController {

    private final FertilizationApplicationService fertilizationApplicationService;

    @GetMapping({"/tree/page", "/client/fertilization/trees/page"})
    @RequireRole(RoleEnum.PUBLIC)
    public ResultVO<PageVO<Tree>> page(@Valid TreeQueryDTO dto) {
        return ResultVO.success(fertilizationApplicationService.listTrees(dto));
    }

    @GetMapping({"/tree/{id}", "/client/fertilization/trees/{id}"})
    @RequireRole(RoleEnum.PUBLIC)
    public ResultVO<Tree> getById(@PathVariable Long id) {
        return ResultVO.success(fertilizationApplicationService.getTree(id));
    }

    @PostMapping({"/tree", "/admin/fertilization/trees"})
    public ResultVO<?> save(@RequestBody Tree tree) {
        fertilizationApplicationService.createTree(tree);
        return ResultVO.success();
    }

    @PutMapping({"/tree", "/admin/fertilization/trees"})
    public ResultVO<?> update(@RequestBody Tree tree) {
        fertilizationApplicationService.updateTree(tree);
        return ResultVO.success();
    }

    @DeleteMapping({"/tree/{id}", "/admin/fertilization/trees/{id}"})
    public ResultVO<?> delete(@PathVariable Long id) {
        fertilizationApplicationService.deleteTree(id);
        return ResultVO.success();
    }

    @GetMapping({"/tree/species", "/client/fertilization/trees/species"})
    @RequireRole(RoleEnum.PUBLIC)
    public ResultVO<?> species() {
        return ResultVO.success(fertilizationApplicationService.getTreeSpecies());
    }
}
