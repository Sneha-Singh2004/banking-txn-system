package com.bank.service;

import com.bank.dto.ChannelMetric;
import com.bank.dto.FailureMetric;
import com.bank.dto.StatementItem;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportService {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ReportingService reportingService;

    public ExportService(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    public Path exportMiniStatementCsv(String accountId, int limit, Path outputFile)
            throws SQLException, IOException {
        List<StatementItem> items = reportingService.getMiniStatement(accountId, limit);
        ensureParentDirectory(outputFile);

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.write("transaction_id,type,amount,source_account,dest_account,channel,status,created_at");
            writer.newLine();

            for (StatementItem item : items) {
                writer.write(String.join(",",
                        escapeCsv(item.transactionId()),
                        escapeCsv(item.type()),
                        item.amount().toPlainString(),
                        escapeCsv(item.sourceAccount()),
                        escapeCsv(item.destAccount()),
                        escapeCsv(item.channel()),
                        escapeCsv(item.status()),
                        escapeCsv(item.createdAt().format(TS_FORMAT))
                ));
                writer.newLine();
            }
        }
        return outputFile;
    }

    public Path exportChannelSummaryCsv(Path outputFile) throws SQLException, IOException {
        List<ChannelMetric> metrics = reportingService.getChannelSummary();
        ensureParentDirectory(outputFile);

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.write("channel,transaction_count,total_volume");
            writer.newLine();

            for (ChannelMetric metric : metrics) {
                writer.write(String.join(",",
                        escapeCsv(metric.channel()),
                        String.valueOf(metric.transactionCount()),
                        metric.totalVolume().toPlainString()
                ));
                writer.newLine();
            }
        }
        return outputFile;
    }

    public Path exportFailureSummaryCsv(Path outputFile) throws SQLException, IOException {
        List<FailureMetric> failures = reportingService.getFailureSummary();
        ensureParentDirectory(outputFile);

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.write("error_category,failure_count");
            writer.newLine();

            for (FailureMetric failure : failures) {
                writer.write(String.join(",",
                        escapeCsv(failure.errorCategory()),
                        String.valueOf(failure.failureCount())
                ));
                writer.newLine();
            }
        }
        return outputFile;
    }

    private void ensureParentDirectory(Path outputFile) throws IOException {
        Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}