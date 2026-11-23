package com.bank.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuditRepository {

    public void logAudit(Connection conn, String transactionId, String operation,
                         String result, int durationMs, String details) throws SQLException {
        String sql = "INSERT INTO transaction_audit (transaction_id, thread_name, operation, result, duration_ms, details) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transactionId);
            ps.setString(2, Thread.currentThread().getName());
            ps.setString(3, operation);
            ps.setString(4, result);
            ps.setInt(5, durationMs);
            ps.setString(6, details);
            ps.executeUpdate();
        }
    }

    public void logFailure(Connection conn, String transactionId, String errorCategory, String reason) throws SQLException {
        String sql = "INSERT INTO failed_transaction (transaction_id, error_category, reason) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transactionId);
            ps.setString(2, errorCategory);
            ps.setString(3, reason);
            ps.executeUpdate();
        }
    }
}