package com.f2pool.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.f2pool.mapper")
public class MyBatisPlusConfig {
}
