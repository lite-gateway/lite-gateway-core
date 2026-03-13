package com.litegateway.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * gRPC支持过滤器
 * 用于处理gRPC请求
 */
@Component
public class GrpcSupportFilter extends AbstractGatewayFilterFactory<GrpcSupportFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(GrpcSupportFilter.class);

    public GrpcSupportFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // 检查是否是gRPC请求
            if (isGrpcRequest(request)) {
                logger.debug("gRPC request detected: {}", request.getPath().value());
                
                // 添加gRPC相关的头信息
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("Content-Type", "application/grpc")
                        .build();
                
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            }
            
            // 非gRPC请求直接通过
            return chain.filter(exchange);
        };
    }

    private boolean isGrpcRequest(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        String contentType = headers.getFirst("Content-Type");
        
        return contentType != null && contentType.startsWith("application/grpc");
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList();
    }

    public static class Config {
        // 可以添加gRPC相关的配置参数
    }
}
