package com.chatbi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.chatbi.mapper")
public class ChatbiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatbiApplication.class, args);
    }
}
