package com.mf.fertilizer.fertilization.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mf.fertilizer.fertilization.entity.FertilizationRecord;
import com.mf.fertilizer.fertilization.vo.StatsVO;

public interface FertilizationRecordService extends IService<FertilizationRecord> {

    StatsVO getStats();
}
