package com.chatbi.util;

import cn.hutool.core.util.StrUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 将 SQL 结果列名转为面向业务人员的中文表头。
 */
public final class ColumnLabelResolver {

    private static final Pattern CJK = Pattern.compile("[\\u4e00-\\u9fff]");

    private static final Map<String, String> EXACT_LABELS = Map.ofEntries(
            Map.entry("id", "编号"),
            Map.entry("sale_count", "销量"),
            Map.entry("total_sales", "销售额"),
            Map.entry("total_amount", "总金额"),
            Map.entry("order_count", "订单数"),
            Map.entry("product_code", "商品编码"),
            Map.entry("product_name", "商品名称"),
            Map.entry("customer_name", "客户姓名"),
            Map.entry("customer_no", "客户编号"),
            Map.entry("pay_value", "实付金额"),
            Map.entry("discount_value", "优惠金额"),
            Map.entry("quantity", "数量"),
            Map.entry("unit_price", "单价"),
            Map.entry("line_amount", "行金额"),
            Map.entry("create_time", "创建时间"),
            Map.entry("pay_time", "支付时间"),
            Map.entry("order_status", "订单状态"),
            Map.entry("month", "月份"),
            Map.entry("category_name", "分类名称"),
            Map.entry("brand_name", "品牌名称"),
            Map.entry("city_name", "城市"),
            Map.entry("region_name", "区域"),
            Map.entry("rating", "评分"),
            Map.entry("cnt", "数量"),
            Map.entry("count", "数量"),
            Map.entry("total", "合计"),
            Map.entry("amount", "金额")
    );

    private static final Map<String, String> TOKEN_LABELS = Map.ofEntries(
            Map.entry("product", "商品"),
            Map.entry("customer", "客户"),
            Map.entry("order", "订单"),
            Map.entry("sale", "销售"),
            Map.entry("sales", "销售"),
            Map.entry("pay", "支付"),
            Map.entry("payment", "支付"),
            Map.entry("amount", "金额"),
            Map.entry("value", "金额"),
            Map.entry("price", "价格"),
            Map.entry("name", "名称"),
            Map.entry("code", "编码"),
            Map.entry("time", "时间"),
            Map.entry("date", "日期"),
            Map.entry("month", "月份"),
            Map.entry("year", "年份"),
            Map.entry("count", "数量"),
            Map.entry("total", "合计"),
            Map.entry("qty", "数量"),
            Map.entry("quantity", "数量"),
            Map.entry("status", "状态"),
            Map.entry("type", "类型"),
            Map.entry("city", "城市"),
            Map.entry("region", "区域"),
            Map.entry("brand", "品牌"),
            Map.entry("category", "分类"),
            Map.entry("voucher", "优惠券"),
            Map.entry("channel", "渠道"),
            Map.entry("campaign", "活动"),
            Map.entry("employee", "员工"),
            Map.entry("supplier", "供应商"),
            Map.entry("warehouse", "仓库"),
            Map.entry("inventory", "库存"),
            Map.entry("rating", "评分"),
            Map.entry("avg", "平均"),
            Map.entry("max", "最大"),
            Map.entry("min", "最小")
    );

    private ColumnLabelResolver() {
    }

    public static Map<String, String> resolve(List<String> columnKeys, Map<String, String> schemaCommentIndex) {
        Map<String, String> labels = new LinkedHashMap<>();
        if (columnKeys == null) {
            return labels;
        }
        for (String key : columnKeys) {
            labels.put(key, resolveOne(key, schemaCommentIndex));
        }
        return labels;
    }

    public static List<String> filterDisplayColumns(List<String> columnKeys) {
        if (columnKeys == null || columnKeys.size() <= 1) {
            return columnKeys;
        }
        boolean hasBizKey = columnKeys.stream().anyMatch(ColumnLabelResolver::isBusinessIdentifierColumn);
        if (!hasBizKey) {
            return columnKeys;
        }
        return columnKeys.stream()
                .filter(key -> !"id".equalsIgnoreCase(key))
                .toList();
    }

    private static boolean isBusinessIdentifierColumn(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.endsWith("_name") || lower.endsWith("_code") || lower.endsWith("_title")
                || lower.contains("name") || lower.contains("title");
    }

    private static String resolveOne(String key, Map<String, String> schemaCommentIndex) {
        if (StrUtil.isBlank(key)) {
            return key;
        }
        if (containsCjk(key)) {
            return key.trim();
        }
        String lower = key.toLowerCase(Locale.ROOT);
        if (schemaCommentIndex != null) {
            String comment = schemaCommentIndex.get(lower);
            if (StrUtil.isNotBlank(comment)) {
                return comment.split("[，,；;（(]")[0].trim();
            }
        }
        String exact = EXACT_LABELS.get(lower);
        if (exact != null) {
            return exact;
        }
        return heuristicLabel(lower);
    }

    private static String heuristicLabel(String lower) {
        if (lower.endsWith("_id")) {
            String base = lower.substring(0, lower.length() - 3);
            return tokenLabel(base) + "编号";
        }
        if (lower.startsWith("total_")) {
            return "总" + tokenLabel(lower.substring(6));
        }
        if (lower.startsWith("avg_")) {
            return "平均" + tokenLabel(lower.substring(4));
        }
        if (lower.endsWith("_count")) {
            return tokenLabel(lower.substring(0, lower.length() - 6)) + "数量";
        }
        if (lower.endsWith("_name")) {
            return tokenLabel(lower.substring(0, lower.length() - 5)) + "名称";
        }
        if (lower.endsWith("_code")) {
            return tokenLabel(lower.substring(0, lower.length() - 5)) + "编码";
        }
        if (lower.endsWith("_time")) {
            return tokenLabel(lower.substring(0, lower.length() - 5)) + "时间";
        }
        if (lower.endsWith("_date")) {
            return tokenLabel(lower.substring(0, lower.length() - 5)) + "日期";
        }
        if (lower.endsWith("_amount") || lower.endsWith("_value")) {
            int cut = lower.endsWith("_amount") ? 7 : 6;
            return tokenLabel(lower.substring(0, lower.length() - cut)) + "金额";
        }
        return joinTokens(lower.split("_"));
    }

    private static String tokenLabel(String token) {
        if (StrUtil.isBlank(token)) {
            return "";
        }
        return TOKEN_LABELS.getOrDefault(token, token);
    }

    private static String joinTokens(String[] parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            sb.append(tokenLabel(part));
        }
        if (sb.isEmpty()) {
            return String.join(" ", parts);
        }
        return sb.toString();
    }

    private static boolean containsCjk(String text) {
        return CJK.matcher(text).find();
    }
}
