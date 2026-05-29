package com.chatbi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponse {

    private String sessionId;
    private String question;
    /** query=数据查询, chat=描述性问答 */
    private String answerType;
    private String generatedSql;
    private ChartData chartData;
    private String naturalExplanation;
    private Integer rowCount;
    private Long executionTimeMs;
    private List<Map<String, Object>> rawRows;
}
