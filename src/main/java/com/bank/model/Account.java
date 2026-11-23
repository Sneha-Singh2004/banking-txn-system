package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Account {
    private String accountId;
    private long customerId;
    private BigDecimal balance;
    private AccountStatus status;
    private long version;
    private LocalDateTime createdAt;

    public Account() {}

    public Account(String accountId, long customerId, BigDecimal balance, AccountStatus status, long version, LocalDateTime createdAt) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.balance = balance;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
    }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}