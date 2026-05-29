package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatbi.entity.ChatQueryLog;
import com.chatbi.mapper.ChatQueryLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CacheService {

    private final ChatQueryLogMapper chatQueryLogMapper;

    public Page<ChatQueryLog> getHistory(String sessionId, int page, int size) {
        return chatQueryLogMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<ChatQueryLog>()
                        .eq(ChatQueryLog::getSessionId, sessionId)
                        .orderByDesc(ChatQueryLog::getCreatedAt)
        );
    }
}
