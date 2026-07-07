package com.mf.fertilizer.fertilization.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.fertilizer.fertilization.entity.FertilizationRecord;
import com.mf.fertilizer.fertilization.mapper.FertilizationRecordMapper;
import com.mf.fertilizer.fertilization.service.FertilizationRecordService;
import com.mf.fertilizer.fertilization.vo.StatsVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class FertilizationRecordServiceImpl extends ServiceImpl<FertilizationRecordMapper, FertilizationRecord>
        implements FertilizationRecordService {

    @Override
    public StatsVO getStats() {
        var records = list();
        long totalRecords = records.size();

        var treeIds = records.stream().map(FertilizationRecord::getTreeId).distinct().count();
        var fertIds = records.stream().map(FertilizationRecord::getFertilizerId).distinct().count();

        BigDecimal totalAmount = records.stream()
                .map(FertilizationRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StatsVO(totalRecords, BigDecimal.ZERO, totalAmount, treeIds, fertIds);
    }
}
