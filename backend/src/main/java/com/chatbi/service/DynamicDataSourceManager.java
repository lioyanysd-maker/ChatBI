package com.chatbi.service;

import com.chatbi.dto.request.DataSourceRequest;
import com.chatbi.entity.DataSourceEntity;
import com.chatbi.exception.BusinessException;
import com.chatbi.util.SecretCryptoService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicDataSourceManager {

    private final SecretCryptoService secretCryptoService;

    private final ConcurrentHashMap<Long, HikariDataSource> pools = new ConcurrentHashMap<>();

    public JdbcTemplate getJdbcTemplate(DataSourceEntity entity) {
        HikariDataSource ds = pools.computeIfAbsent(entity.getId(), id -> createPool(entity));
        return new JdbcTemplate(ds);
    }

    public void evict(Long dataSourceId) {
        HikariDataSource ds = pools.remove(dataSourceId);
        if (ds != null) {
            ds.close();
        }
    }

    public boolean testConnection(DataSourceRequest request) {
        try (HikariDataSource ds = createPool(request)) {
            ds.getConnection().close();
            return true;
        } catch (Exception ex) {
            log.warn("Connection test failed: {}", ex.getMessage());
            throw new BusinessException(400, "连接失败：" + ex.getMessage());
        }
    }

    private HikariDataSource createPool(DataSourceEntity entity) {
        return createPool(entity.getDbType(), entity.getHost(), entity.getPort(),
                entity.getDatabaseName(), entity.getUsername(),
                secretCryptoService.decrypt(entity.getPassword()));
    }

    private HikariDataSource createPool(DataSourceRequest request) {
        return createPool(request.getDbType(), request.getHost(), request.getPort(),
                request.getDatabaseName(), request.getUsername(), request.getPassword());
    }

    private HikariDataSource createPool(String dbType, String host, Integer port,
                                        String databaseName, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(buildJdbcUrl(dbType, host, port, databaseName));
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5000);
        config.setPoolName("chatbi-ds-" + databaseName);
        return new HikariDataSource(config);
    }

    private String buildJdbcUrl(String dbType, String host, Integer port, String databaseName) {
        String type = dbType == null ? "mysql" : dbType.toLowerCase();
        return switch (type) {
            case "pg", "postgresql" -> String.format(
                    "jdbc:postgresql://%s:%d/%s", host, port, databaseName);
            case "oracle" -> String.format(
                    "jdbc:oracle:thin:@%s:%d:%s", host, port, databaseName);
            default -> String.format(
                    "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false",
                    host, port, databaseName);
        };
    }
}
