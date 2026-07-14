package com.mf.datacenter.analysis;

import com.mf.datacenter.ai.AiDataStore;
import com.mf.datacenter.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDate;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AiDataStore aiDataStore;

    public AnalysisController(AiDataStore aiDataStore) {
        this.aiDataStore = aiDataStore;
    }

    @GetMapping("/products")
    public ApiResponse<ProductAnalysis> products() {
        return ApiResponse.ok(new ProductAnalysis(
                List.of(
                        new RankItem("菌肥 1 号", 2860, "复购率 31%"),
                        new RankItem("水溶肥 A 套装", 2418, "转化率 8.4%"),
                        new RankItem("土壤修复包", 1982, "咨询增长快")
                ),
                List.of(
                        new RiskItem("复合微生物菌剂", "库存 18", "low_stock"),
                        new RiskItem("果蔬冲施肥", "转化率 1.6%", "low_conversion")
                ),
                List.of(new RankItem("有机肥", 382000, "销售额"), new RankItem("水溶肥", 258000, "销售额"))
        ));
    }

    @GetMapping("/content")
    public ApiResponse<ContentAnalysis> content() {
        return ApiResponse.ok(new ContentAnalysis(
                List.of(new RankItem("番茄黄叶怎么办", 1850, "百科"), new RankItem("土壤板结处理", 1630, "百科")),
                List.of(new RankItem("夏季追肥指南", 1420, "文章"), new RankItem("水肥一体化入门", 1196, "文章")),
                List.of(new TrendPoint("06-28", 210), new TrendPoint("06-29", 244), new TrendPoint("07-04", 326)),
                List.of("葡萄裂果防治", "有机肥和复合肥混用", "低温寡照补肥")
        ));
    }

    @GetMapping("/merchants")
    public ApiResponse<MerchantAnalysis> merchants() {
        return ApiResponse.ok(new MerchantAnalysis(
                List.of(new StatusCount("pending", 24), new StatusCount("approved", 318), new StatusCount("rejected", 16), new StatusCount("disabled", 8)),
                List.of(new RankItem("华北农资旗舰店", 286, "商品数"), new RankItem("苗丰优选服务商", 242, "商品数")),
                List.of(new RankItem("苗丰优选服务商", 920, "订单项"), new RankItem("川渝农资仓", 684, "订单项")),
                List.of(new RiskItem("华南仓配服务商", "12 单超过 36 小时未发货", "shipping_risk"))
        ));
    }

    @GetMapping("/ai")
    public ApiResponse<AiAnalysis> ai() {
        var stats = aiDataStore.stats();
        var toolCalls = aiDataStore.toolCalls();
        var samples = aiDataStore.sampleCandidates(null, null);
        var successfulTools = toolCalls.stream().filter(item -> Boolean.TRUE.equals(item.success())).count();
        var approvedSamples = samples.stream().filter(item -> "approved".equals(item.reviewStatus())).count();
        var trends = new ArrayList<TrendPoint>();
        var conversations = aiDataStore.conversations();
        for (var i = 6; i >= 0; i--) {
            var day = LocalDate.now().minusDays(i);
            trends.add(new TrendPoint(day.toString().substring(5), conversations.stream().filter(item -> day.equals(item.createTime().toLocalDate())).count()));
        }
        return ApiResponse.ok(new AiAnalysis(
                stats.conversationTotal(),
                stats.uniqueUserTotal(),
                stats.unresolvedTotal(),
                stats.toolCallTotal(),
                stats.sampleCandidateTotal(),
                stats.frequentQuestions().stream()
                        .map(item -> new RankItem(item.question(), item.count(), "咨询日志"))
                        .toList(),
                ratio(successfulTools, toolCalls.size()),
                ratio(stats.unresolvedTotal(), stats.conversationTotal()),
                ratio(approvedSamples, samples.size()),
                trends
        ));
    }

    private double ratio(long numerator, long denominator) {
        return denominator == 0 ? 0 : Math.round(numerator * 1000.0 / denominator) / 10.0;
    }

    public record ProductAnalysis(List<RankItem> hotProducts, List<RiskItem> riskProducts, List<RankItem> categorySales) {
    }

    public record ContentAnalysis(List<RankItem> hotEncyclopedias, List<RankItem> hotArticles, List<TrendPoint> interactionTrend, List<String> knowledgeGaps) {
    }

    public record MerchantAnalysis(List<StatusCount> statusDistribution, List<RankItem> productRank, List<RankItem> orderItemRank, List<RiskItem> shippingRisks) {
    }

    public record AiAnalysis(Integer conversationTotal, Integer uniqueUserTotal, Long unresolvedTotal, Integer toolCallTotal, Integer sampleCandidateTotal, List<RankItem> frequentQuestions, double toolSuccessRate, double unresolvedRate, double sampleApprovalRate, List<TrendPoint> conversationTrend) {
    }

    public record RankItem(String name, Number value, String note) {
    }

    public record RiskItem(String name, String reason, String type) {
    }

    public record StatusCount(String status, Integer count) {
    }

    public record TrendPoint(String date, Number value) {
    }
}
