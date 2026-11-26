package com.bank.service;

import com.bank.config.DataSourceConfig;
import com.bank.dto.TransactionRequest;
import com.bank.dto.TransactionResult;
import com.bank.exception.InvalidAmountException;
import com.bank.model.TransactionStatus;
import com.bank.model.TransactionType;
import com.bank.repository.AccountRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankingServiceTest {

    private static BankingService bankingService;
    private static AccountRepository accountRepo;
    private static String runTag;

    @BeforeAll
    static void setUpSuite() {
        bankingService = new BankingService();
        accountRepo = new AccountRepository();
        runTag = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BigDecimal fetchBalance(String accountId) throws Exception {
        try (Connection conn = DataSourceConfig.getDataSource().getConnection()) {
            return accountRepo.findById(conn, accountId)
                    .orElseThrow(() -> new AssertionError("Account not found: " + accountId))
                    .getBalance();
        }
    }

    @Test
    @DisplayName("1. Valid deposit should credit account balance and return SUCCESS")
    void testDepositIncreasesBalance() throws Exception {
        BigDecimal initialBalance = fetchBalance("ACC1003");
        BigDecimal depositAmount = new BigDecimal("1500.00");

        TransactionResult result = bankingService.processTransaction(
                new TransactionRequest("JUNIT-" + runTag + "-DEP", TransactionType.DEPOSIT,
                        depositAmount, null, "ACC1003", "WEB")
        );

        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        BigDecimal updatedBalance = fetchBalance("ACC1003");
        assertEquals(0, initialBalance.add(depositAmount).compareTo(updatedBalance));
    }

    @Test
    @DisplayName("2. Withdrawal exceeding balance must rollback and preserve original balance")
    void testInsufficientFundsRollback() throws Exception {
        BigDecimal initialBalance = fetchBalance("ACC1004");
        BigDecimal excessiveAmount = new BigDecimal("99999999.00");

        TransactionResult result = bankingService.processTransaction(
                new TransactionRequest("JUNIT-" + runTag + "-INSUFF", TransactionType.WITHDRAWAL,
                        excessiveAmount, "ACC1004", null, "ATM")
        );

        assertEquals(TransactionStatus.REJECTED, result.getStatus());
        assertTrue(result.getReason().contains("Insufficient funds"));

        BigDecimal afterBalance = fetchBalance("ACC1004");
        assertEquals(0, initialBalance.compareTo(afterBalance));
    }

    @Test
    @DisplayName("3. Duplicate transaction ID must be rejected for idempotency")
    void testDuplicateTransactionIdRejected() throws Exception {
        String txnId = "JUNIT-" + runTag + "-DUP";

        TransactionResult first = bankingService.processTransaction(
                new TransactionRequest(txnId, TransactionType.DEPOSIT,
                        new BigDecimal("200.00"), null, "ACC1003", "UPI")
        );
        assertEquals(TransactionStatus.SUCCESS, first.getStatus());

        BigDecimal balanceAfterFirst = fetchBalance("ACC1003");

        TransactionResult duplicate = bankingService.processTransaction(
                new TransactionRequest(txnId, TransactionType.DEPOSIT,
                        new BigDecimal("200.00"), null, "ACC1003", "UPI")
        );
        assertEquals(TransactionStatus.REJECTED, duplicate.getStatus());
        assertTrue(duplicate.getReason().contains("Duplicate transaction ID"));
        assertEquals(0, balanceAfterFirst.compareTo(fetchBalance("ACC1003")));
    }

    @Test
    @DisplayName("4. Zero or negative transaction amount must throw InvalidAmountException")
    void testInvalidAmountRejected() {
        assertThrows(InvalidAmountException.class, () -> bankingService.processTransaction(
                new TransactionRequest("JUNIT-" + runTag + "-ZERO", TransactionType.DEPOSIT,
                        BigDecimal.ZERO, null, "ACC1003", "WEB")
        ));

        assertThrows(InvalidAmountException.class, () -> bankingService.processTransaction(
                new TransactionRequest("JUNIT-" + runTag + "-NEG", TransactionType.WITHDRAWAL,
                        new BigDecimal("-100.00"), "ACC1003", null, "ATM")
        ));
    }

    @Test
    @DisplayName("5. Concurrent bidirectional transfers must not deadlock and must conserve money")
    void testConcurrentBidirectionalTransfersNoDeadlock() throws Exception {
        int totalTransfers = 100;
        int threads = 20;

        BigDecimal beforeTotal = fetchBalance("ACC1003").add(fetchBalance("ACC1004"));

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(totalTransfers);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<TransactionResult>> futures = new ArrayList<>(totalTransfers);

        for (int i = 1; i <= totalTransfers; i++) {
            final int idx = i;
            boolean forward = (i % 2 == 0);
            String src = forward ? "ACC1003" : "ACC1004";
            String dst = forward ? "ACC1004" : "ACC1003";

            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                return bankingService.processTransaction(
                        new TransactionRequest("JUNIT-" + runTag + "-CONC-" + idx,
                                TransactionType.TRANSFER, new BigDecimal("5.00"), src, dst, "UPI")
                );
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        int successCount = 0;
        for (Future<TransactionResult> f : futures) {
            TransactionResult res = f.get(15, TimeUnit.SECONDS);
            if (res.getStatus() == TransactionStatus.SUCCESS) {
                successCount++;
            }
        }
        executor.shutdown();

        assertEquals(totalTransfers, successCount);

        BigDecimal afterTotal = fetchBalance("ACC1003").add(fetchBalance("ACC1004"));
        assertEquals(0, beforeTotal.compareTo(afterTotal));
    }
}