package com.chatbi.service;

import com.chatbi.util.SQLValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class SecurityService {

    private final SQLValidator sqlValidator;

    public String validate(String sql, Set<String> allowedTables) {
        return sqlValidator.validateAndRewrite(sql, allowedTables);
    }
}
