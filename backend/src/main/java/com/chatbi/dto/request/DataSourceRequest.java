package com.chatbi.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DataSourceRequest {

    @NotBlank(message = "数据源名称不能为空")
    @Size(max = 100, message = "数据源名称不能超过100字符")
    private String name;

    @NotBlank(message = "数据库类型不能为空")
    private String dbType = "mysql";

    @NotBlank(message = "主机地址不能为空")
    @Size(max = 100, message = "主机地址不能超过100字符")
    private String host;

    @NotNull(message = "端口不能为空")
    @Min(value = 1, message = "端口不合法")
    @Max(value = 65535, message = "端口不合法")
    private Integer port;

    @NotBlank(message = "数据库名不能为空")
    @Size(max = 100, message = "数据库名不能超过100字符")
    private String databaseName;

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名不能超过50字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 200, message = "密码不能超过200字符")
    private String password;
}
