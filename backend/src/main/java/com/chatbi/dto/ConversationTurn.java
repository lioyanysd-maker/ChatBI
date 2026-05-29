package com.chatbi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConversationTurn {

    private String question;
    private String answerType;
    private String answerText;
    private String generatedSql;
    private String chartType;
}
