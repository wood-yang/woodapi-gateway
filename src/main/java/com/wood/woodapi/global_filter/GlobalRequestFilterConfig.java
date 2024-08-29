package com.wood.woodapi.global_filter;

import com.wood.common.model.entity.InterfaceInfo;
import com.wood.common.model.entity.User;
import com.wood.woodapiclientsdk.utils.SignUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import com.wood.common.service.*;
import com.wood.common.service.InnerInterfaceInfoService;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * 全局请求过滤
 */
@Slf4j
@Configuration
public class GlobalRequestFilterConfig implements GlobalFilter, Ordered {

    @DubboReference
    private InnerUserService innerUserService;

    @DubboReference
    private InnerInterfaceInfoService innerInterfaceInfoService;

    private final static List<String> IP_WHITE_LIST = Arrays.asList("127.0.0.1");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("global request filter");
        // 1. 日志
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpRequest newRequest = request.mutate().headers(HttpHeaders::entrySet).path("/myPath").build();
        System.out.println("请求唯一标识：" + request.getId());
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        String address = remoteAddress.getAddress().toString();
        String hostName = remoteAddress.getHostName();
        System.out.println("请求来源：" + remoteAddress);
        System.out.println("请求目的地：" + request.getLocalAddress());
        System.out.println("请求方法：" + request.getMethod());
        String path = request.getPath().toString();
        System.out.println("请求路径：" + path);
        System.out.println("请求参数：" + request.getQueryParams());
        String uri = request.getURI().toString();
        System.out.println("请求 uri：" + uri);

        // 2. (黑白名单)
        ServerHttpResponse response = exchange.getResponse();
        if (!IP_WHITE_LIST.contains(hostName)) {
            return handlerNoAuth(response);
        }
        // 3. 用户鉴权(判断 ak、 sk 是否合法)
        HttpHeaders httpHeaders = request.getHeaders();
        String accessKey = httpHeaders.getFirst("accessKey");
        String nonce = httpHeaders.getFirst("nonce");
        String timestamp = httpHeaders.getFirst("timestamp");
        String sign = httpHeaders.getFirst("sign");
        String body = httpHeaders.getFirst("body");
        // 去数据库中查看用户是否具有accessKey
        User user = innerUserService.getUserByAccessKey(accessKey);

        if (user == null || user.getAccessKey() == null) {
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
        // 验证 sign
        String secretKey = user.getSecretKey();
        if (secretKey == null) {
            return handlerNoAuth(response);
        }
        if (!SignUtils.getSign(body, secretKey).equals(sign)) {
            return handlerNoAuth(response);
        }

        // 从数据库中查询模拟接口是否存在，以及请求方法是否匹配
        InterfaceInfo interfaceInfo = innerInterfaceInfoService.getInterfaceInfoByUri(uri);
        if (interfaceInfo == null) {
            return handlerNoAuth(response);
        }
        // todo 去数据库中查看用户是否还具有目标接口的可调用次数


        // 5. 请求转发，调用模拟接口
        // 把查到的 用户id 和 接口id 保存在请求头中，如果接口调用成功就让他去执行增加次数的逻辑
        request = exchange.getRequest().mutate().header("userId", user.getId().toString()).header("interfaceInfoId", interfaceInfo.getId().toString()).build();
        //把新的 exchange放回到过滤链
        return chain.filter(exchange.mutate().request(request).build());
//        return chain.filter(exchange);
    }

    public Mono<Void> handlerNoAuth(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -3;
    }
}