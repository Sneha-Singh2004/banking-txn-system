package com.bank.dto;

import com.bank.model.TransactionStatus;
import java.math.BigDecimal;

public class TransactionResult {
    private final String transactionId;
    private final TransactionStatus status;
    private final BigDecimal updatedBalance;
    private final String reason;

    public TransactionResult(String transactionId, TransactionStatus status, BigDecimal updatedBalance, String reason) {
        this.transactionId = transactionId;
        this.status = status;
        this.updatedBalance = updatedBalance;
        this.reason = reason;
    }

    public String getTransactionId() { return transactionId; }
    public TransactionStatus getStatus() { return status; }
    public BigDecimal getUpdatedBalance() { return updatedBalance; }
    public String getReason() { return reason; }

    @Override
    public String toString() {
        return String.format("TransactionResult{id='%s', status=%s, balance=%s, reason='%s'}",
                transactionId, status, updatedBalance, reason);
    }
}