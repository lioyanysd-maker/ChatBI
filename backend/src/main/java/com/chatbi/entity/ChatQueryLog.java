package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_query_log")
public class ChatQueryLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private Long dataSourceId;
    private String question;
    private String generatedSql;
    private String executedSql;
    private Boolean isSafe;
    private String errorMessage;
    private Integer rowCount;
    private Integer executionTimeMs;
    private String chartType;
    private String chartData;
    private String answerText;
    private String answerType;
    private LocalDateTime createdAt;
}
