package com.bank.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportServiceTest {

    private static ExportService exportService;

    @BeforeAll
    static void setUp() {
        exportService = new ExportService(new ReportingService());
    }

    @Test
    @DisplayName("1. Mini-statement CSV export should write header and transaction rows")
    void testExportMiniStatementCsv(@TempDir Path tempDir) throws Exception {
        Path csvPath = tempDir.resolve("nested/acc1001_statement.csv");
        exportService.exportMiniStatementCsv("ACC1001", 5, csvPath);

        assertTrue(Files.exists(csvPath));
        List<String> lines = Files.readAllLines(csvPath);
        assertTrue(lines.size() >= 2);
        assertEquals("transaction_id,type,amount,source_account,dest_account,channel,status,created_at", lines.get(0));
    }

    @Test
    @DisplayName("2. Channel summary CSV export should write aggregated channel rows")
    void testExportChannelSummaryCsv(@TempDir Path tempDir) throws Exception {
        Path csvPath = tempDir.resolve("channel_summary.csv");
        exportService.exportChannelSummaryCsv(csvPath);

        assertTrue(Files.exists(csvPath));
        List<String> lines = Files.readAllLines(csvPath);
        assertTrue(lines.size() >= 2);
        assertEquals("channel,transaction_count,total_volume", lines.get(0));
    }

    @Test
    @DisplayName("3. Failure summary CSV export should write failure category rows")
    void testExportFailureSummaryCsv(@TempDir Path tempDir) throws Exception {
        Path csvPath = tempDir.resolve("failure_summary.csv");
        exportService.exportFailureSummaryCsv(csvPath);

        assertTrue(Files.exists(csvPath));
        List<String> lines = Files.readAllLines(csvPath);
        assertTrue(lines.size() >= 2);
        assertEquals("error_category,failure_count", lines.get(0));
    }
}