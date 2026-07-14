package com.mf.datacenter.dashboard;

import com.mf.datacenter.ai.AiDataStore;
import com.mf.datacenter.common.ApiResponse;
import com.mf.datacenter.metric.MetricSnapshotService;
import com.mf.datacenter.notification.NotificationService;
import com.mf.datacenter.quality.DataQualityService;
import com.mf.datacenter.source.SourceContractService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final AiDataStore aiDataStore;
    private final MfEpDashboardReadService mfEpDashboardReadService;
    private final MetricSnapshotService metricSnapshotService;
    private final DataQualityService dataQualityService;
    private final SourceContractService sourceContractService;
    private final NotificationService notificationService;

    public DashboardController(
            AiDataStore aiDataStore,
            MfEpDashboardReadService mfEpDashboardReadService,
            MetricSnapshotService metricSnapshotService,
            DataQualityService dataQualityService,
            SourceContractService sourceContractService,
            NotificationService notificationService
    ) {
        this.aiDataStore = aiDataStore;
        this.mfEpDashboardReadService = mfEpDashboardReadService;
        this.metricSnapshotService = metricSnapshotService;
        this.dataQualityService = dataQualityService;
        this.sourceContractService = sourceContractService;
        this.notificationService = notificationService;
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverview> overview() {
        var governance = governanceStatus();
        var snapshotCards = snapshotCards();
        if (!snapshotCards.isEmpty()) {
            var orderTrend = metricSnapshotService.dailyTrend("order_total");
            var gmvTrend = metricSnapshotService.dailyTrend("gmv_total");
            var categorySales = metricSnapshotService.latestCategorySales();
            return ApiResponse.ok(new DashboardOverview(snapshotCards, orderTrend, gmvTrend,
                    categorySales.isEmpty() ? fallbackCategorySales() : categorySales, governance, latestAgentWrite(), governanceTasks()));
        }
        var realtime = realtimeOverview();
        return ApiResponse.ok(new DashboardOverview(realtime.cards(), realtime.orderTrend(), realtime.gmvTrend(),
                realtime.categorySales(), governance, latestAgentWrite(), governanceTasks()));
    }

    private GovernanceStatus governanceStatus() {
        var source = sourceContractService.check();
        var quality = dataQualityService.summary();
        var latest = metricSnapshotService.latestDashboardMetrics();
        var latestSnapshotTime = latest.values().stream()
                .map(MetricSnapshotService.MetricSnapshot::snapshotTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        var snapshotAgeMinutes = latestSnapshotTime == null ? null : Duration.between(latestSnapshotTime, LocalDateTime.now()).toMinutes();
        var riskReasons = new ArrayList<String>();
        if (!Boolean.TRUE.equals(source.connected())) {
            riskReasons.add("MF_EP 源库连接不可用");
        }
        if (source.failedTables() > 0) {
            riskReasons.add("源表契约异常 " + source.failedTables() + " 项");
        }
        if (source.missingFields() > 0) {
            riskReasons.add("源表缺失字段 " + source.missingFields() + " 项");
        }
        if (quality.failed() > 0) {
            riskReasons.add("数据质量检查失败 " + quality.failed() + " 项");
        }
        if (latestSnapshotTime == null) {
            riskReasons.add("尚未生成指标快照，请先刷新小时快照");
        } else if (snapshotAgeMinutes > 120) {
            riskReasons.add("指标快照已过期 " + snapshotAgeMinutes + " 分钟，请刷新小时快照");
        }
        var activeIssues = dataQualityService.issues("open").size() + dataQualityService.issues("processing").size();
        if (activeIssues > 0) {
            riskReasons.add("存在 " + activeIssues + " 个待处理质量问题");
        }
        var healthy = Boolean.TRUE.equals(source.connected())
                && source.failedTables() == 0
                && quality.failed() == 0
                && activeIssues == 0
                && latestSnapshotTime != null
                && snapshotAgeMinutes <= 120;
        return new GovernanceStatus(
                healthy ? "trusted" : "risk",
                source.connected(),
                source.failedTables(),
                source.missingFields(),
                quality.failed(),
                activeIssues,
                latestSnapshotTime,
                snapshotAgeMinutes,
                healthy ? "数据源、质量和快照均正常" : String.join("；", riskReasons),
                riskReasons
        );
    }

    private AgentWrite latestAgentWrite() {
        return aiDataStore.conversations().stream()
                .findFirst()
                .map(item -> new AgentWrite(item.source(), item.intent(), item.question(), item.createTime()))
                .orElse(null);
    }

    private List<GovernanceTask> governanceTasks() {
        return notificationService.notifications().stream()
                .filter(item -> !item.read())
                .map(item -> new GovernanceTask(item.id(), item.title(), item.content(), item.severity(), "system", item.updateTime(), item.targetPath()))
                .toList();
    }

    private List<MetricCard> snapshotCards() {
        Map<String, MetricSnapshotService.MetricSnapshot> snapshots = metricSnapshotService.latestDashboardMetrics();
        if (!snapshots.keySet().containsAll(List.of("user_total", "product_total", "order_total", "gmv_total", "merchant_total", "ai_conversation_total"))) {
            return List.of();
        }
        var unresolved = snapshots.get("ai_unresolved_total");
        return List.of(
                card(snapshots.get("user_total"), "来自 dc_metric_snapshot"),
                card(snapshots.get("product_total"), "来自 dc_metric_snapshot"),
                card(snapshots.get("order_total"), "来自 dc_metric_snapshot"),
                card(snapshots.get("gmv_total"), "来自 dc_metric_snapshot"),
                card(snapshots.get("merchant_total"), "来自 dc_metric_snapshot"),
                card(snapshots.get("ai_conversation_total"), "未解决 " + (unresolved == null ? "0" : unresolved.metricValue().stripTrailingZeros().toPlainString()))
        );
    }

    private MetricCard card(MetricSnapshotService.MetricSnapshot snapshot, String note) {
        var value = "gmv_total".equals(snapshot.metricCode())
                ? "¥" + snapshot.metricValue()
                : snapshot.metricValue().stripTrailingZeros().toPlainString();
        return new MetricCard(snapshot.metricName(), snapshot.metricCode(), value, note + " / " + snapshot.snapshotGranularity());
    }

    private DashboardOverview realtimeOverview() {
        var aiStats = aiDataStore.stats();
        if (mfEpDashboardReadService.enabled()) {
            var facts = mfEpDashboardReadService.readFacts();
            var cards = List.of(
                    new MetricCard("用户总数", "user_total", String.valueOf(facts.userTotal()), "实时读取 MF_EP user"),
                    new MetricCard("商品总数", "product_total", String.valueOf(facts.productTotal()), "上架 " + facts.onSaleProductTotal()),
                    new MetricCard("订单数", "order_total", String.valueOf(facts.orderTotal()), "实时读取 MF_EP order"),
                    new MetricCard("GMV", "gmv_total", "¥" + facts.gmvTotal(), "实时读取 MF_EP order"),
                    new MetricCard("商家总数", "merchant_total", String.valueOf(facts.merchantTotal()), "待审核 " + facts.pendingMerchantTotal()),
                    new MetricCard("AI 咨询次数", "ai_conversation_total", String.valueOf(aiStats.conversationTotal()), "未解决 " + aiStats.unresolvedTotal())
            );
            return new DashboardOverview(cards, facts.orderTrend(), facts.gmvTrend(), facts.categorySales(), null, null, List.of());
        }
        var cards = List.of(
                new MetricCard("用户总数", "user_total", "12,580", "演示数据"),
                new MetricCard("商品总数", "product_total", "3,426", "上架 2,918"),
                new MetricCard("订单数", "order_total", "8,742", "近 7 日 +9.6%"),
                new MetricCard("GMV", "gmv_total", "¥684,230", "支付转化稳定"),
                new MetricCard("商家总数", "merchant_total", "386", "待审核 24"),
                new MetricCard("AI 咨询次数", "ai_conversation_total", String.valueOf(aiStats.conversationTotal()), "未解决 " + aiStats.unresolvedTotal())
        );
        var orderTrend = List.of(
                new TrendPoint("06-28", 108), new TrendPoint("06-29", 126),
                new TrendPoint("06-30", 118), new TrendPoint("07-01", 142),
                new TrendPoint("07-02", 153), new TrendPoint("07-03", 167),
                new TrendPoint("07-04", 184)
        );
        var gmvTrend = List.of(
                new TrendPoint("06-28", 68200), new TrendPoint("06-29", 73500),
                new TrendPoint("06-30", 70100), new TrendPoint("07-01", 82000),
                new TrendPoint("07-02", 89300), new TrendPoint("07-03", 94600),
                new TrendPoint("07-04", 102400)
        );
        return new DashboardOverview(cards, orderTrend, gmvTrend, fallbackCategorySales(), null, null, List.of());
    }

    private List<CategoryShare> fallbackCategorySales() {
        if (mfEpDashboardReadService.enabled()) {
            return mfEpDashboardReadService.readFacts().categorySales();
        }
        return List.of(
                new CategoryShare("有机肥", new BigDecimal("38.5")),
                new CategoryShare("水溶肥", new BigDecimal("26.0")),
                new CategoryShare("土壤改良", new BigDecimal("19.5")),
                new CategoryShare("农技服务", new BigDecimal("16.0"))
        );
    }

    public record DashboardOverview(
            List<MetricCard> cards,
            List<TrendPoint> orderTrend,
            List<TrendPoint> gmvTrend,
            List<CategoryShare> categorySales,
            GovernanceStatus governance,
            AgentWrite latestAgentWrite,
            List<GovernanceTask> governanceTasks
    ) {
    }

    public record GovernanceStatus(
            String status,
            Boolean sourceConnected,
            long sourceFailedTables,
            long sourceMissingFields,
            long qualityFailed,
            long activeIssues,
            LocalDateTime latestSnapshotTime,
            Long snapshotAgeMinutes,
            String message,
            List<String> riskReasons
    ) {
    }

    public record AgentWrite(String source, String intent, String question, LocalDateTime createTime) {
    }

    public record GovernanceTask(Long id, String title, String content, String severity, String owner, LocalDateTime lastSeenTime, String targetPath) {
    }

    public record MetricCard(String name, String code, String value, String note) {
    }

    public record TrendPoint(String date, Number value) {
    }

    public record CategoryShare(String name, BigDecimal value) {
    }
}
