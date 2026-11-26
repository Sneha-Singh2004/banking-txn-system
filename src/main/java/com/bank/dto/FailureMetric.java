package com.bank.dto;

public record FailureMetric(
        String errorCategory,
        long failureCount
) {
}