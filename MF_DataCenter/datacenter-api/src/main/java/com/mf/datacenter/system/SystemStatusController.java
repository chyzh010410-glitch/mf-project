package com.mf.datacenter.system;

import com.mf.datacenter.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/system")
public class SystemStatusController {

    private final String aiDataFile;
    private final boolean mysqlEnabled;
    private final String mysqlDatabase;
    private final boolean mfEpDatasourceEnabled;

    public SystemStatusController(
            @Value("${datacenter.storage.ai-data-file:data/ai-store.json}") String aiDataFile,
            @Value("${datacenter.mysql.enabled:false}") boolean mysqlEnabled,
            @Value("${datacenter.mysql.database:}") String mysqlDatabase,
            @Value("${datacenter.mf-ep.datasource.enabled:false}") boolean mfEpDatasourceEnabled
    ) {
        this.aiDataFile = aiDataFile;
        this.mysqlEnabled = mysqlEnabled;
        this.mysqlDatabase = mysqlDatabase;
        this.mfEpDatasourceEnabled = mfEpDatasourceEnabled;
    }

    @GetMapping("/status")
    public ApiResponse<SystemStatus> status() {
        return ApiResponse.ok(new SystemStatus(
                "running",
                LocalDateTime.now(),
                new DataSourceState("AI 数据沉淀", mysqlEnabled ? "mysql-mybatis-plus" : "local-json", true,
                        mysqlEnabled ? mysqlDatabase : aiDataFile, "咨询、工具调用、问题池、样本池写入 DataCenter"),
                new DataSourceState("MF_EP 业务库", "mysql-readonly", mfEpDatasourceEnabled,
                        "datacenter.mf-ep.datasource", mfEpDatasourceEnabled ? "运营总览读取真实业务数据" : "未启用时运营总览使用演示数据"),
                List.of(
                        new MockDomain("运营总览", mfEpDatasourceEnabled ? "用户、商品、订单、GMV、商家指标已接真实库" : "业务指标为演示数据"),
                        new MockDomain("商品分析", "热门商品、风险商品、分类排行仍为后续扩展范围"),
                        new MockDomain("内容分析", "热门内容、互动趋势、知识缺口仍为后续扩展范围"),
                        new MockDomain("商家分析", "审核分布、排行、发货风险仍为后续扩展范围")
                )
        ));
    }

    public record SystemStatus(
            String status,
            LocalDateTime serverTime,
            DataSourceState aiStorage,
            DataSourceState mfEpDatasource,
            List<MockDomain> mockDomains
    ) {
    }

    public record DataSourceState(String name, String type, Boolean enabled, String location, String note) {
    }

    public record MockDomain(String name, String note) {
    }
}
