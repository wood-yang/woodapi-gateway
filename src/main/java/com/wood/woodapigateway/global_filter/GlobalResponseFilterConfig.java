package com.wood.woodapigateway.global_filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class GlobalResponseFilterConfig implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            log.info("global response filter");
            ServerHttpResponse response = exchange.getResponse();
            // 6. 响应日志
            HttpStatus statusCode = response.getStatusCode();
            log.info("statusCode: " + statusCode);
            // 7. todo 调用成功，接口调用次数 +1 invokeCount
            // todo 怎么定义失败，一个就是根本就发过去，或者其他异常，也有可能是业务问题 出bug了 导致虽然OK 200 了 但是数据是不对的
            // 8. 调用失败，返回一个规范的错误码
            if (statusCode == HttpStatus.OK) {

            }
            else {
                response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }));
    }

    @Override
    public int getOrder() {
        return -3;
    }
}