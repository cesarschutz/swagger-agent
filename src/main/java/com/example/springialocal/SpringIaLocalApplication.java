package com.example.springialocal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class SpringIaLocalApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringIaLocalApplication.class, args);
    }
}