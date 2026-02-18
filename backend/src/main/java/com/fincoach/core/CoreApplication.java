package com.fincoach.core;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan({
        "com.fincoach.core.repository.mapper",
        "com.fincoach.core.healthv2.mapper",
        "com.fincoach.core.rbac.mapper",
        "com.fincoach.core.ticker.mapper"
})
public class CoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreApplication.class, args);
    }
}
