package com.chatbi.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class QuestionIntentClassifier {

    private static final Pattern CHART_FOLLOW_UP = Pattern.compile(
            ".*(折线图|柱状图|条形图|饼图|占比图|图表|可视化|画图|绘制|画一个|画个|展示.*图|改成.*图|换.*图|用.*图).*",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CONTEXT_FOLLOW_UP = Pattern.compile(
            ".*(上面|上一个|刚才|之前|这笔|继续|同上|同样|在此基础上|基于.*(结果|回答|上面|上面)).*",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern[] DESCRIPTIVE_PATTERNS = {
            Pattern.compile(".*(存储|保存|存放).*(什么|哪些|啥|何种).*"),
            Pattern.compile(".*主要.*(信息|内容|数据|存储|用途).*"),
            Pattern.compile(".*(介绍|描述|说明|解释|概述|总结).*(数据库|数据|表|字段|结构).*"),
            Pattern.compile(".*(数据库|这个库|该库|数据源).*(是|做|干|用途|功能|含义|干什么的).*"),
            Pattern.compile(".*(有哪些|有什么|包含哪些).*(表|字段|数据类型).*"),
            Pattern.compile(".*(表结构|字段含义|字段说明|schema|Schema).*"),
            Pattern.compile(".*帮我了解.*"),
            Pattern.compile(".*能查什么.*"),
            Pattern.compile(".*可以问什么.*"),
    };

    private static final Pattern[] SQL_PATTERNS = {
            Pattern.compile(".*(多少|几个|数量|总数|总额|合计|平均|最高|最低|最大|最小).*"),
            Pattern.compile(".*(统计|查询|列出|显示|找出|排序|分组|排名).*"),
            Pattern.compile(".*(SELECT|select|COUNT|SUM|AVG|GROUP BY).*"),
            Pattern.compile(".*(上个月|上月|本月|去年|最近|Top|TOP).*"),
            Pattern.compile(".*(大于|小于|等于|超过|低于).*"),
            Pattern.compile(".*(折线图|柱状图|条形图|饼图|图表|可视化|画图|绘制).*"),
            Pattern.compile(".*(更详细|再查|再看|进一步|深入).*"),
    };

    public enum Intent {
        DESCRIPTIVE,
        SQL_QUERY
    }

    public Intent classify(String question) {
        if (question == null || question.isBlank()) {
            return Intent.SQL_QUERY;
        }
        String q = question.trim();

        boolean descriptive = matchesAny(q, DESCRIPTIVE_PATTERNS);
        boolean sqlLike = matchesAny(q, SQL_PATTERNS);

        if (descriptive && !sqlLike) {
            return Intent.DESCRIPTIVE;
        }
        if (sqlLike) {
            return Intent.SQL_QUERY;
        }
        if (descriptive) {
            return Intent.DESCRIPTIVE;
        }
        return Intent.SQL_QUERY;
    }

    /** 仅更换图表展示方式，可复用上一轮 SQL */
    public boolean isChartOnlyFollowUp(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        String q = question.trim();
        if (!CHART_FOLLOW_UP.matcher(q).matches()) {
            return false;
        }
        return q.length() <= 24;
    }

    /** 短追问，需结合上一轮问题理解 */
    public boolean isContextFollowUp(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        String q = question.trim();
        if (isChartOnlyFollowUp(q)) {
            return true;
        }
        if (CONTEXT_FOLLOW_UP.matcher(q).matches()) {
            return true;
        }
        return q.length() <= 20 && matchesAny(q, SQL_PATTERNS);
    }

    private boolean matchesAny(String text, Pattern[] patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(text).matches()) {
                return true;
            }
        }
        return false;
    }
}
