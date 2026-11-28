package com.bank.dto;

import com.bank.model.AccountStatus;

import java.math.BigDecimal;

public record AccountSummary(
        String accountId,
        long customerId,
        String customerName,
        String email,
        BigDecimal balance,
        AccountStatus status
) {
}