package com.chatbi.util;

import cn.hutool.core.date.DateUtil;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从自然语言问题中解析相对时间范围，生成供 SQL Prompt 使用的提示。
 */
@Component
public class TimeParser {

    private static final Pattern RECENT_DAYS = Pattern.compile("(?:近|最近)\\s*(\\d+)\\s*天");

    public Optional<TimeHint> parseQuestion(String question) {
        if (question == null || question.isBlank()) {
            return Optional.empty();
        }
        String text = question.trim();

        if (text.contains("上个月") || text.contains("上月")) {
            return Optional.of(range("上个月", beginOfLastMonth(), beginOfThisMonth()));
        }
        if (text.contains("本月") || text.contains("这个月") || text.contains("当月")) {
            return Optional.of(range("本月", beginOfThisMonth(), beginOfNextMonth()));
        }
        if (text.contains("昨天")) {
            Date today = beginOfToday();
            return Optional.of(range("昨天", DateUtil.offsetDay(today, -1), today));
        }
        if (text.contains("今天") || text.contains("当日")) {
            return Optional.of(range("今天", beginOfToday(), beginOfTomorrow()));
        }
        if (text.contains("上周")) {
            Date thisWeek = DateUtil.beginOfWeek(new Date());
            return Optional.of(range("上周", DateUtil.offsetWeek(thisWeek, -1), thisWeek));
        }
        if (text.contains("本周") || text.contains("这周")) {
            Date now = new Date();
            return Optional.of(range("本周", DateUtil.beginOfWeek(now), DateUtil.offsetWeek(DateUtil.beginOfWeek(now), 1)));
        }
        if (text.contains("去年")) {
            Date now = new Date();
            Date begin = DateUtil.beginOfYear(DateUtil.offsetMonth(now, -12));
            Date end = DateUtil.beginOfYear(now);
            return Optional.of(range("去年", begin, end));
        }
        if (text.contains("去年同期")) {
            Date now = new Date();
            Date begin = DateUtil.beginOfMonth(DateUtil.offsetMonth(now, -12));
            Date end = DateUtil.beginOfMonth(DateUtil.offsetMonth(now, -11));
            return Optional.of(range("去年同期（同月）", begin, end));
        }

        Matcher matcher = RECENT_DAYS.matcher(text);
        if (matcher.find()) {
            int days = Integer.parseInt(matcher.group(1));
            Date end = beginOfTomorrow();
            Date start = DateUtil.offsetDay(end, -days);
            return Optional.of(range("近" + days + "天", start, end));
        }
        if (text.contains("近7天") || text.contains("最近7天") || text.contains("近七天")) {
            Date end = beginOfTomorrow();
            return Optional.of(range("近7天", DateUtil.offsetDay(end, -7), end));
        }
        if (text.contains("近30天") || text.contains("最近30天") || text.contains("近一个月")
                || text.contains("最近一个月")) {
            Date end = beginOfTomorrow();
            return Optional.of(range("近30天", DateUtil.offsetDay(end, -30), end));
        }

        return Optional.empty();
    }

    public String buildSqlTimeHint(String question) {
        return parseQuestion(question)
                .map(TimeHint::toPromptLine)
                .orElse("");
    }

    private TimeHint range(String label, Date start, Date end) {
        return new TimeHint(
                label,
                DateUtil.formatDateTime(start),
                DateUtil.formatDateTime(end)
        );
    }

    private Date beginOfToday() {
        return DateUtil.beginOfDay(new Date());
    }

    private Date beginOfTomorrow() {
        return DateUtil.beginOfDay(DateUtil.offsetDay(new Date(), 1));
    }

    private Date beginOfThisMonth() {
        return DateUtil.beginOfMonth(new Date());
    }

    private Date beginOfNextMonth() {
        return DateUtil.beginOfMonth(DateUtil.offsetMonth(new Date(), 1));
    }

    private Date beginOfLastMonth() {
        return DateUtil.beginOfMonth(DateUtil.offsetMonth(new Date(), -1));
    }

    public record TimeHint(String label, String start, String end) {
        public String toPromptLine() {
            return "时间范围提示（「" + label + "」）：建议使用 create_time / pay_time / order_time 等时间字段，"
                    + "条件为 >= '" + start + "' AND < '" + end + "'";
        }
    }
}
