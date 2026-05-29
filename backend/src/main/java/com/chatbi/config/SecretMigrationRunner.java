package com.chatbi.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.entity.DataSourceEntity;
import com.chatbi.mapper.DataSourceMapper;
import com.chatbi.util.SecretCryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecretMigrationRunner {

    private final DataSourceMapper dataSourceMapper;
    private final SecretCryptoService secretCryptoService;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateDataSourcePasswords() {
        List<DataSourceEntity> datasources = dataSourceMapper.selectList(
                new LambdaQueryWrapper<DataSourceEntity>().eq(DataSourceEntity::getStatus, 1)
        );
        int migrated = 0;
        for (DataSourceEntity entity : datasources) {
            if (entity.getPassword() == null || secretCryptoService.isEncrypted(entity.getPassword())) {
                continue;
            }
            entity.setPassword(secretCryptoService.encrypt(entity.getPassword()));
            dataSourceMapper.updateById(entity);
            migrated++;
        }
        if (migrated > 0) {
            log.info("Migrated {} data source password(s) to encrypted storage", migrated);
        }
    }
}
