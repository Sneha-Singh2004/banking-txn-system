package com.bank.repository;

import com.bank.model.Account;
import com.bank.model.AccountStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class AccountRepository {

    public Optional<Account> findById(Connection conn, String accountId) throws SQLException {
        String sql = "SELECT account_id, customer_id, balance, status, version, created_at FROM account WHERE account_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Account> findByIdForUpdate(Connection conn, String accountId) throws SQLException {
        String sql = "SELECT account_id, customer_id, balance, status, version, created_at FROM account WHERE account_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public boolean updateBalance(Connection conn, String accountId, BigDecimal newBalance, long currentVersion) throws SQLException {
        String sql = "UPDATE account SET balance = ?, version = version + 1 WHERE account_id = ? AND version = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setString(2, accountId);
            ps.setLong(3, currentVersion);
            return ps.executeUpdate() == 1;
        }
    }

    private Account mapRow(ResultSet rs) throws SQLException {
        return new Account(
                rs.getString("account_id"),
                rs.getLong("customer_id"),
                rs.getBigDecimal("balance"),
                AccountStatus.valueOf(rs.getString("status")),
                rs.getLong("version"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}