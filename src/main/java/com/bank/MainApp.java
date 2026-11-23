package com.bank;

import com.bank.config.DataSourceConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MainApp {
    public static void main(String[] args) {
        try (Connection conn = DataSourceConfig.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT account_id, balance, status FROM account WHERE account_id = ?")) {

            ps.setString(1, "ACC1001");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.printf("Connected! Account=%s | Balance=%s | Status=%s%n",
                            rs.getString("account_id"),
                            rs.getBigDecimal("balance"),
                            rs.getString("status"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DataSourceConfig.close();
        }
    }
}