package com.wood.woodapi;

import javafx.application.Application;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.core.env.Environment;

import java.io.File;

@SpringBootApplication(exclude={DataSourceAutoConfiguration.class})
@EnableDubbo
public class WoodapiGatewayApplication {
    public static void main(String[] args) {
        System.setProperty("dubbo.meta.cache.filePath.meta", "C:\\Users\\24420\\.dubbo\\gateway");
        System.setProperty("dubbo.mapping.cache.filePath.meta", "C:\\Users\\24420\\.dubbo\\gateway");
        SpringApplication.run(WoodapiGatewayApplication.class, args);
    }
}
