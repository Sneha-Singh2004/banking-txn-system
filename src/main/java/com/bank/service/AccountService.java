package com.bank.service;

import com.bank.config.DataSourceConfig;
import com.bank.dto.AccountSummary;
import com.bank.model.AccountStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class AccountService {

    public AccountSummary openAccount(String accountId, String fullName, String email,
                                      String phone, BigDecimal initialDeposit) throws SQLException {
        if (initialDeposit == null || initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial deposit cannot be negative");
        }

        String insertCustomerSql = "INSERT INTO customer (name, email, phone) VALUES (?, ?, ?)";
        String insertAccountSql = "INSERT INTO account (account_id, customer_id, balance, status) VALUES (?, ?, ?, 'ACTIVE')";

        try (Connection conn = DataSourceConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try {
                long customerId;
                try (PreparedStatement psCust = conn.prepareStatement(insertCustomerSql, Statement.RETURN_GENERATED_KEYS)) {
                    psCust.setString(1, fullName);
                    psCust.setString(2, email);
                    psCust.setString(3, phone);
                    psCust.executeUpdate();

                    try (ResultSet keys = psCust.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Failed to retrieve generated customer_id");
                        }
                        customerId = keys.getLong(1);
                    }
                }

                try (PreparedStatement psAcc = conn.prepareStatement(insertAccountSql)) {
                    psAcc.setString(1, accountId);
                    psAcc.setLong(2, customerId);
                    psAcc.setBigDecimal(3, initialDeposit);
                    psAcc.executeUpdate();
                }

                conn.commit();
                return new AccountSummary(accountId, customerId, fullName, email, initialDeposit, AccountStatus.ACTIVE);
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public AccountSummary getAccountSummary(String accountId) throws SQLException {
        String sql = """
                SELECT a.account_id, a.customer_id, c.name, c.email, a.balance, a.status
                FROM account a
                JOIN customer c ON a.customer_id = c.customer_id
                WHERE a.account_id = ?
                """;

        try (Connection conn = DataSourceConfig.getDataSource().getConnection()) {
            conn.setReadOnly(true);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, accountId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Account not found: " + accountId);
                    }
                    return new AccountSummary(
                            rs.getString("account_id"),
                            rs.getLong("customer_id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getBigDecimal("balance"),
                            AccountStatus.valueOf(rs.getString("status"))
                    );
                }
            }
        }
    }

    public void updateAccountStatus(String accountId, AccountStatus targetStatus) throws SQLException {
        String selectSql = "SELECT balance, status FROM account WHERE account_id = ? FOR UPDATE";
        String updateSql = "UPDATE account SET status = ?, version = version + 1 WHERE account_id = ?";

        try (Connection conn = DataSourceConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal currentBalance;
                AccountStatus currentStatus;

                try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
                    psSelect.setString(1, accountId);
                    try (ResultSet rs = psSelect.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Account not found: " + accountId);
                        }
                        currentBalance = rs.getBigDecimal("balance");
                        currentStatus = AccountStatus.valueOf(rs.getString("status"));
                    }
                }

                if (currentStatus == AccountStatus.CLOSED) {
                    throw new IllegalStateException("Closed account cannot be modified: " + accountId);
                }

                if (targetStatus == AccountStatus.CLOSED && currentBalance.compareTo(BigDecimal.ZERO) > 0) {
                    throw new IllegalStateException("Cannot close account with positive balance: " + currentBalance);
                }

                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setString(1, targetStatus.name());
                    psUpdate.setString(2, accountId);
                    psUpdate.executeUpdate();
                }

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}