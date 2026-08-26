package com.travelplatform.contenttrip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.travelplatform")
public class ContentTripServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentTripServiceApplication.class, args);
    }
}
