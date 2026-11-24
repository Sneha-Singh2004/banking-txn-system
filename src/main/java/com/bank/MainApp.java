package com.bank;

import com.bank.config.DataSourceConfig;
import com.bank.simulation.TransactionSimulator;

public class MainApp {
    public static void main(String[] args) {
        try {
            new TransactionSimulator().runVerificationSuite();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DataSourceConfig.close();
        }
    }
}