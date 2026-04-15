package com.simulab;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = "com.simulab.modules", annotationClass = Mapper.class)
public class SimuLabBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimuLabBackendApplication.class, args);
    }
}
