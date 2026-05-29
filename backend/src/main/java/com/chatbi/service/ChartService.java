package com.chatbi.service;

import com.chatbi.dto.response.ChartData;
import com.chatbi.util.ColumnLabelResolver;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ChartService {

    private static final Pattern LISTING_QUESTION = Pattern.compile(
            ".*(哪些|有哪些|列出|罗列|显示所有|都有什么|都有那些|都有啥|包含什么|有什么|什么.*有|分别|明细|清单|列表|详情).*",
            Pattern.CASE_INSENSITIVE
    );

    public ChartData buildChartData(List<Map<String, Object>> rows, String preferredType) {
        return buildChartData(rows, preferredType, null, null);
    }

    public ChartData buildChartData(List<Map<String, Object>> rows, String preferredType, String question) {
        return buildChartData(rows, preferredType, question, null);
    }

    public ChartData buildChartData(List<Map<String, Object>> rows, String preferredType, String question,
                                    Map<String, String> columnLabels) {
        String effectiveType = inferPreferredChartType(question, preferredType);
        if (rows == null || rows.isEmpty()) {
            return ChartData.builder()
                    .type("table")
                    .columns(List.of())
                    .columnLabels(Map.of())
                    .rows(List.of())
                    .build();
        }

        List<String> columnList = extractColumns(rows);
        List<String> displayColumns = ColumnLabelResolver.filterDisplayColumns(columnList);
        Map<String, String> labels = columnLabels != null ? columnLabels : Map.of();
        Map<String, String> displayLabels = new LinkedHashMap<>();
        for (String col : displayColumns) {
            displayLabels.put(col, labels.getOrDefault(col, col));
        }

        String chartType = effectiveType != null ? effectiveType.toLowerCase() : "auto";

        if ("table".equals(chartType)) {
            return buildTable(displayColumns, rows, displayLabels);
        }

        if (columnList.size() == 1) {
            Object value = rows.get(0).get(columnList.get(0));
            return ChartData.builder()
                    .type("kpi")
                    .series(value)
                    .unit("")
                    .columns(columnList)
                    .columnLabels(labels)
                    .rows(rows)
                    .build();
        }

        if (columnList.size() >= 2) {
            if ("auto".equals(chartType) && shouldUseTable(rows, columnList, question)) {
                return buildTable(displayColumns, rows, displayLabels);
            }

            String dim = columnList.get(0);
            String metric = columnList.get(1);
            List<Object> xAxis = new ArrayList<>();
            List<Object> series = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                xAxis.add(row.get(dim));
                series.add(toNumberIfPossible(row.get(metric)));
            }
            String type = resolveChartType(chartType, rows, columnList, question);
            if ("table".equals(type)) {
                return buildTable(displayColumns, rows, displayLabels);
            }
            return ChartData.builder()
                    .type(type)
                    .xAxis(xAxis)
                    .series(series)
                    .unit("")
                    .columns(columnList)
                    .columnLabels(labels)
                    .rows(rows)
                    .build();
        }

        return buildTable(displayColumns, rows, displayLabels);
    }

    public String inferPreferredChartType(String question, String chartTypeFromRequest) {
        if (chartTypeFromRequest != null && !"auto".equalsIgnoreCase(chartTypeFromRequest)) {
            return chartTypeFromRequest.toLowerCase(Locale.ROOT);
        }
        if (question == null || question.isBlank()) {
            return "auto";
        }
        String q = question.toLowerCase(Locale.ROOT);
        if (q.contains("折线")) {
            return "line";
        }
        if (q.contains("柱状") || q.contains("条形")) {
            return "bar";
        }
        if (q.contains("饼图") || q.contains("占比图")) {
            return "pie";
        }
        if (q.contains("表格") || q.contains("明细")) {
            return "table";
        }
        return "auto";
    }

    private boolean shouldUseTable(List<Map<String, Object>> rows, List<String> columns, String question) {
        if (columns.size() > 2) {
            return true;
        }
        if (question != null && LISTING_QUESTION.matcher(question).matches()) {
            return true;
        }
        if (columns.size() == 2) {
            String col0 = columns.get(0);
            String col1 = columns.get(1);
            boolean col0Numeric = isMostlyNumeric(rows, col0);
            boolean col1Numeric = isMostlyNumeric(rows, col1);
            // 两列都是文本 → 明细列表
            if (!col0Numeric && !col1Numeric) {
                return true;
            }
            // 第二列不是数值 → 不适合柱状/饼图
            if (!col1Numeric) {
                return true;
            }
            // 第一列是 ID 类数值 → 明细
            if (col0Numeric && looksLikeIdColumn(rows, col0)) {
                return true;
            }
            // 维度列几乎全唯一 → 明细而非聚合
            if (!col0Numeric && uniqueRatio(rows, col0) > 0.85) {
                return true;
            }
        }
        return false;
    }

    private String resolveChartType(String preferredType, List<Map<String, Object>> rows,
                                    List<String> columns, String question) {
        if ("auto".equals(preferredType)) {
            if (shouldUseTable(rows, columns, question)) {
                return "table";
            }
            int rowCount = rows.size();
            if (rowCount > 12) {
                return "bar";
            }
            return rowCount <= 6 ? "pie" : "bar";
        }
        if ("pie".equals(preferredType) || "bar".equals(preferredType) || "line".equals(preferredType)) {
            return columns.size() >= 2 ? preferredType : "table";
        }
        return "table";
    }

    private boolean looksLikeIdColumn(List<Map<String, Object>> rows, String column) {
        String lower = column.toLowerCase();
        if (lower.equals("id") || lower.endsWith("_id")) {
            return true;
        }
        return isMostlyNumeric(rows, column) && uniqueRatio(rows, column) > 0.9;
    }

    private double uniqueRatio(List<Map<String, Object>> rows, String column) {
        Set<Object> unique = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            unique.add(row.get(column));
        }
        return rows.isEmpty() ? 0 : (double) unique.size() / rows.size();
    }

    private boolean isMostlyNumeric(List<Map<String, Object>> rows, String column) {
        int numeric = 0;
        int total = 0;
        for (Map<String, Object> row : rows) {
            Object value = row.get(column);
            if (value == null) {
                continue;
            }
            total++;
            if (value instanceof Number || isNumericString(value.toString())) {
                numeric++;
            }
        }
        return total > 0 && numeric >= total * 0.8;
    }

    private boolean isNumericString(String text) {
        try {
            Double.parseDouble(text);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private ChartData buildTable(List<String> columnList, List<Map<String, Object>> rows,
                                 Map<String, String> columnLabels) {
        return ChartData.builder()
                .type("table")
                .columns(columnList)
                .columnLabels(columnLabels)
                .rows(rows)
                .build();
    }

    private List<String> extractColumns(List<Map<String, Object>> rows) {
        Set<String> columns = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            columns.addAll(row.keySet());
        }
        return new ArrayList<>(columns);
    }

    private Object toNumberIfPossible(Object value) {
        if (value instanceof Number) {
            return value;
        }
        if (value == null) {
            return 0;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return value;
        }
    }

    public String summarizeRows(List<Map<String, Object>> rows) {
        return summarizeRows(rows, Map.of());
    }

    public String summarizeRows(List<Map<String, Object>> rows, Map<String, String> columnLabels) {
        if (rows == null || rows.isEmpty()) {
            return "无数据";
        }
        if (rows.size() == 1 && rows.get(0).size() == 1) {
            Object value = rows.get(0).values().iterator().next();
            return "结果值：" + value;
        }

        List<String> columns = ColumnLabelResolver.filterDisplayColumns(extractColumns(rows));
        Map<String, String> labels = columnLabels != null ? columnLabels : Map.of();

        StringBuilder sb = new StringBuilder();
        sb.append("共 ").append(rows.size()).append(" 行");

        appendNumericStats(sb, rows, columns, labels);

        int sampleSize = Math.min(10, rows.size());
        sb.append("\n\n前 ").append(sampleSize).append(" 行数据：\n");
        for (int i = 0; i < sampleSize; i++) {
            sb.append(i + 1).append(". ");
            sb.append(formatRowSample(rows.get(i), columns, labels));
            sb.append('\n');
        }
        if (rows.size() > sampleSize) {
            sb.append("... 其余 ").append(rows.size() - sampleSize).append(" 行未列出");
        }

        String summary = sb.toString().trim();
        if (summary.length() > 3000) {
            return summary.substring(0, 3000) + "...";
        }
        return summary;
    }

    private void appendNumericStats(StringBuilder sb, List<Map<String, Object>> rows,
                                    List<String> columns, Map<String, String> labels) {
        boolean hasStats = false;
        StringBuilder stats = new StringBuilder();
        for (String column : columns) {
            if (!isMostlyNumeric(rows, column)) {
                continue;
            }
            double sum = 0;
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            int count = 0;
            for (Map<String, Object> row : rows) {
                Object raw = row.get(column);
                if (raw == null) {
                    continue;
                }
                try {
                    double value = raw instanceof Number n ? n.doubleValue() : Double.parseDouble(raw.toString());
                    sum += value;
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                    count++;
                } catch (NumberFormatException ignored) {
                    // skip non-numeric cell
                }
            }
            if (count == 0) {
                continue;
            }
            if (!hasStats) {
                stats.append("\n\n数值统计：\n");
                hasStats = true;
            }
            String header = labels.getOrDefault(column, column);
            stats.append("- ").append(header)
                    .append("：合计=").append(formatNumber(sum))
                    .append("，最小=").append(formatNumber(min))
                    .append("，最大=").append(formatNumber(max))
                    .append('\n');
        }
        sb.append(stats);
    }

    private String formatRowSample(Map<String, Object> row, List<String> columns, Map<String, String> labels) {
        List<String> parts = new ArrayList<>();
        for (String column : columns) {
            Object value = row.get(column);
            if (value == null) {
                continue;
            }
            parts.add(labels.getOrDefault(column, column) + "=" + formatCellValue(value));
        }
        return String.join("，", parts);
    }

    private String formatCellValue(Object value) {
        if (value instanceof Number number) {
            return formatNumber(number.doubleValue());
        }
        String text = value.toString();
        if (text.length() > 40) {
            return text.substring(0, 37) + "...";
        }
        return text;
    }

    private String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.000001) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
