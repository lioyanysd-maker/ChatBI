package com.chatbi.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartServiceSummarizeTest {

    private final ChartService chartService = new ChartService();

    @Test
    void summarizeIncludesSampleRowsAndStats() {
        var rows = List.of(
                Map.<String, Object>of("month", "2024-01", "amount", 100),
                Map.<String, Object>of("month", "2024-02", "amount", 200),
                Map.<String, Object>of("month", "2024-03", "amount", 150)
        );
        Map<String, String> labels = Map.of("month", "月份", "amount", "销售额");

        String summary = chartService.summarizeRows(rows, labels);

        assertTrue(summary.contains("共 3 行"));
        assertTrue(summary.contains("2024-01"));
        assertTrue(summary.contains("销售额"));
        assertTrue(summary.contains("合计=450"));
    }
}
