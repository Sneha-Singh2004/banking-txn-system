package com.bank.service;

import com.bank.config.DataSourceConfig;
import com.bank.dto.TransactionRequest;
import com.bank.dto.TransactionResult;
import com.bank.exception.*;
import com.bank.model.*;
import com.bank.repository.AccountRepository;
import com.bank.repository.AuditRepository;
import com.bank.repository.TransactionRepository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

public class BankingService {

    private static final int MAX_RETRIES = 3;
    private final DataSource dataSource;
    private final AccountRepository accountRepo;
    private final TransactionRepository txnRepo;
    private final AuditRepository auditRepo;

    public BankingService() {
        this(DataSourceConfig.getDataSource(), new AccountRepository(), new TransactionRepository(), new AuditRepository());
    }

    public BankingService(DataSource dataSource, AccountRepository accountRepo,
                          TransactionRepository txnRepo, AuditRepository auditRepo) {
        this.dataSource = dataSource;
        this.accountRepo = accountRepo;
        this.txnRepo = txnRepo;
        this.auditRepo = auditRepo;
    }

    public TransactionResult processTransaction(TransactionRequest req) {
        long startTime = System.currentTimeMillis();
        validateRequest(req);

        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return executeAtomic(req, startTime, attempt);
            } catch (SQLException e) {
                // 40P01 = PostgreSQL Deadlock Detected, 40001 = Serialization Failure
                if (("40P01".equals(e.getSQLState()) || "40001".equals(e.getSQLState())) && attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(10L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BankingException("INTERRUPTED", "Transaction interrupted during retry");
                    }
                    continue;
                }
                int duration = (int) (System.currentTimeMillis() - startTime);
                recordFailureSafely(req, "DB_ERROR", e.getMessage(), duration);
                return new TransactionResult(req.getTransactionId(), TransactionStatus.FAILED, null, "Database error: " + e.getMessage());
            } catch (BankingException be) {
                int duration = (int) (System.currentTimeMillis() - startTime);
                recordFailureSafely(req, be.getErrorCategory(), be.getMessage(), duration);
                return new TransactionResult(req.getTransactionId(), TransactionStatus.REJECTED, null, be.getMessage());
            }
        }
    }

    private TransactionResult executeAtomic(TransactionRequest req, long startTime, int attempt) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            try {
                if (txnRepo.existsById(conn, req.getTransactionId())) {
                    throw new DuplicateTransactionException("Duplicate transaction ID: " + req.getTransactionId());
                }

                Transaction txn = new Transaction(
                        req.getTransactionId(),
                        req.getType(),
                        req.getAmount(),
                        req.getSourceAccount(),
                        req.getDestAccount(),
                        req.getChannel(),
                        TransactionStatus.PENDING,
                        null
                );
                txnRepo.insert(conn, txn);

                BigDecimal updatedBalance = switch (req.getType()) {
                    case DEPOSIT -> handleDeposit(conn, req);
                    case WITHDRAWAL -> handleWithdrawal(conn, req);
                    case TRANSFER -> handleTransfer(conn, req);
                };

                txnRepo.updateStatus(conn, req.getTransactionId(), TransactionStatus.SUCCESS, "Completed successfully");
                int duration = (int) (System.currentTimeMillis() - startTime);
                auditRepo.logAudit(conn, req.getTransactionId(), req.getType().name(), "SUCCESS", duration, "Attempt=" + attempt);

                conn.commit();
                return new TransactionResult(req.getTransactionId(), TransactionStatus.SUCCESS, updatedBalance, "Success");
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    private BigDecimal handleDeposit(Connection conn, TransactionRequest req) throws SQLException {
        Account dest = accountRepo.findByIdForUpdate(conn, req.getDestAccount())
                .orElseThrow(() -> new AccountNotFoundException("Destination account not found: " + req.getDestAccount()));
        ensureActive(dest);

        BigDecimal newBalance = dest.getBalance().add(req.getAmount());
        boolean updated = accountRepo.updateBalance(conn, dest.getAccountId(), newBalance, dest.getVersion());
        if (!updated) {
            throw new BankingException("OPTIMISTIC_LOCK_CONFLICT", "Concurrent update conflict on " + dest.getAccountId());
        }
        return newBalance;
    }

    private BigDecimal handleWithdrawal(Connection conn, TransactionRequest req) throws SQLException {
        Account source = accountRepo.findByIdForUpdate(conn, req.getSourceAccount())
                .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + req.getSourceAccount()));
        ensureActive(source);

        if (source.getBalance().compareTo(req.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds in " + source.getAccountId());
        }

        BigDecimal newBalance = source.getBalance().subtract(req.getAmount());
        boolean updated = accountRepo.updateBalance(conn, source.getAccountId(), newBalance, source.getVersion());
        if (!updated) {
            throw new BankingException("OPTIMISTIC_LOCK_CONFLICT", "Concurrent update conflict on " + source.getAccountId());
        }
        return newBalance;
    }

    private BigDecimal handleTransfer(Connection conn, TransactionRequest req) throws SQLException {
        String srcId = req.getSourceAccount();
        String dstId = req.getDestAccount();

        // Canonical Lock Ordering to prevent Deadlocks
        String firstLockId = srcId.compareTo(dstId) < 0 ? srcId : dstId;
        String secondLockId = srcId.compareTo(dstId) < 0 ? dstId : srcId;

        Account first = accountRepo.findByIdForUpdate(conn, firstLockId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + firstLockId));
        Account second = accountRepo.findByIdForUpdate(conn, secondLockId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + secondLockId));

        Account source = first.getAccountId().equals(srcId) ? first : second;
        Account dest = first.getAccountId().equals(dstId) ? first : second;

        ensureActive(source);
        ensureActive(dest);

        if (source.getBalance().compareTo(req.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds in " + source.getAccountId());
        }

        BigDecimal newSourceBal = source.getBalance().subtract(req.getAmount());
        BigDecimal newDestBal = dest.getBalance().add(req.getAmount());

        boolean srcUpdated = accountRepo.updateBalance(conn, source.getAccountId(), newSourceBal, source.getVersion());
        boolean dstUpdated = accountRepo.updateBalance(conn, dest.getAccountId(), newDestBal, dest.getVersion());

        if (!srcUpdated || !dstUpdated) {
            throw new BankingException("OPTIMISTIC_LOCK_CONFLICT", "Concurrent transfer conflict");
        }
        return newSourceBal;
    }

    private void ensureActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountStateException("Account " + account.getAccountId() + " is " + account.getStatus());
        }
    }

    private void validateRequest(TransactionRequest req) {
        if (req == null || req.getTransactionId() == null || req.getType() == null) {
            throw new InvalidAmountException("Transaction ID and type are required");
        }
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Transaction amount must be greater than zero");
        }
        if (req.getType() == TransactionType.TRANSFER &&
                req.getSourceAccount() != null && req.getSourceAccount().equals(req.getDestAccount())) {
            throw new InvalidAmountException("Source and destination accounts cannot be the same");
        }
    }

    private void recordFailureSafely(TransactionRequest req, String category, String reason, int durationMs) {
        try (Connection conn = dataSource.getConnection()) {
            try {
                if (!txnRepo.existsById(conn, req.getTransactionId())) {
                    Transaction failedTxn = new Transaction(
                            req.getTransactionId(), req.getType(), req.getAmount(),
                            req.getSourceAccount(), req.getDestAccount(), req.getChannel(),
                            TransactionStatus.FAILED, reason
                    );
                    txnRepo.insert(conn, failedTxn);
                }
                auditRepo.logFailure(conn, req.getTransactionId(), category, reason);
                auditRepo.logAudit(conn, req.getTransactionId(), req.getType().name(), "FAILED", durationMs, reason);
                conn.commit();
            } catch (Exception ignored) {
                conn.rollback();
            }
        } catch (SQLException ignored) {
        }
    }
}