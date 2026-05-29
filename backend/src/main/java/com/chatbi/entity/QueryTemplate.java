package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("query_template")
public class QueryTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dataSourceId;
    private String title;
    private String question;
    private String category;
    private String chartType;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
