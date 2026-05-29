package com.chatbi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatbi.dto.request.SessionBatchDeleteRequest;
import com.chatbi.dto.response.ApiResponse;
import com.chatbi.dto.response.SessionMessagesResponse;
import com.chatbi.dto.response.SessionSummaryResponse;
import com.chatbi.entity.ChatQueryLog;
import com.chatbi.service.CacheService;
import com.chatbi.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/chatbi")
@RequiredArgsConstructor
public class SessionController {

    private final CacheService cacheService;
    private final SessionService sessionService;

    @GetMapping("/sessions")
    public ApiResponse<List<SessionSummaryResponse>> listSessions(
            @RequestParam(defaultValue = "30") int limit) {
        return ApiResponse.ok(sessionService.listSessions(limit));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<SessionMessagesResponse> sessionMessages(@PathVariable String sessionId) {
        return ApiResponse.ok(sessionService.getSessionMessages(sessionId));
    }

    @GetMapping("/history")
    public ApiResponse<Page<ChatQueryLog>> history(
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(cacheService.getHistory(sessionId, page, size));
    }

    @DeleteMapping("/sessions")
    public ApiResponse<Void> deleteSessions(@Valid @RequestBody SessionBatchDeleteRequest request) {
        sessionService.deleteSessions(request.getSessionIds());
        return ApiResponse.ok(null);
    }
}
