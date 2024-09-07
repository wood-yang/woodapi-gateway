package com.wood.woodapi.global_filter;

import com.wood.common.model.entity.InterfaceInfo;
import com.wood.common.model.entity.User;
import com.wood.common.service.InnerInterfaceInfoService;
import com.wood.common.service.InnerUserInterfaceInfoService;
import com.wood.common.service.InnerUserService;
import com.wood.woodapiclientsdk.utils.SignUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

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

    @DubboReference
    private InnerUserInterfaceInfoService innerUserInterfaceInfoService;

    private final static List<String> IP_WHITE_LIST = Arrays.asList("127.0.0.1", "116.196.68.160", "localhost");

    private static final String EXTRA_BODY = "userInfoWoodAPI";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 日志
        log.info("global request filter");
//        if (true) {
//            //把新的 exchange放回到过滤链
//            log.info("调用接口");
//            return chain.filter(exchange);
//        }
        ServerHttpRequest request = exchange.getRequest();
        // 2. (黑白名单)
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        ServerHttpResponse response = exchange.getResponse();
        if (remoteAddress == null) {
            return handlerNoAuth(response);
        }
        String hostName = remoteAddress.getHostName();
        String uri = request.getURI().toString();
        log.info("hostName: " + hostName);
        log.info("uri: " + uri);
        if (!IP_WHITE_LIST.contains(hostName)) {
            log.info("非白名单用户");
            return handlerNoAuth(response);
        }
        log.info("白名单用户");
        // 3. 用户鉴权(判断 ak、 sk 是否合法)
        HttpHeaders httpHeaders = request.getHeaders();
        String accessKey = httpHeaders.getFirst("accessKey");
        String nonce = httpHeaders.getFirst("nonce");
        String timestamp = httpHeaders.getFirst("timestamp");
        String sign = httpHeaders.getFirst("sign");
        String body = httpHeaders.getFirst("body");
        // 去数据库中查看用户是否具有accessKey
        log.info("数据库查询accessKey");
        User user = innerUserService.getUserByAccessKey(accessKey);
        if (user == null || user.getAccessKey() == null) {
            log.info("不存在该用户或用户没有AccessKey");
            return handlerNoAuth(response);
        }
        if (nonce == null || Long.parseLong(nonce) > 10000) {
            return handlerNoAuth(response);
        }
        log.info("accessKey存在");
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
        log.info("鉴权通过");

        // 从数据库中查询模拟接口是否存在，以及请求方法是否匹配
        String[] split = uri.split("\\?");
        uri = split[0];
        InterfaceInfo interfaceInfo = innerInterfaceInfoService.getInterfaceInfoByUri(uri);
        if (interfaceInfo == null) {
            return handlerNoAuth(response);
        }
        log.info("接口存在");
        // 去数据库中查看用户是否还具有目标接口的可调用次数
        Long userId = user.getId();
        Long interfaceInfoId = interfaceInfo.getId();
//        if (!innerUserInterfaceInfoService.isEnough(userId, interfaceInfoId)) {
//            log.info("用户接口可调用次数不足");
//            return handlerNoAuth(response);
//        }
        // 5. 请求转发，调用模拟接口
        // 把查到的 用户id 和 接口id 保存在请求头中，如果接口调用成功就让他去执行增加次数的逻辑
//        request = exchange.getRequest().mutate().header("userId", userId.toString()).header("interfaceInfoId", interfaceInfoId.toString()).build();
        //把新的 exchange放回到过滤链
        log.info("调用接口");
//        return chain.filter(exchange.mutate().request(request).build()).then(Mono.just(exchange))
        return chain.filter(exchange).then(Mono.just(exchange))
                .map(serverWebExchange -> {
                    log.info("global response filter");
//                    if (true) {
//                        ServerHttpResponse serverHttpResponse = serverWebExchange.getResponse();
//                        return serverWebExchange;
//                    }
                    ServerHttpResponse serverHttpResponse = exchange.getResponse();
                    // 6. 响应日志
                    HttpStatus statusCode = serverHttpResponse.getStatusCode();
                    log.info("statusCode: " + statusCode);
                    // 7. 调用成功，接口调用次数 +1 invokeCount
                    // todo 怎么定义失败，一个就是根本就发过去，或者其他异常，也有可能是业务问题 出bug了 导致虽然OK 200 了 但是数据是不对的
                    // 8. 调用失败，返回一个规范的错误码
                    if (statusCode == HttpStatus.OK) {
//                        ServerHttpRequest serverHttpRequest = exchange.getRequest();
//                        HttpHeaders headers = serverHttpRequest.getHeaders();
//                        String userId = headers.getFirst("userId");
//                        String interfaceInfoId = headers.getFirst("interfaceInfoId");
//                        innerUserInterfaceInfoService.invokeCount(Long.parseLong(userId), Long.parseLong(interfaceInfoId));
                        innerUserInterfaceInfoService.invokeCount(userId, interfaceInfoId);
                        log.info("统计次数完成");
                    } else {
                        serverHttpResponse.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                    return serverWebExchange;
                })
                .then();
    }

    public Mono<Void> handlerNoAuth(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}