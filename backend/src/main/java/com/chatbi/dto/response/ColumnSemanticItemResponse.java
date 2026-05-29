package com.chatbi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnSemanticItemResponse {

    private Long id;
    private String tableName;
    private String columnName;
    private String businessName;
    private String description;
}
