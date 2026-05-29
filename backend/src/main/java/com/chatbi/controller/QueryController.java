package com.chatbi.controller;

import com.chatbi.dto.request.ExecuteSqlRequest;
import com.chatbi.dto.request.QueryRequest;
import com.chatbi.dto.response.ApiResponse;
import com.chatbi.dto.response.QueryResponse;
import com.chatbi.service.SQLGenerateService;
import com.chatbi.service.SchemaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbi")
@RequiredArgsConstructor
public class QueryController {

    private final SQLGenerateService sqlGenerateService;
    private final SchemaService schemaService;

    @PostMapping("/query")
    public ApiResponse<QueryResponse> query(@Valid @RequestBody QueryRequest request) {
        return ApiResponse.ok(sqlGenerateService.query(request));
    }

    @PostMapping("/execute-sql")
    public ApiResponse<QueryResponse> executeSql(@Valid @RequestBody ExecuteSqlRequest request) {
        return ApiResponse.ok(sqlGenerateService.executeSql(request));
    }

    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter queryStream(@Valid @RequestBody QueryRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        sqlGenerateService.queryStream(request, emitter);
        return emitter;
    }

    @GetMapping("/schema")
    public ApiResponse<Map<String, Object>> schema(
            @RequestParam(required = false) Long dataSourceId) {
        return ApiResponse.ok(schemaService.getSchemaSummary(dataSourceId));
    }
}
