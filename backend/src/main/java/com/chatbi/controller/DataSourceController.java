package com.chatbi.controller;

import com.chatbi.dto.request.ColumnSemanticUpdateRequest;
import com.chatbi.dto.request.DataSourceRequest;
import com.chatbi.dto.response.ApiResponse;
import com.chatbi.dto.response.ColumnSemanticItemResponse;
import com.chatbi.dto.response.DataSourceResponse;
import com.chatbi.dto.request.TableWhitelistUpdateRequest;
import com.chatbi.dto.response.TableWhitelistItemResponse;
import com.chatbi.service.ColumnSemanticService;
import com.chatbi.service.DataSourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbi/datasources")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService dataSourceService;
    private final ColumnSemanticService columnSemanticService;

    @GetMapping
    public ApiResponse<List<DataSourceResponse>> list() {
        return ApiResponse.ok(dataSourceService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<DataSourceResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(dataSourceService.getById(id));
    }

    @PostMapping
    public ApiResponse<DataSourceResponse> create(@Valid @RequestBody DataSourceRequest request) {
        return ApiResponse.ok(dataSourceService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<DataSourceResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody DataSourceRequest request) {
        return ApiResponse.ok(dataSourceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dataSourceService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/test")
    public ApiResponse<Map<String, Object>> test(@Valid @RequestBody DataSourceRequest request) {
        dataSourceService.testConnection(request);
        return ApiResponse.ok(Map.of("success", true, "message", "连接成功"));
    }

    @GetMapping("/{id}/whitelist")
    public ApiResponse<List<TableWhitelistItemResponse>> whitelist(@PathVariable Long id) {
        return ApiResponse.ok(dataSourceService.listWhitelist(id));
    }

    @PutMapping("/{id}/whitelist")
    public ApiResponse<List<TableWhitelistItemResponse>> updateWhitelist(
            @PathVariable Long id,
            @Valid @RequestBody TableWhitelistUpdateRequest request) {
        return ApiResponse.ok(dataSourceService.updateWhitelist(id, request));
    }

    @GetMapping("/{id}/semantics/columns")
    public ApiResponse<List<ColumnSemanticItemResponse>> bootstrapSemantics(
            @PathVariable Long id,
            @RequestParam String table) {
        return ApiResponse.ok(columnSemanticService.bootstrapTableColumns(id, table));
    }

    @PutMapping("/{id}/semantics")
    public ApiResponse<List<ColumnSemanticItemResponse>> updateSemantics(
            @PathVariable Long id,
            @Valid @RequestBody ColumnSemanticUpdateRequest request) {
        return ApiResponse.ok(columnSemanticService.updateSemantics(id, request));
    }
}
