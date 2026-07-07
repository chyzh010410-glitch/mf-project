package com.mf.fertilizer.fertilization.controller;

import com.mf.fertilizer.annotation.RequireRole;
import com.mf.fertilizer.constant.RoleEnum;
import com.mf.fertilizer.fertilization.dto.FertilizationRecordDTO;
import com.mf.fertilizer.fertilization.dto.RecordQueryDTO;
import com.mf.fertilizer.fertilization.entity.FertilizationRecord;
import com.mf.fertilizer.fertilization.service.FertilizationApplicationService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FertilizationRecordController {

    private final FertilizationApplicationService fertilizationApplicationService;

    @GetMapping({"/record/page", "/client/fertilization/records/page"})
    @RequireRole(RoleEnum.PUBLIC)
    public ResultVO<PageVO<FertilizationRecord>> page(@Valid RecordQueryDTO dto) {
        return ResultVO.success(fertilizationApplicationService.listRecords(dto));
    }

    @GetMapping({"/record/{id}", "/client/fertilization/records/{id}"})
    @RequireRole(RoleEnum.PUBLIC)
    public ResultVO<FertilizationRecord> getById(@PathVariable Long id) {
        return ResultVO.success(fertilizationApplicationService.getRecord(id));
    }

    @PostMapping({"/record", "/admin/fertilization/records"})
    public ResultVO<?> add(@Valid @RequestBody FertilizationRecordDTO dto) {
        fertilizationApplicationService.createRecord(dto);
        return ResultVO.success();
    }

    @DeleteMapping({"/record/{id}", "/admin/fertilization/records/{id}"})
    public ResultVO<?> delete(@PathVariable Long id) {
        fertilizationApplicationService.deleteRecord(id);
        return ResultVO.success();
    }

    @GetMapping({"/record/stats", "/client/fertilization/records/stats"})
    @RequireRole(RoleEnum.PUBLIC)
    public ResultVO<?> stats() {
        return ResultVO.success(fertilizationApplicationService.getRecordStats());
    }
}
