package com.chatbi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableWhitelistItemResponse {

    private Long id;
    private String tableName;
    private String tableComment;
    private Boolean active;
}
