package com.litegateway.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * WebSocket支持过滤器
 * 用于处理WebSocket连接
 */
@Component
public class WebSocketSupportFilter extends AbstractGatewayFilterFactory<WebSocketSupportFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketSupportFilter.class);

    public WebSocketSupportFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // 检查是否是WebSocket请求
            if (isWebSocketRequest(request)) {
                logger.debug("WebSocket request detected: {}", request.getPath().value());
                
                // 添加WebSocket相关的头信息
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("Upgrade", "websocket")
                        .header("Connection", "Upgrade")
                        .build();
                
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            }
            
            // 非WebSocket请求直接通过
            return chain.filter(exchange);
        };
    }

    private boolean isWebSocketRequest(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        String upgradeHeader = headers.getFirst("Upgrade");
        String connectionHeader = headers.getFirst("Connection");
        
        return "websocket".equalsIgnoreCase(upgradeHeader) && 
               connectionHeader != null && connectionHeader.toLowerCase().contains("upgrade");
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList();
    }

    public static class Config {
        // 可以添加WebSocket相关的配置参数
    }
}
