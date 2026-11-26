package com.bank.service;

import com.bank.dto.ChannelMetric;
import com.bank.dto.FailureMetric;
import com.bank.dto.StatementItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportingServiceTest {

    private static ReportingService reportingService;

    @BeforeAll
    static void setUp() {
        reportingService = new ReportingService();
    }

    @Test
    @DisplayName("1. Mini-statement should return latest transactions up to requested limit")
    void testGetMiniStatement() throws Exception {
        List<StatementItem> statement = reportingService.getMiniStatement("ACC1001", 5);

        assertFalse(statement.isEmpty());
        assertTrue(statement.size() <= 5);
        statement.forEach(item -> assertTrue(
                "ACC1001".equals(item.sourceAccount()) || "ACC1001".equals(item.destAccount())
        ));
    }

    @Test
    @DisplayName("2. Channel summary should aggregate successful transaction counts and volumes")
    void testGetChannelSummary() throws Exception {
        List<ChannelMetric> summary = reportingService.getChannelSummary();

        assertFalse(summary.isEmpty());
        for (ChannelMetric metric : summary) {
            assertTrue(metric.transactionCount() > 0);
            assertTrue(metric.totalVolume().compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Test
    @DisplayName("3. Failure summary should categorize rejected/failed transactions")
    void testGetFailureSummary() throws Exception {
        List<FailureMetric> failures = reportingService.getFailureSummary();

        assertFalse(failures.isEmpty());
        assertTrue(failures.get(0).failureCount() > 0);
    }
}