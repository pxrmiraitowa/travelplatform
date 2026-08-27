package com.travelplatform.contenttrip;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.travelplatform.contenttrip.mapper")
@SpringBootApplication(scanBasePackages = "com.travelplatform")
public class ContentTripServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentTripServiceApplication.class, args);
    }
}
