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
        // 创建SpringApplication实例
        SpringApplication application = new SpringApplication(Application.class);
        // 添加自定义的ApplicationContextInitializer
        application.addInitializers(context -> {
            // 获取Environment对象
            Environment env = context.getEnvironment();
            // 从Environment中读取"spring.application.name"属性值
//            String appName = env.getProperty("spring.application.name");
            String appName = "gateway";
            String filePath = System.getProperty("user.home") + File.separator + ".dubbo" +File.separator + appName;
            // 修改dubbo的本地缓存路径，避免缓存冲突
            System.setProperty("dubbo.meta.cache.filePath.meta", filePath);
            System.setProperty("dubbo.mapping.cache.filePath.mapping", filePath);
        });
        //启动应用
//        application.run(args);
        application.run(WoodapiGatewayApplication.class, args);

//        SpringApplication.run(WoodapiGatewayApplication.class, args);
    }
}
