package com.logistics.routes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LogisticsRoutesApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogisticsRoutesApplication.class, args);
    }
}
