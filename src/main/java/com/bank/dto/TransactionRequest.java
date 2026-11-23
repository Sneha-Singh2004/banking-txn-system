package com.bank.dto;

import com.bank.model.TransactionType;
import java.math.BigDecimal;

public class TransactionRequest {
    private final String transactionId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final String sourceAccount;
    private final String destAccount;
    private final String channel;

    public TransactionRequest(String transactionId, TransactionType type, BigDecimal amount,
                              String sourceAccount, String destAccount, String channel) {
        this.transactionId = transactionId;
        this.type = type;
        this.amount = amount;
        this.sourceAccount = sourceAccount;
        this.destAccount = destAccount;
        this.channel = channel;
    }

    public String getTransactionId() { return transactionId; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getSourceAccount() { return sourceAccount; }
    public String getDestAccount() { return destAccount; }
    public String getChannel() { return channel; }
}