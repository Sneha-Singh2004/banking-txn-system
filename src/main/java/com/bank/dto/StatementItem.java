package com.bank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StatementItem(
        String transactionId,
        String type,
        BigDecimal amount,
        String sourceAccount,
        String destAccount,
        String channel,
        String status,
        LocalDateTime createdAt
) {
}