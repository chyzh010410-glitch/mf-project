package com.mf.datacenter.metric.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mf.datacenter.metric.entity.MetricSnapshotEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MetricSnapshotMapper extends BaseMapper<MetricSnapshotEntity> {
}
