package com.mf.datacenter.metric;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mf.datacenter.metric.entity.MetricDefinitionEntity;
import com.mf.datacenter.metric.mapper.MetricDefinitionMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MetricDefinitionService {

    private final MetricDefinitionMapper metricDefinitionMapper;

    public MetricDefinitionService(ObjectProvider<MetricDefinitionMapper> metricDefinitionMapper) {
        this.metricDefinitionMapper = metricDefinitionMapper.getIfAvailable();
    }

    public List<MetricDefinition> definitions() {
        if (metricDefinitionMapper == null) {
            return fallbackDefinitions();
        }
        return metricDefinitionMapper.selectList(new LambdaQueryWrapper<MetricDefinitionEntity>()
                        .orderByDesc(MetricDefinitionEntity::getEnabled)
                        .orderByAsc(MetricDefinitionEntity::getMetricCode))
                .stream()
                .map(this::toRecord)
                .toList();
    }

    public List<MetricDefinition> activeDefinitions() {
        return definitions().stream().filter(MetricDefinition::enabled).toList();
    }

    public MetricDefinition create(MetricDefinitionRequest request) {
        ensureMapper();
        validate(request);
        var exists = metricDefinitionMapper.selectOne(new LambdaQueryWrapper<MetricDefinitionEntity>()
                .eq(MetricDefinitionEntity::getMetricCode, request.metricCode().trim()));
        if (exists != null) {
            throw new IllegalArgumentException("指标编码已存在");
        }
        var now = LocalDateTime.now();
        var entity = new MetricDefinitionEntity();
        apply(entity, request);
        entity.setEnabled(request.enabled() == null || request.enabled());
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        metricDefinitionMapper.insert(entity);
        return toRecord(entity);
    }

    public MetricDefinition update(Long id, MetricDefinitionRequest request) {
        ensureMapper();
        validate(request);
        var entity = requireEntity(id);
        var exists = metricDefinitionMapper.selectOne(new LambdaQueryWrapper<MetricDefinitionEntity>()
                .eq(MetricDefinitionEntity::getMetricCode, request.metricCode().trim())
                .ne(MetricDefinitionEntity::getId, id));
        if (exists != null) {
            throw new IllegalArgumentException("指标编码已存在");
        }
        apply(entity, request);
        entity.setEnabled(request.enabled() == null || request.enabled());
        entity.setUpdateTime(LocalDateTime.now());
        metricDefinitionMapper.updateById(entity);
        return toRecord(entity);
    }

    public MetricDefinition setEnabled(Long id, boolean enabled) {
        ensureMapper();
        var entity = requireEntity(id);
        var now = LocalDateTime.now();
        metricDefinitionMapper.update(null, new LambdaUpdateWrapper<MetricDefinitionEntity>()
                .eq(MetricDefinitionEntity::getId, id)
                .set(MetricDefinitionEntity::getEnabled, enabled)
                .set(MetricDefinitionEntity::getUpdateTime, now));
        entity.setEnabled(enabled);
        entity.setUpdateTime(now);
        return toRecord(entity);
    }

    private void apply(MetricDefinitionEntity entity, MetricDefinitionRequest request) {
        entity.setMetricCode(request.metricCode().trim());
        entity.setMetricName(request.metricName().trim());
        entity.setSourceTable(request.sourceTable().trim());
        entity.setFormula(request.formula().trim());
        entity.setPeriod(request.period().trim());
        entity.setOwner(request.owner().trim());
        entity.setDescription(request.description());
    }

    private MetricDefinitionEntity requireEntity(Long id) {
        var entity = metricDefinitionMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("指标不存在");
        }
        return entity;
    }

    private void ensureMapper() {
        if (metricDefinitionMapper == null) {
            throw new IllegalArgumentException("当前未启用指标字典数据库");
        }
    }

    private void validate(MetricDefinitionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (blank(request.metricCode()) || !request.metricCode().trim().matches("[a-z][a-z0-9_]{2,63}")) {
            throw new IllegalArgumentException("指标编码需为小写字母、数字或下划线，且以字母开头");
        }
        if (blank(request.metricName()) || blank(request.sourceTable()) || blank(request.formula())
                || blank(request.period()) || blank(request.owner())) {
            throw new IllegalArgumentException("指标名称、来源、口径、周期、负责人不能为空");
        }
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private MetricDefinition toRecord(MetricDefinitionEntity entity) {
        return new MetricDefinition(
                entity.getId(),
                entity.getMetricCode(),
                entity.getMetricName(),
                entity.getSourceTable(),
                entity.getFormula(),
                entity.getPeriod(),
                entity.getOwner(),
                entity.getDescription(),
                Boolean.TRUE.equals(entity.getEnabled())
        );
    }

    private List<MetricDefinition> fallbackDefinitions() {
        return List.of(
                new MetricDefinition(null, "user_total", "用户总数", "MF_EP.user", "count(*) where deleted = 0", "hourly,daily", "platform-ops", "消费者用户总量", true),
                new MetricDefinition(null, "product_total", "商品总数", "MF_EP.product", "count(*) where deleted = 0", "hourly,daily", "product-ops", "商品 SPU 总量", true),
                new MetricDefinition(null, "order_total", "订单数", "MF_EP.order", "count(*) where deleted = 0", "hourly,daily", "platform-ops", "订单主表总量", true),
                new MetricDefinition(null, "gmv_total", "GMV", "MF_EP.order", "sum(pay_amount)", "hourly,daily", "platform-ops", "已支付链路订单金额", true),
                new MetricDefinition(null, "ai_conversation_total", "AI 咨询次数", "dc_ai_conversation_log", "count(*)", "hourly,daily", "ai-ops", "AI 咨询会话累计量", true)
        );
    }

    public record MetricDefinition(
            Long id,
            String metricCode,
            String metricName,
            String sourceTable,
            String formula,
            String period,
            String owner,
            String description,
            Boolean enabled
    ) {
    }

    public record MetricDefinitionRequest(
            String metricCode,
            String metricName,
            String sourceTable,
            String formula,
            String period,
            String owner,
            String description,
            Boolean enabled
    ) {
    }
}
