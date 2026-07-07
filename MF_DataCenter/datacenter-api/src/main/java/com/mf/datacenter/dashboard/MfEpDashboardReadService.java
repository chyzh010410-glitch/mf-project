package com.mf.datacenter.dashboard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MfEpDashboardReadService {

    private final boolean enabled;
    private final String url;
    private final String username;
    private final String password;

    public MfEpDashboardReadService(
            @Value("${datacenter.mf-ep.datasource.enabled:false}") boolean enabled,
            @Value("${datacenter.mf-ep.datasource.url:}") String url,
            @Value("${datacenter.mf-ep.datasource.username:}") String username,
            @Value("${datacenter.mf-ep.datasource.password:}") String password
    ) {
        this.enabled = enabled;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public boolean enabled() {
        return enabled;
    }

    public DashboardFacts readFacts() {
        if (!enabled || url == null || url.isBlank()) {
            throw new IllegalStateException("MF_EP datasource is disabled");
        }
        try (var connection = openConnection()) {
            return new DashboardFacts(
                    count(connection, "SELECT COUNT(*) FROM user WHERE deleted = 0"),
                    count(connection, "SELECT COUNT(*) FROM product WHERE deleted = 0"),
                    count(connection, "SELECT COUNT(*) FROM product WHERE deleted = 0 AND status = 1"),
                    count(connection, "SELECT COUNT(*) FROM `order` WHERE deleted = 0"),
                    sum(connection, "SELECT COALESCE(SUM(pay_amount), 0) FROM `order` WHERE deleted = 0 AND status IN ('pending_ship','shipped','completed','refunding','refunded')"),
                    count(connection, "SELECT COUNT(*) FROM merchant WHERE deleted = 0"),
                    count(connection, "SELECT COUNT(*) FROM merchant WHERE deleted = 0 AND status = 'pending'"),
                    trend(connection, "SELECT DATE(create_time) AS day, COUNT(*) AS value FROM `order` WHERE deleted = 0 AND create_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) GROUP BY DATE(create_time)"),
                    trend(connection, "SELECT DATE(create_time) AS day, COALESCE(SUM(pay_amount), 0) AS value FROM `order` WHERE deleted = 0 AND status IN ('pending_ship','shipped','completed','refunding','refunded') AND create_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) GROUP BY DATE(create_time)"),
                    categoryShare(connection)
            );
        } catch (SQLException ex) {
            throw new IllegalStateException("failed to read MF_EP dashboard facts", ex);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private long count(Connection connection, String sql) throws SQLException {
        try (var statement = connection.prepareStatement(sql);
             var rs = statement.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private BigDecimal sum(Connection connection, String sql) throws SQLException {
        try (var statement = connection.prepareStatement(sql);
             var rs = statement.executeQuery()) {
            rs.next();
            return rs.getBigDecimal(1);
        }
    }

    private List<DashboardController.TrendPoint> trend(Connection connection, String sql) throws SQLException {
        var values = new ArrayList<DashboardController.TrendPoint>();
        var byDate = new java.util.HashMap<LocalDate, BigDecimal>();
        try (var statement = connection.prepareStatement(sql);
             var rs = statement.executeQuery()) {
            while (rs.next()) {
                byDate.put(rs.getDate("day").toLocalDate(), rs.getBigDecimal("value"));
            }
        }
        for (var i = 6; i >= 0; i--) {
            var day = LocalDate.now().minusDays(i);
            values.add(new DashboardController.TrendPoint(day.toString().substring(5), byDate.getOrDefault(day, BigDecimal.ZERO)));
        }
        return values;
    }

    private List<DashboardController.CategoryShare> categoryShare(Connection connection) throws SQLException {
        var rows = new ArrayList<DashboardController.CategoryShare>();
        var sql = """
                SELECT COALESCE(pc.name, '未分类') AS name, COALESCE(SUM(oi.total_price), 0) AS value
                FROM order_item oi
                JOIN `order` o ON o.id = oi.order_id AND o.deleted = 0
                JOIN product p ON p.id = oi.product_id AND p.deleted = 0
                LEFT JOIN product_category pc ON pc.id = p.category_id AND pc.deleted = 0
                WHERE oi.deleted = 0
                  AND o.status IN ('pending_ship','shipped','completed','refunding','refunded')
                GROUP BY COALESCE(pc.name, '未分类')
                ORDER BY value DESC
                LIMIT 6
                """;
        try (var statement = connection.prepareStatement(sql);
             var rs = statement.executeQuery()) {
            while (rs.next()) {
                rows.add(new DashboardController.CategoryShare(rs.getString("name"), rs.getBigDecimal("value")));
            }
        }
        return rows;
    }

    public record DashboardFacts(
            long userTotal,
            long productTotal,
            long onSaleProductTotal,
            long orderTotal,
            BigDecimal gmvTotal,
            long merchantTotal,
            long pendingMerchantTotal,
            List<DashboardController.TrendPoint> orderTrend,
            List<DashboardController.TrendPoint> gmvTrend,
            List<DashboardController.CategoryShare> categorySales
    ) {
    }
}
