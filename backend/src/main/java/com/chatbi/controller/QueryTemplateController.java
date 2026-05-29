package com.chatbi.controller;

import com.chatbi.dto.request.QueryTemplateRequest;
import com.chatbi.dto.response.ApiResponse;
import com.chatbi.dto.response.QueryTemplateResponse;
import com.chatbi.service.QueryTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chatbi/templates")
@RequiredArgsConstructor
public class QueryTemplateController {

    private final QueryTemplateService queryTemplateService;

    @GetMapping
    public ApiResponse<List<QueryTemplateResponse>> list(
            @RequestParam(required = false) Long dataSourceId) {
        return ApiResponse.ok(queryTemplateService.listTemplates(dataSourceId));
    }

    @PostMapping
    public ApiResponse<QueryTemplateResponse> create(@Valid @RequestBody QueryTemplateRequest request) {
        return ApiResponse.ok(queryTemplateService.create(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        queryTemplateService.delete(id);
        return ApiResponse.ok(null);
    }
}
