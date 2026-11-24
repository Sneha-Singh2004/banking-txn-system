package com.bank.simulation;

import com.bank.config.DataSourceConfig;
import com.bank.dto.TransactionRequest;
import com.bank.dto.TransactionResult;
import com.bank.model.TransactionStatus;
import com.bank.model.TransactionType;
import com.bank.repository.AccountRepository;
import com.bank.service.BankingService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

public class TransactionSimulator {

    private static final int TOTAL_TRANSFERS = 50_000;
    private static final int THREAD_POOL_SIZE = 20;

    private final BankingService bankingService;
    private final AccountRepository accountRepo;

    public TransactionSimulator() {
        this.bankingService = new BankingService();
        this.accountRepo = new AccountRepository();
    }

    public void runVerificationSuite() throws Exception {
        System.out.println("=== 1. Functional & Edge-Case Verification ===");
        String runTag = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        TransactionResult dep = bankingService.processTransaction(
                new TransactionRequest("TXN-" + runTag + "-1", TransactionType.DEPOSIT,
                        new BigDecimal("5000.00"), null, "ACC1001", "WEB"));
        System.out.println("Deposit Result: " + dep);

        TransactionResult wd = bankingService.processTransaction(
                new TransactionRequest("TXN-" + runTag + "-2", TransactionType.WITHDRAWAL,
                        new BigDecimal("2000.00"), "ACC1001", null, "ATM"));
        System.out.println("Withdrawal Result: " + wd);

        TransactionResult dup = bankingService.processTransaction(
                new TransactionRequest("TXN-" + runTag + "-2", TransactionType.WITHDRAWAL,
                        new BigDecimal("2000.00"), "ACC1001", null, "ATM"));
        System.out.println("Duplicate Txn Result: " + dup);

        TransactionResult lowBal = bankingService.processTransaction(
                new TransactionRequest("TXN-" + runTag + "-3", TransactionType.WITHDRAWAL,
                        new BigDecimal("9999999.00"), "ACC1002", null, "UPI"));
        System.out.println("Insufficient Funds Result: " + lowBal);

        System.out.printf("%n=== 2. High-Volume Concurrency Benchmark (%d Transfers, %d Threads) ===%n",
                TOTAL_TRANSFERS, THREAD_POOL_SIZE);
        BigDecimal beforeTotal = getCombinedBalance("ACC1001", "ACC1002");
        System.out.println("Combined Balance Before (ACC1001 + ACC1002): " + beforeTotal);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Callable<TransactionResult>> tasks = new ArrayList<>(TOTAL_TRANSFERS);

        for (int i = 1; i <= TOTAL_TRANSFERS; i++) {
            final int idx = i;
            boolean forward = (i % 2 == 0);
            String src = forward ? "ACC1001" : "ACC1002";
            String dst = forward ? "ACC1002" : "ACC1001";

            tasks.add(() -> bankingService.processTransaction(
                    new TransactionRequest("TXN-" + runTag + "-CONC-" + idx,
                            TransactionType.TRANSFER, new BigDecimal("10.00"), src, dst, "UPI")
            ));
        }

        long start = System.currentTimeMillis();
        List<Future<TransactionResult>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        int successCount = 0;
        for (Future<TransactionResult> f : futures) {
            if (f.get().getStatus() == TransactionStatus.SUCCESS) {
                successCount++;
            }
        }
        long elapsedMs = System.currentTimeMillis() - start;
        double throughputTps = (successCount * 1000.0) / Math.max(elapsedMs, 1);

        BigDecimal afterTotal = getCombinedBalance("ACC1001", "ACC1002");
        System.out.printf("Completed %d/%d concurrent transfers in %d ms (%.2f TPS)%n",
                successCount, TOTAL_TRANSFERS, elapsedMs, throughputTps);
        System.out.println("Combined Balance After  (ACC1001 + ACC1002): " + afterTotal);
        System.out.println("Money Conservation Verified: " + (beforeTotal.compareTo(afterTotal) == 0));

        printDatabaseSummary();
    }

    private BigDecimal getCombinedBalance(String acc1, String acc2) throws Exception {
        try (Connection conn = DataSourceConfig.getDataSource().getConnection()) {
            BigDecimal b1 = accountRepo.findById(conn, acc1).orElseThrow().getBalance();
            BigDecimal b2 = accountRepo.findById(conn, acc2).orElseThrow().getBalance();
            return b1.add(b2);
        }
    }

    private void printDatabaseSummary() throws Exception {
        System.out.println("\n=== 3. Database Audit & Failure Summary ===");
        try (Connection conn = DataSourceConfig.getDataSource().getConnection();
             PreparedStatement ps1 = conn.prepareStatement("SELECT COUNT(*) FROM transaction_audit");
             PreparedStatement ps2 = conn.prepareStatement("SELECT COUNT(*) FROM failed_transaction");
             ResultSet rs1 = ps1.executeQuery();
             ResultSet rs2 = ps2.executeQuery()) {
            if (rs1.next()) System.out.println("Total Audit Log Entries: " + rs1.getLong(1));
            if (rs2.next()) System.out.println("Total Failed Txn Entries: " + rs2.getLong(1));
        }
    }
}