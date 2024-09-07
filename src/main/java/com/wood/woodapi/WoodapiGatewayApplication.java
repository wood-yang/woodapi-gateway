package com.wood.woodapi;

import com.wood.common.service.InnerUserInterfaceInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude={DataSourceAutoConfiguration.class})
@EnableDubbo
@Slf4j
public class WoodapiGatewayApplication {
    @DubboReference
    private InnerUserInterfaceInfoService innerUserInterfaceInfoService;

    public static void main(String[] args) {
        SpringApplication.run(WoodapiGatewayApplication.class, args);
    }

//    @Bean
//    public RouteLocator myRoutes(RouteLocatorBuilder routeLocatorBuilder, HttpServletRequest request)
//    {
//        String newUri = request.getHeader("newUri");
//        return routeLocatorBuilder.routes()
//                .route(r ->
//                        r.uri(newUri))
//                .build();
//    }

//    @Bean
//    public GlobalFilter customGlobalPostFilter() {
//        return (exchange, chain) -> chain.filter(exchange)
//                .then(Mono.just(exchange))
//                .map(serverWebExchange -> {
//                    ServerHttpResponse response = serverWebExchange.getResponse();
//                    // 6. 响应日志
//                    HttpStatus statusCode = response.getStatusCode();
//                    log.info("statusCode: " + statusCode);
//                    // 7. 调用成功，接口调用次数 +1 invokeCount
//                    // todo 怎么定义失败，一个就是根本就发过去，或者其他异常，也有可能是业务问题 出bug了 导致虽然OK 200 了 但是数据是不对的
//                    // 8. 调用失败，返回一个规范的错误码
//                    if (statusCode == HttpStatus.OK) {
//                        ServerHttpRequest request = exchange.getRequest();
//                        HttpHeaders headers = request.getHeaders();
//                        String userId = headers.getFirst("userId");
//                        String interfaceInfoId = headers.getFirst("interfaceInfoId");
//                        innerUserInterfaceInfoService.invokeCount(Long.parseLong(userId), Long.parseLong(interfaceInfoId));
//                        log.info("统计次数完成");
//                    }
//                    else {
//                        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
//                    }
//                    return serverWebExchange;
//                })
//                .then();
//    }
}
