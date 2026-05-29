package com.chatbi.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SessionBatchDeleteRequest {

    @NotEmpty(message = "请选择要删除的对话")
    private List<String> sessionIds;
}
