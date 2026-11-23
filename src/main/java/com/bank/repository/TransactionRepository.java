package com.bank.repository;

import com.bank.model.Transaction;
import com.bank.model.TransactionStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TransactionRepository {

    public boolean existsById(Connection conn, String transactionId) throws SQLException {
        String sql = "SELECT 1 FROM \"transaction\" WHERE transaction_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insert(Connection conn, Transaction txn) throws SQLException {
        String sql = "INSERT INTO \"transaction\" (transaction_id, type, amount, source_account, dest_account, channel, status, reason) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, txn.getTransactionId());
            ps.setString(2, txn.getType().name());
            ps.setBigDecimal(3, txn.getAmount());
            ps.setString(4, txn.getSourceAccount());
            ps.setString(5, txn.getDestAccount());
            ps.setString(6, txn.getChannel());
            ps.setString(7, txn.getStatus().name());
            ps.setString(8, txn.getReason());
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection conn, String transactionId, TransactionStatus status, String reason) throws SQLException {
        String sql = "UPDATE \"transaction\" SET status = ?, reason = ? WHERE transaction_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, reason);
            ps.setString(3, transactionId);
            ps.executeUpdate();
        }
    }
}