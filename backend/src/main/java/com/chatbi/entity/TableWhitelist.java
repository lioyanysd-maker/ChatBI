package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("table_whitelist")
public class TableWhitelist {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dataSourceId;
    private String tableName;
    private String tableComment;
    private Integer isActive;
}
