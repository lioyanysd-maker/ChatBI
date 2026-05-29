package com.chatbi.service;

/**
 * SSE 事件发送器，供流式查询使用。
 */
public interface QueryStreamSink {

    void sendStep(String message);

    void sendSql(String sql);

    void sendChart(Object chartData);

    void sendDelta(String text);

    void sendDone(Object response);

    void sendError(String message);
}
