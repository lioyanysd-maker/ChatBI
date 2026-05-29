package com.chatbi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
public class SseQueryStreamSink implements QueryStreamSink {

    private final ObjectMapper objectMapper;
    private final SseEmitter emitter;

    public SseQueryStreamSink(ObjectMapper objectMapper, SseEmitter emitter) {
        this.objectMapper = objectMapper;
        this.emitter = emitter;
    }

    @Override
    public void sendStep(String message) {
        safeSend(SseEmitter.event().name("step").data(message));
    }

    @Override
    public void sendSql(String sql) {
        safeSend(SseEmitter.event().name("sql").data(sql));
    }

    @Override
    public void sendChart(Object chartData) {
        try {
            safeSend(SseEmitter.event().name("chart").data(objectMapper.writeValueAsString(chartData)));
        } catch (Exception ex) {
            log.warn("Failed to serialize chart data: {}", ex.getMessage());
        }
    }

    @Override
    public void sendDelta(String text) {
        safeSend(SseEmitter.event().name("delta").data(text));
    }

    @Override
    public void sendDone(Object response) {
        try {
            safeSend(SseEmitter.event().name("done").data(objectMapper.writeValueAsString(response)));
        } catch (Exception ex) {
            log.warn("Failed to serialize done payload: {}", ex.getMessage());
        }
    }

    @Override
    public void sendError(String message) {
        safeSend(SseEmitter.event().name("error").data(message));
    }

    private void safeSend(SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (Exception ex) {
            log.warn("Failed to send SSE event: {}", ex.getMessage());
        }
    }
}
