package com.egg.springboot_egg;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.egg.springboot_egg.mapper") // 指定 Mapper 包
@EnableAsync  // 开启异步支持
@EnableCaching  // 开启缓存功能
public class SpringbootEggApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootEggApplication.class, args);
    }

}
