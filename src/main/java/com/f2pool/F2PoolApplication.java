package com.f2pool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class F2PoolApplication {
    public static void main(String[] args) {
        SpringApplication.run(F2PoolApplication.class, args);
    }
}
