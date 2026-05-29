package com.chatbi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartData {

    private String type;
    /** 维度轴标签，避免 Jackson 将 xAxis 序列化为 xaxis */
    @JsonProperty("xAxis")
    private Object xAxis;
    private Object series;
    private String unit;
    private Object columns;
    private Object rows;
    /** 列 key -> 中文表头，供前端展示 */
    private Map<String, String> columnLabels;
}
