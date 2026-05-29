package com.chatbi.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class TableWhitelistUpdateRequest {

    @NotEmpty(message = "表白名单不能为空")
    private List<Item> items;

    @Data
    public static class Item {
        private String tableName;
        private String tableComment;
        private Boolean active;
    }
}
