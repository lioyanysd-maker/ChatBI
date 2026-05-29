package com.chatbi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryTemplateResponse {

    private Long id;
    private Long dataSourceId;
    private String title;
    private String question;
    private String category;
    private String chartType;
    private Integer sortOrder;
}
