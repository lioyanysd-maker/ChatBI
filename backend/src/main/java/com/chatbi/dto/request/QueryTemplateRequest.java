package com.chatbi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QueryTemplateRequest {

    private Long dataSourceId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100)
    private String title;

    @NotBlank(message = "问题不能为空")
    @Size(max = 500)
    private String question;

    private String category = "query";

    private String chartType = "auto";

    private Integer sortOrder = 0;
}
