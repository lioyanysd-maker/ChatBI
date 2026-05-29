package com.chatbi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QueryRequest {

    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题长度不能超过500字符")
    private String question;

    private String sessionId;

    /** auto / bar / line / pie / table */
    private String chartType = "auto";

    /** 数据源 ID，为空时使用默认库 */
    private Long dataSourceId;
}
