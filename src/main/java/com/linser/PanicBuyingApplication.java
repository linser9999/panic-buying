package com.linser;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.linser.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class PanicBuyingApplication {

    public static void main(String[] args) {
        SpringApplication.run(PanicBuyingApplication.class, args);
    }
}
