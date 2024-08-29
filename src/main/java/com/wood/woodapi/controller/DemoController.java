package com.wood.woodapi.controller;

import com.wood.woodapi.provider.DemoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/demo")
@Slf4j
public class DemoController {
    @DubboReference //声明服务引用
    private DemoService demoService;

    @GetMapping("/test")
    public void testDubbo(String str) {
        String result = demoService.sayHello(str);
        log.info(str);
        System.out.println("consumer开始RPC调用provider服务");
        System.out.println(demoService.sayHello("猪猪皮"));
    }
}