package com.chatbi.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.dto.request.ColumnSemanticUpdateRequest;
import com.chatbi.dto.response.ColumnSemanticItemResponse;
import com.chatbi.entity.ColumnSemantic;
import com.chatbi.entity.DataSourceEntity;
import com.chatbi.mapper.ColumnSemanticMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ColumnSemanticService {

    private final ColumnSemanticMapper columnSemanticMapper;
    private final DataSourceService dataSourceService;

    public List<ColumnSemanticItemResponse> listByTable(Long dataSourceId, String tableName) {
        dataSourceService.requireEntity(dataSourceId);
        if (StrUtil.isBlank(tableName)) {
            return List.of();
        }
        return columnSemanticMapper.selectList(
                new LambdaQueryWrapper<ColumnSemantic>()
                        .eq(ColumnSemantic::getDataSourceId, dataSourceId)
                        .eq(ColumnSemantic::getTableName, tableName)
                        .orderByAsc(ColumnSemantic::getColumnName)
        ).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<String> listConfiguredTables(Long dataSourceId) {
        dataSourceService.requireEntity(dataSourceId);
        return columnSemanticMapper.selectList(
                new LambdaQueryWrapper<ColumnSemantic>()
                        .eq(ColumnSemantic::getDataSourceId, dataSourceId)
                        .select(ColumnSemantic::getTableName)
        ).stream()
                .map(ColumnSemantic::getTableName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ColumnSemanticItemResponse> updateSemantics(Long dataSourceId, ColumnSemanticUpdateRequest request) {
        dataSourceService.requireEntity(dataSourceId);
        List<ColumnSemanticItemResponse> saved = new ArrayList<>();
        for (ColumnSemanticUpdateRequest.Item item : request.getItems()) {
            if (StrUtil.isBlank(item.getTableName()) || StrUtil.isBlank(item.getColumnName())) {
                continue;
            }
            ColumnSemantic existing = columnSemanticMapper.selectOne(
                    new LambdaQueryWrapper<ColumnSemantic>()
                            .eq(ColumnSemantic::getDataSourceId, dataSourceId)
                            .eq(ColumnSemantic::getTableName, item.getTableName())
                            .eq(ColumnSemantic::getColumnName, item.getColumnName())
            );
            boolean blank = StrUtil.isBlank(item.getBusinessName()) && StrUtil.isBlank(item.getDescription());
            if (existing != null && blank) {
                columnSemanticMapper.deleteById(existing.getId());
                continue;
            }
            if (blank) {
                continue;
            }
            if (existing == null) {
                existing = new ColumnSemantic();
                existing.setDataSourceId(dataSourceId);
                existing.setTableName(item.getTableName());
                existing.setColumnName(item.getColumnName());
                existing.setCreatedAt(LocalDateTime.now());
            }
            existing.setBusinessName(StrUtil.blankToDefault(item.getBusinessName(), null));
            existing.setDescription(StrUtil.blankToDefault(item.getDescription(), null));
            existing.setUpdatedAt(LocalDateTime.now());
            if (existing.getId() == null) {
                columnSemanticMapper.insert(existing);
            } else {
                columnSemanticMapper.updateById(existing);
            }
            saved.add(toResponse(existing));
        }
        return saved;
    }

    public Map<String, String> buildColumnLabelIndex(Long dataSourceId) {
        LambdaQueryWrapper<ColumnSemantic> wrapper = new LambdaQueryWrapper<>();
        if (dataSourceId != null) {
            wrapper.eq(ColumnSemantic::getDataSourceId, dataSourceId);
        }
        Map<String, String> index = new HashMap<>();
        for (ColumnSemantic item : columnSemanticMapper.selectList(wrapper)) {
            if (StrUtil.isNotBlank(item.getBusinessName())) {
                index.put(item.getColumnName().toLowerCase(Locale.ROOT), item.getBusinessName().trim());
            }
        }
        return index;
    }

    public Map<String, String> buildColumnDescriptionIndex(Long dataSourceId) {
        LambdaQueryWrapper<ColumnSemantic> wrapper = new LambdaQueryWrapper<>();
        if (dataSourceId != null) {
            wrapper.eq(ColumnSemantic::getDataSourceId, dataSourceId);
        }
        Map<String, String> index = new HashMap<>();
        for (ColumnSemantic item : columnSemanticMapper.selectList(wrapper)) {
            String key = item.getTableName().toLowerCase(Locale.ROOT) + "."
                    + item.getColumnName().toLowerCase(Locale.ROOT);
            if (StrUtil.isNotBlank(item.getDescription())) {
                index.put(key, item.getDescription().trim());
            }
        }
        return index;
    }

    public List<ColumnSemanticItemResponse> bootstrapTableColumns(Long dataSourceId, String tableName) {
        dataSourceService.requireEntity(dataSourceId);
        DataSourceEntity entity = dataSourceService.requireEntity(dataSourceId);
        JdbcTemplate template = dataSourceService.getJdbcTemplate(dataSourceId);
        String schemaName = entity.getDatabaseName();
        List<Map<String, Object>> columns = queryColumns(template, schemaName, tableName, entity.getDbType());
        Map<String, ColumnSemantic> existing = columnSemanticMapper.selectList(
                new LambdaQueryWrapper<ColumnSemantic>()
                        .eq(ColumnSemantic::getDataSourceId, dataSourceId)
                        .eq(ColumnSemantic::getTableName, tableName)
        ).stream().collect(Collectors.toMap(
                c -> c.getColumnName().toLowerCase(Locale.ROOT),
                c -> c,
                (a, b) -> a
        ));

        List<ColumnSemanticItemResponse> result = new ArrayList<>();
        for (Map<String, Object> col : columns) {
            String columnName = String.valueOf(col.get("COLUMN_NAME"));
            ColumnSemantic semantic = existing.get(columnName.toLowerCase(Locale.ROOT));
            if (semantic != null) {
                result.add(toResponse(semantic));
            } else {
                result.add(ColumnSemanticItemResponse.builder()
                        .tableName(tableName)
                        .columnName(columnName)
                        .businessName(col.get("COLUMN_COMMENT") != null ? col.get("COLUMN_COMMENT").toString() : "")
                        .description("")
                        .build());
            }
        }
        return result;
    }

    private List<Map<String, Object>> queryColumns(JdbcTemplate template, String schemaName,
                                                   String table, String dbType) {
        String type = dbType == null ? "mysql" : dbType.toLowerCase(Locale.ROOT);
        if (type.startsWith("pg")) {
            return template.queryForList(
                    """
                    SELECT column_name AS COLUMN_NAME, '' AS COLUMN_COMMENT
                    FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = ?
                    ORDER BY ordinal_position
                    """,
                    table
            );
        }
        return template.queryForList(
                """
                SELECT COLUMN_NAME, COLUMN_COMMENT
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
                ORDER BY ORDINAL_POSITION
                """,
                schemaName, table
        );
    }

    private ColumnSemanticItemResponse toResponse(ColumnSemantic entity) {
        return ColumnSemanticItemResponse.builder()
                .id(entity.getId())
                .tableName(entity.getTableName())
                .columnName(entity.getColumnName())
                .businessName(entity.getBusinessName())
                .description(entity.getDescription())
                .build();
    }
}
