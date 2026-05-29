package com.chatbi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionMessagesResponse {

    private String sessionId;
    private Long dataSourceId;
    private List<ChatMessageResponse> messages;
}
