package com.wood.woodapigateway;

import com.wood.woodapiclientsdk.utils.SignUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

/**
 * 全局过滤
 */
@Slf4j
@Component
public class CustomGlobalFilter implements GlobalFilter, Ordered {

    private final static List<String> IP_WHITE_LIST = Arrays.asList("127.0.0.1");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 日志
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        System.out.println("请求唯一标识：" + serverHttpRequest.getId());
        InetSocketAddress remoteAddress = serverHttpRequest.getRemoteAddress();
        InetAddress address = remoteAddress.getAddress();
        String hostName = remoteAddress.getHostName();
        System.out.println("请求来源：" + remoteAddress);
        System.out.println("请求目的地：" + serverHttpRequest.getLocalAddress());
        System.out.println("请求方法：" + serverHttpRequest.getMethod());
        System.out.println("请求路径：" + serverHttpRequest.getPath());
        System.out.println("请求参数：" + serverHttpRequest.getQueryParams());
        System.out.println("请求 uri：" + serverHttpRequest.getURI());

        // 2. (黑白名单)
        ServerHttpResponse response = exchange.getResponse();
        if (!IP_WHITE_LIST.contains(hostName)) {
            return handlerNoAuth(response);
        }
        // 3. 用户鉴权(判断 ak、 sk 是否合法)
        HttpHeaders httpHeaders = serverHttpRequest.getHeaders();
        String accessKey = httpHeaders.getFirst("accessKey");
        String nonce = httpHeaders.getFirst("nonce");
        String timestamp = httpHeaders.getFirst("timestamp");
        String sign = httpHeaders.getFirst("sign");
        String body = httpHeaders.getFirst("body");
        // todo 实际情况应该是去数据库中查看用户是否具有accessKey
        if (!"6ee5840c98ddda30c686343405484939".equals(accessKey)) {
            return handlerNoAuth(response);
        }
        if (nonce == null || Long.parseLong(nonce) > 10000) {
            return handlerNoAuth(response);
        }
        // 时间和当前时间不能超过 5 min
        long current = System.currentTimeMillis() / 1000;
        final long FIVE_MINUTES = 60 * 5;
        if (timestamp == null || current - Long.parseLong(timestamp) > FIVE_MINUTES) {
            return handlerNoAuth(response);
        }
        // todo 实际情况中是从服务器查出 secertKey
        if (!SignUtils.getSign(body, "4c72e368e9dc1b5495943c48ee1ecb25").equals(sign)) {
            return handlerNoAuth(response);
        }

        // 4. 请求的模拟接口是否存在?
        // todo 从数据库中查询模拟接口是否存在，以及请求方法是否匹配
        // 5. 请求转发，调用模拟接口
        Mono<Void> filter = chain.filter(exchange);
        // 6. 响应日志
        HttpStatus statusCode = response.getStatusCode();
        log.info("statusCode: " + statusCode);
        // 7. todo 调用成功，接口调用次数 +1 invokeCount
        // 8. 调用失败，返回一个规范的错误码
        if (statusCode == HttpStatus.OK) {

        }
        else {
            return handlerInvokeError(response);
        }
        log.info("custom global filter");
        return filter;
    }

    public Mono<Void> handlerNoAuth(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }

    public Mono<Void> handlerInvokeError(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}