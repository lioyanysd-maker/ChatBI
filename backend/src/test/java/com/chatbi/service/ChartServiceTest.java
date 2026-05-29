package com.chatbi.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChartServiceTest {

    private final ChartService chartService = new ChartService();

    @Test
    void infersLineChartFromQuestion() {
        assertEquals("line", chartService.inferPreferredChartType("画一个折线图", "auto"));
    }

    @Test
    void buildsLineChartForTwoColumns() {
        var rows = List.of(
                Map.<String, Object>of("month", "2024-01", "amount", 100),
                Map.<String, Object>of("month", "2024-02", "amount", 200)
        );
        var chart = chartService.buildChartData(rows, "line", "月度销售");
        assertEquals("line", chart.getType());
    }

    @Test
    void buildsTableForListingQuestion() {
        var rows = List.of(
                Map.<String, Object>of("name", "A", "category", "手机"),
                Map.<String, Object>of("name", "B", "category", "电脑")
        );
        var chart = chartService.buildChartData(rows, "auto", "有哪些商品？");
        assertEquals("table", chart.getType());
    }
}
