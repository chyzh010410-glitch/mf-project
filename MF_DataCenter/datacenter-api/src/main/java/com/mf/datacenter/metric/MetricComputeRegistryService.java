package com.mf.datacenter.metric;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mf.datacenter.metric.entity.MetricComputeRegistryEntity;
import com.mf.datacenter.metric.mapper.MetricComputeRegistryMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MetricComputeRegistryService {

    private final MetricComputeRegistryMapper registryMapper;

    public MetricComputeRegistryService(ObjectProvider<MetricComputeRegistryMapper> registryMapper) {
        this.registryMapper = registryMapper.getIfAvailable();
    }

    public List<MetricComputeRegistry> registries() {
        if (registryMapper == null) {
            return fallbackRegistries();
        }
        return registryMapper.selectList(new LambdaQueryWrapper<MetricComputeRegistryEntity>()
                        .orderByDesc(MetricComputeRegistryEntity::getEnabled)
                        .orderByAsc(MetricComputeRegistryEntity::getMetricCode))
                .stream()
                .map(this::toRecord)
                .toList();
    }

    public Set<String> activeMetricCodes() {
        return registries().stream()
                .filter(MetricComputeRegistry::enabled)
                .map(MetricComputeRegistry::metricCode)
                .collect(Collectors.toSet());
    }

    public MetricComputeRegistry setEnabled(Long id, boolean enabled) {
        if (registryMapper == null) {
            throw new IllegalArgumentException("当前未启用指标计算注册表数据库");
        }
        var entity = registryMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("指标计算注册不存在");
        }
        var now = LocalDateTime.now();
        registryMapper.update(null, new LambdaUpdateWrapper<MetricComputeRegistryEntity>()
                .eq(MetricComputeRegistryEntity::getId, id)
                .set(MetricComputeRegistryEntity::getEnabled, enabled)
                .set(MetricComputeRegistryEntity::getUpdateTime, now));
        entity.setEnabled(enabled);
        entity.setUpdateTime(now);
        return toRecord(entity);
    }

    private MetricComputeRegistry toRecord(MetricComputeRegistryEntity entity) {
        return new MetricComputeRegistry(
                entity.getId(),
                entity.getMetricCode(),
                entity.getComputeHandler(),
                entity.getSourceName(),
                entity.getSourceContract(),
                entity.getComputeMode(),
                entity.getFormulaText(),
                Boolean.TRUE.equals(entity.getEnabled()),
                entity.getDescription()
        );
    }

    private List<MetricComputeRegistry> fallbackRegistries() {
        return List.of(
                new MetricComputeRegistry(null, "user_total", "mf_ep_user_total", "MF_EP", "fertilizer.user", "builtin", "count(user.id) where deleted = 0", true, "用户总数受控计算"),
                new MetricComputeRegistry(null, "product_total", "mf_ep_product_total", "MF_EP", "fertilizer.product", "builtin", "count(product.id) where deleted = 0", true, "商品总数受控计算"),
                new MetricComputeRegistry(null, "order_total", "mf_ep_order_total", "MF_EP", "fertilizer.order", "builtin", "count(order.id) where deleted = 0", true, "订单数受控计算"),
                new MetricComputeRegistry(null, "gmv_total", "mf_ep_gmv_total", "MF_EP", "fertilizer.order", "builtin", "sum(order.pay_amount) for paid order statuses", true, "GMV 受控计算"),
                new MetricComputeRegistry(null, "merchant_total", "mf_ep_merchant_total", "MF_EP", "fertilizer.merchant", "builtin", "count(merchant.id) where deleted = 0", true, "商家总数受控计算"),
                new MetricComputeRegistry(null, "category_sales", "mf_ep_category_sales", "MF_EP", "fertilizer.order_item/product/product_category", "builtin-dimension", "sum(order_item.total_price) group by product_category.name", true, "分类销售额受控计算"),
                new MetricComputeRegistry(null, "ai_conversation_total", "dc_ai_conversation_total", "DataCenter", "dc_ai_conversation_log", "builtin", "count(ai_conversation_log.id)", true, "AI 咨询次数受控计算"),
                new MetricComputeRegistry(null, "ai_unresolved_total", "dc_ai_unresolved_total", "DataCenter", "dc_unresolved_question", "builtin", "count(unresolved_question.id) where status in pending, processing", true, "AI 未解决问题数受控计算")
        );
    }

    public record MetricComputeRegistry(
            Long id,
            String metricCode,
            String computeHandler,
            String sourceName,
            String sourceContract,
            String computeMode,
            String formulaText,
            Boolean enabled,
            String description
    ) {
    }
}
