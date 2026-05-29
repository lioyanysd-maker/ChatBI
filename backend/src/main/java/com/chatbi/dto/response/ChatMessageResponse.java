package com.chatbi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    private String role;
    private String content;
    private String answerType;
    private String sql;
    private Integer rowCount;
    private Long executionTimeMs;
    private ChartData chartData;
}
