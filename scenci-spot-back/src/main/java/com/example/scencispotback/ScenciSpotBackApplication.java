package com.example.scencispotback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.example.scencispotback.mapper")
@EnableAspectJAutoProxy
public class ScenciSpotBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScenciSpotBackApplication.class, args);
    }

}
