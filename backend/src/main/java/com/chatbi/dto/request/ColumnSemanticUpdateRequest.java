package com.chatbi.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ColumnSemanticUpdateRequest {

    @NotEmpty(message = "字段语义不能为空")
    private List<Item> items;

    @Data
    public static class Item {
        private String tableName;
        private String columnName;
        private String businessName;
        private String description;
    }
}
