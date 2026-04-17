package com.linser;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.linser.mapper")
public class PanicBuyingApplication {

    public static void main(String[] args) {
        SpringApplication.run(PanicBuyingApplication.class, args);
    }
}
