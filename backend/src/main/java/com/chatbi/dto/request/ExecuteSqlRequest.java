package com.chatbi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExecuteSqlRequest {

    @NotBlank(message = "SQL 不能为空")
    @Size(max = 4000, message = "SQL 长度不能超过4000字符")
    private String sql;

    private String sessionId;

    /** auto / bar / line / pie / table */
    private String chartType = "auto";

    private Long dataSourceId;

    /** 用于日志展示，可选 */
    @Size(max = 200)
    private String question;
}
