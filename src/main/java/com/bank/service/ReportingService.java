package com.bank.service;

import com.bank.config.DataSourceConfig;
import com.bank.dto.ChannelMetric;
import com.bank.dto.FailureMetric;
import com.bank.dto.StatementItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReportingService {

    public List<StatementItem> getMiniStatement(String accountId, int limit) throws SQLException {
        String sql = """
                SELECT transaction_id, type, amount, source_account,
                       dest_account, channel, status, created_at
                FROM "transaction"
                WHERE source_account = ? OR dest_account = ?
                ORDER BY created_at DESC
                LIMIT ?
                """;

        List<StatementItem> items = new ArrayList<>();

        try (Connection conn = DataSourceConfig.getDataSource().getConnection()) {
            conn.setReadOnly(true);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, accountId);
                ps.setString(2, accountId);
                ps.setInt(3, limit);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(new StatementItem(
                                rs.getString("transaction_id"),
                                rs.getString("type"),
                                rs.getBigDecimal("amount"),
                                rs.getString("source_account"),
                                rs.getString("dest_account"),
                                rs.getString("channel"),
                                rs.getString("status"),
                                rs.getTimestamp("created_at").toLocalDateTime()
                        ));
                    }
                }
            }
        }
        return items;
    }

    public List<ChannelMetric> getChannelSummary() throws SQLException {
        String sql = """
                SELECT COALESCE(channel, 'UNKNOWN') AS channel,
                       COUNT(*) AS txn_count,
                       COALESCE(SUM(amount), 0) AS total_volume
                FROM "transaction"
                WHERE status = 'SUCCESS'
                GROUP BY channel
                ORDER BY total_volume DESC
                """;

        List<ChannelMetric> metrics = new ArrayList<>();

        try (Connection conn = DataSourceConfig.getDataSource().getConnection()) {
            conn.setReadOnly(true);

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    metrics.add(new ChannelMetric(
                            rs.getString("channel"),
                            rs.getLong("txn_count"),
                            rs.getBigDecimal("total_volume")
                    ));
                }
            }
        }
        return metrics;
    }

    public List<FailureMetric> getFailureSummary() throws SQLException {
        String sql = """
                SELECT error_category, COUNT(*) AS failure_count
                FROM failed_transaction
                GROUP BY error_category
                ORDER BY failure_count DESC
                """;

        List<FailureMetric> failures = new ArrayList<>();

        try (Connection conn = DataSourceConfig.getDataSource().getConnection()) {
            conn.setReadOnly(true);

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    failures.add(new FailureMetric(
                            rs.getString("error_category"),
                            rs.getLong("failure_count")
                    ));
                }
            }
        }
        return failures;
    }
}