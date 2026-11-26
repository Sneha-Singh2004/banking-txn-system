package com.bank.dto;

import java.math.BigDecimal;

public record ChannelMetric(
        String channel,
        long transactionCount,
        BigDecimal totalVolume
) {
}