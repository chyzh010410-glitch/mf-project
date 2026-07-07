package com.mf.datacenter.source;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mf.datacenter.source.entity.SourceFieldContractEntity;
import com.mf.datacenter.source.entity.SourceTableContractEntity;
import com.mf.datacenter.source.mapper.SourceFieldContractMapper;
import com.mf.datacenter.source.mapper.SourceTableContractMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SourceContractService {

    private final SourceTableContractMapper tableContractMapper;
    private final SourceFieldContractMapper fieldContractMapper;
    private final boolean mfEpEnabled;
    private final String mfEpUrl;
    private final String mfEpUsername;
    private final String mfEpPassword;

    public SourceContractService(
            ObjectProvider<SourceTableContractMapper> tableContractMapper,
            ObjectProvider<SourceFieldContractMapper> fieldContractMapper,
            @Value("${datacenter.mf-ep.datasource.enabled:false}") boolean mfEpEnabled,
            @Value("${datacenter.mf-ep.datasource.url:}") String mfEpUrl,
            @Value("${datacenter.mf-ep.datasource.username:}") String mfEpUsername,
            @Value("${datacenter.mf-ep.datasource.password:}") String mfEpPassword
    ) {
        this.tableContractMapper = tableContractMapper.getIfAvailable();
        this.fieldContractMapper = fieldContractMapper.getIfAvailable();
        this.mfEpEnabled = mfEpEnabled;
        this.mfEpUrl = mfEpUrl;
        this.mfEpUsername = mfEpUsername;
        this.mfEpPassword = mfEpPassword;
    }

    public List<SourceTableContract> contracts() {
        if (tableContractMapper == null || fieldContractMapper == null) {
            return List.of();
        }
        var tables = tableContractMapper.selectList(new LambdaQueryWrapper<SourceTableContractEntity>()
                .orderByAsc(SourceTableContractEntity::getSourceName)
                .orderByAsc(SourceTableContractEntity::getTableName));
        var fieldsByTable = fieldContractMapper.selectList(new LambdaQueryWrapper<SourceFieldContractEntity>()
                        .orderByAsc(SourceFieldContractEntity::getFieldName))
                .stream()
                .collect(Collectors.groupingBy(SourceFieldContractEntity::getTableContractId));
        return tables.stream()
                .map(table -> toContract(table, fieldsByTable.getOrDefault(table.getId(), List.of())))
                .toList();
    }

    public SourceCheckSummary check() {
        var checkedAt = LocalDateTime.now();
        if (!mfEpEnabled || mfEpUrl == null || mfEpUrl.isBlank()) {
            return new SourceCheckSummary("MF_EP", false, "MF_EP 数据源未启用", checkedAt, 0, 0, 0, contracts(), List.of());
        }
        var contracts = contracts().stream().filter(SourceTableContract::enabled).toList();
        try (var connection = DriverManager.getConnection(mfEpUrl, mfEpUsername, mfEpPassword)) {
            var meta = connection.getMetaData();
            var tableResults = contracts.stream().map(contract -> {
                var tableExists = existsTable(meta, contract.schemaName(), contract.tableName());
                var fieldResults = contract.fields().stream()
                        .map(field -> {
                            var actualType = findColumnType(meta, contract.schemaName(), contract.tableName(), field.fieldName());
                            var passed = actualType != null || !field.required();
                            return new SourceFieldCheck(field.fieldName(), field.fieldType(), actualType, field.required(), passed,
                                    passed ? "字段存在" : "必需字段缺失");
                        })
                        .toList();
                var missingFields = fieldResults.stream().filter(item -> !item.passed()).count();
                var passed = tableExists && missingFields == 0;
                return new SourceTableCheck(
                        contract.sourceName(),
                        contract.schemaName(),
                        contract.tableName(),
                        contract.businessName(),
                        tableExists,
                        passed,
                        passed ? "契约通过" : tableExists ? "字段契约不满足" : "源表缺失",
                        fieldResults
                );
            }).toList();
            var failedTables = tableResults.stream().filter(item -> !item.passed()).count();
            var missingFields = tableResults.stream()
                    .flatMap(item -> item.fields().stream())
                    .filter(item -> !item.passed())
                    .count();
            return new SourceCheckSummary("MF_EP", true, failedTables == 0 ? "源库契约通过" : "源库契约存在风险",
                    checkedAt, tableResults.size(), failedTables, missingFields, contracts, tableResults);
        } catch (SQLException ex) {
            return new SourceCheckSummary("MF_EP", false, "源库连接失败：" + ex.getMessage(), checkedAt,
                    contracts.size(), contracts.size(), 0, contracts, List.of());
        }
    }

    private boolean existsTable(java.sql.DatabaseMetaData meta, String schemaName, String tableName) {
        try (var rs = meta.getTables(schemaName, null, tableName, new String[]{"TABLE"})) {
            if (rs.next()) {
                return true;
            }
        } catch (SQLException ignored) {
        }
        try (var rs = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        } catch (SQLException ignored) {
            return false;
        }
    }

    private String findColumnType(java.sql.DatabaseMetaData meta, String schemaName, String tableName, String fieldName) {
        try (var rs = meta.getColumns(schemaName, null, tableName, fieldName)) {
            if (rs.next()) {
                return rs.getString("TYPE_NAME");
            }
        } catch (SQLException ignored) {
        }
        try (var rs = meta.getColumns(null, null, tableName, fieldName)) {
            if (rs.next()) {
                return rs.getString("TYPE_NAME");
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    private SourceTableContract toContract(SourceTableContractEntity table, List<SourceFieldContractEntity> fields) {
        return new SourceTableContract(
                table.getId(),
                table.getSourceName(),
                table.getSchemaName(),
                table.getTableName(),
                table.getBusinessName(),
                table.getDescription(),
                Boolean.TRUE.equals(table.getEnabled()),
                fields.stream().map(this::toField).toList()
        );
    }

    private SourceFieldContract toField(SourceFieldContractEntity field) {
        return new SourceFieldContract(
                field.getId(),
                field.getFieldName(),
                field.getFieldType(),
                Boolean.TRUE.equals(field.getRequired()),
                field.getDescription()
        );
    }

    public record SourceTableContract(
            Long id,
            String sourceName,
            String schemaName,
            String tableName,
            String businessName,
            String description,
            Boolean enabled,
            List<SourceFieldContract> fields
    ) {
    }

    public record SourceFieldContract(
            Long id,
            String fieldName,
            String fieldType,
            Boolean required,
            String description
    ) {
    }

    public record SourceCheckSummary(
            String sourceName,
            Boolean connected,
            String message,
            LocalDateTime checkedAt,
            long tableTotal,
            long failedTables,
            long missingFields,
            List<SourceTableContract> contracts,
            List<SourceTableCheck> tables
    ) {
    }

    public record SourceTableCheck(
            String sourceName,
            String schemaName,
            String tableName,
            String businessName,
            Boolean tableExists,
            Boolean passed,
            String message,
            List<SourceFieldCheck> fields
    ) {
    }

    public record SourceFieldCheck(
            String fieldName,
            String expectedType,
            String actualType,
            Boolean required,
            Boolean passed,
            String message
    ) {
    }
}
