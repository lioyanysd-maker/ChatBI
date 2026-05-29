package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("column_semantic")
public class ColumnSemantic {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dataSourceId;
    private String tableName;
    private String columnName;
    private String businessName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
