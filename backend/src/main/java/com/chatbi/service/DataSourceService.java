package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.dto.request.DataSourceRequest;
import com.chatbi.dto.request.TableWhitelistUpdateRequest;
import com.chatbi.dto.response.DataSourceResponse;
import com.chatbi.dto.response.TableWhitelistItemResponse;
import com.chatbi.entity.DataSourceEntity;
import com.chatbi.entity.TableWhitelist;
import com.chatbi.exception.BusinessException;
import com.chatbi.mapper.DataSourceMapper;
import com.chatbi.util.SecretCryptoService;
import com.chatbi.mapper.TableWhitelistMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataSourceService {

    private final DataSourceMapper dataSourceMapper;
    private final TableWhitelistMapper tableWhitelistMapper;
    private final DynamicDataSourceManager dynamicDataSourceManager;
    private final SecretCryptoService secretCryptoService;

    public List<DataSourceResponse> listAll() {
        return dataSourceMapper.selectList(
                new LambdaQueryWrapper<DataSourceEntity>()
                        .eq(DataSourceEntity::getStatus, 1)
                        .orderByDesc(DataSourceEntity::getCreatedAt)
        ).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public DataSourceResponse getById(Long id) {
        DataSourceEntity entity = requireEntity(id);
        return toResponse(entity);
    }

    @Transactional
    public DataSourceResponse create(DataSourceRequest request) {
        dynamicDataSourceManager.testConnection(request);

        DataSourceEntity entity = toEntity(request);
        entity.setStatus(1);
        entity.setCreatedAt(LocalDateTime.now());
        dataSourceMapper.insert(entity);

        syncTableWhitelist(entity);
        return toResponse(entity);
    }

    @Transactional
    public DataSourceResponse update(Long id, DataSourceRequest request) {
        DataSourceEntity entity = requireEntity(id);
        dynamicDataSourceManager.testConnection(request);
        dynamicDataSourceManager.evict(id);

        entity.setName(request.getName());
        entity.setDbType(request.getDbType());
        entity.setHost(request.getHost());
        entity.setPort(request.getPort());
        entity.setDatabaseName(request.getDatabaseName());
        entity.setUsername(request.getUsername());
        entity.setPassword(secretCryptoService.encrypt(request.getPassword()));
        dataSourceMapper.updateById(entity);

        resyncTableWhitelist(entity);
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        DataSourceEntity entity = requireEntity(id);
        entity.setStatus(0);
        dataSourceMapper.updateById(entity);
        dynamicDataSourceManager.evict(id);
    }

    public void testConnection(DataSourceRequest request) {
        dynamicDataSourceManager.testConnection(request);
    }

    public JdbcTemplate getJdbcTemplate(Long dataSourceId) {
        DataSourceEntity entity = requireEntity(dataSourceId);
        return dynamicDataSourceManager.getJdbcTemplate(entity);
    }

    public DataSourceEntity requireEntity(Long id) {
        DataSourceEntity entity = dataSourceMapper.selectById(id);
        if (entity == null || entity.getStatus() == null || entity.getStatus() != 1) {
            throw new BusinessException(404, "数据源不存在");
        }
        return entity;
    }

    private void syncTableWhitelist(DataSourceEntity entity) {
        JdbcTemplate jdbcTemplate = dynamicDataSourceManager.getJdbcTemplate(entity);
        List<String> tables = discoverTables(jdbcTemplate, entity.getDbType(), entity.getDatabaseName());
        for (String table : tables) {
            TableWhitelist whitelist = new TableWhitelist();
            whitelist.setDataSourceId(entity.getId());
            whitelist.setTableName(table);
            whitelist.setTableComment("");
            whitelist.setIsActive(1);
            tableWhitelistMapper.insert(whitelist);
        }
    }

    private void resyncTableWhitelist(DataSourceEntity entity) {
        tableWhitelistMapper.delete(
                new LambdaQueryWrapper<TableWhitelist>().eq(TableWhitelist::getDataSourceId, entity.getId())
        );
        syncTableWhitelist(entity);
    }

    private List<String> discoverTables(JdbcTemplate jdbcTemplate, String dbType, String databaseName) {
        String type = dbType == null ? "mysql" : dbType.toLowerCase(Locale.ROOT);
        if (type.startsWith("pg")) {
            return jdbcTemplate.queryForList(
                    """
                    SELECT table_name
                    FROM information_schema.tables
                    WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                    ORDER BY table_name
                    """,
                    String.class
            );
        }
        return jdbcTemplate.queryForList(
                """
                SELECT TABLE_NAME
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE'
                ORDER BY TABLE_NAME
                """,
                String.class,
                databaseName
        );
    }

    public String resolveDbType(Long dataSourceId) {
        if (dataSourceId == null) {
            return "mysql";
        }
        String dbType = requireEntity(dataSourceId).getDbType();
        return dbType == null ? "mysql" : dbType.toLowerCase(Locale.ROOT);
    }

    public List<TableWhitelistItemResponse> listWhitelist(Long dataSourceId) {
        requireEntity(dataSourceId);
        return tableWhitelistMapper.selectList(
                new LambdaQueryWrapper<TableWhitelist>()
                        .eq(TableWhitelist::getDataSourceId, dataSourceId)
                        .orderByAsc(TableWhitelist::getTableName)
        ).stream().map(this::toWhitelistResponse).collect(Collectors.toList());
    }

    @Transactional
    public List<TableWhitelistItemResponse> updateWhitelist(Long dataSourceId, TableWhitelistUpdateRequest request) {
        requireEntity(dataSourceId);
        for (TableWhitelistUpdateRequest.Item item : request.getItems()) {
            if (item.getTableName() == null || item.getTableName().isBlank()) {
                continue;
            }
            TableWhitelist existing = tableWhitelistMapper.selectOne(
                    new LambdaQueryWrapper<TableWhitelist>()
                            .eq(TableWhitelist::getDataSourceId, dataSourceId)
                            .eq(TableWhitelist::getTableName, item.getTableName())
            );
            if (existing == null) {
                continue;
            }
            existing.setIsActive(Boolean.TRUE.equals(item.getActive()) ? 1 : 0);
            if (item.getTableComment() != null) {
                existing.setTableComment(item.getTableComment());
            }
            tableWhitelistMapper.updateById(existing);
        }
        return listWhitelist(dataSourceId);
    }

    private TableWhitelistItemResponse toWhitelistResponse(TableWhitelist entity) {
        return TableWhitelistItemResponse.builder()
                .id(entity.getId())
                .tableName(entity.getTableName())
                .tableComment(entity.getTableComment())
                .active(entity.getIsActive() != null && entity.getIsActive() == 1)
                .build();
    }

    private DataSourceEntity toEntity(DataSourceRequest request) {
        DataSourceEntity entity = new DataSourceEntity();
        entity.setName(request.getName());
        entity.setDbType(request.getDbType());
        entity.setHost(request.getHost());
        entity.setPort(request.getPort());
        entity.setDatabaseName(request.getDatabaseName());
        entity.setUsername(request.getUsername());
        entity.setPassword(secretCryptoService.encrypt(request.getPassword()));
        return entity;
    }

    private DataSourceResponse toResponse(DataSourceEntity entity) {
        return DataSourceResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .dbType(entity.getDbType())
                .host(entity.getHost())
                .port(entity.getPort())
                .databaseName(entity.getDatabaseName())
                .username(entity.getUsername())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
