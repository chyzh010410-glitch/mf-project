package com.mf.fertilizer.fertilization.controller;

import com.mf.fertilizer.annotation.RequireRole;
import com.mf.fertilizer.constant.RoleEnum;
import com.mf.fertilizer.fertilization.dto.FertilizerQueryDTO;
import com.mf.fertilizer.fertilization.entity.Fertilizer;
import com.mf.fertilizer.fertilization.service.FertilizationApplicationService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FertilizerController {

    private final FertilizationApplicationService fertilizationApplicationService;

    @GetMapping({"/fertilizer/page", "/client/fertilization/fertilizers/page"})
    @RequireRole(RoleEnum.PUBLIC)
    public ResultVO<PageVO<Fertilizer>> page(@Valid FertilizerQueryDTO dto) {
        return ResultVO.success(fertilizationApplicationService.listFertilizers(dto));
    }

    @GetMapping({"/fertilizer/list", "/client/fertilization/fertilizers/list"})
    @RequireRole(RoleEnum.PUBLIC)
    public ResultVO<?> list() {
        return ResultVO.success(fertilizationApplicationService.getFertilizerList());
    }

    @GetMapping({"/fertilizer/{id}", "/client/fertilization/fertilizers/{id}"})
    @RequireRole(RoleEnum.PUBLIC)
    public ResultVO<Fertilizer> getById(@PathVariable Long id) {
        return ResultVO.success(fertilizationApplicationService.getFertilizer(id));
    }

    @PostMapping({"/fertilizer", "/admin/fertilization/fertilizers"})
    public ResultVO<?> save(@RequestBody Fertilizer fertilizer) {
        fertilizationApplicationService.createFertilizer(fertilizer);
        return ResultVO.success();
    }

    @PutMapping({"/fertilizer", "/admin/fertilization/fertilizers"})
    public ResultVO<?> update(@RequestBody Fertilizer fertilizer) {
        fertilizationApplicationService.updateFertilizer(fertilizer);
        return ResultVO.success();
    }

    @DeleteMapping({"/fertilizer/{id}", "/admin/fertilization/fertilizers/{id}"})
    public ResultVO<?> delete(@PathVariable Long id) {
        fertilizationApplicationService.deleteFertilizer(id);
        return ResultVO.success();
    }
}
