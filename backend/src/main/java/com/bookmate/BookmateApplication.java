package com.bookmate;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.bookmate.mapper")
public class BookmateApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookmateApplication.class, args);
    }
}
