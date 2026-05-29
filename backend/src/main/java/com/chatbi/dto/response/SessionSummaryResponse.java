package com.chatbi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSummaryResponse {

    private String sessionId;
    private String title;
    private Long dataSourceId;
    private LocalDateTime updatedAt;
    private Integer messageCount;
}
