package com.bank.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.Properties;

public class DataSourceConfig {
    private static HikariDataSource dataSource;

    public static synchronized DataSource getDataSource() {
        if (dataSource == null) {
            try (InputStream in = DataSourceConfig.class.getClassLoader()
                    .getResourceAsStream("application.properties")) {
                Properties props = new Properties();
                props.load(in);

                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(props.getProperty("db.url"));
                config.setUsername(props.getProperty("db.user"));
                config.setPassword(props.getProperty("db.password"));
                config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.maxSize", "20")));
                config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minIdle", "5")));
                config.setConnectionTimeout(Long.parseLong(props.getProperty("db.pool.connectionTimeoutMs", "5000")));
                config.setAutoCommit(false);

                dataSource = new HikariDataSource(config);
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize HikariCP DataSource", e);
            }
        }
        return dataSource;
    }

    public static synchronized void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}