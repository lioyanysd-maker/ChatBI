package com.chatbi.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeParserTest {

    private final TimeParser timeParser = new TimeParser();

    @Test
    void parsesLastMonth() {
        var hint = timeParser.parseQuestion("上个月的订单总额是多少");
        assertTrue(hint.isPresent());
        assertEquals("上个月", hint.get().label());
        assertTrue(hint.get().start().compareTo(hint.get().end()) < 0);
    }

    @Test
    void parsesRecentDays() {
        var hint = timeParser.parseQuestion("近7天的销售额");
        assertTrue(hint.isPresent());
        assertEquals("近7天", hint.get().label());
    }

    @Test
    void parsesCustomRecentDays() {
        var hint = timeParser.parseQuestion("最近15天的订单数");
        assertTrue(hint.isPresent());
        assertEquals("近15天", hint.get().label());
    }

    @Test
    void returnsEmptyForPlainQuestion() {
        assertFalse(timeParser.parseQuestion("每个分类的商品数量").isPresent());
    }

    @Test
    void buildSqlTimeHintContainsRange() {
        String hint = timeParser.buildSqlTimeHint("本月新增用户");
        assertTrue(hint.contains("时间范围提示"));
        assertTrue(hint.contains(">="));
    }
}
