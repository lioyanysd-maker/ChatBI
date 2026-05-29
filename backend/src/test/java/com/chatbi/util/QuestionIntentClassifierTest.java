package com.chatbi.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionIntentClassifierTest {

    private final QuestionIntentClassifier classifier = new QuestionIntentClassifier();

    @Test
    void classifiesSchemaQuestionAsDescriptive() {
        assertTrue(classifier.classify("这个数据库主要存储了什么信息？")
                == QuestionIntentClassifier.Intent.DESCRIPTIVE);
    }

    @Test
    void classifiesMetricQuestionAsSqlQuery() {
        assertTrue(classifier.classify("每个分类的商品数量是多少？")
                == QuestionIntentClassifier.Intent.SQL_QUERY);
    }

    @Test
    void detectsChartOnlyFollowUp() {
        assertTrue(classifier.isChartOnlyFollowUp("画一个折线图"));
    }

    @Test
    void detectsContextFollowUp() {
        assertTrue(classifier.isContextFollowUp("更详细一点"));
    }

    @Test
    void longQuestionWithChartIsNotChartOnlyFollowUp() {
        assertFalse(classifier.isChartOnlyFollowUp(
                "请画一个折线图展示最近6个月每个分类的商品销售总额变化趋势"));
    }
}
