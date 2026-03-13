package com.litegateway.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API密钥认证过滤器
 * 用于验证请求中的API密钥
 */
@Component
public class ApiKeyAuthenticationFilter extends AbstractGatewayFilterFactory<ApiKeyAuthenticationFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

    // 存储有效的API密钥
    private final Map<String, String> validApiKeys = new ConcurrentHashMap<>();

    public ApiKeyAuthenticationFilter() {
        super(Config.class);
        // 初始化示例API密钥
        validApiKeys.put("api-key-123", "service-a");
        validApiKeys.put("api-key-456", "service-b");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // 从请求头获取API密钥
            String apiKey = request.getHeaders().getFirst(config.getHeaderName());
            
            if (apiKey == null || !validApiKeys.containsKey(apiKey)) {
                logger.warn("Invalid API key: {}", apiKey);
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                response.getHeaders().add("Content-Type", "application/json");
                
                String errorMessage = "{\"error\": \"Unauthorized\", \"message\": \"Invalid or missing API key\"}";
                DataBuffer buffer = response.bufferFactory().wrap(errorMessage.getBytes());
                return response.writeWith(Mono.just(buffer));
            }
            
            logger.debug("Valid API key for service: {}", validApiKeys.get(apiKey));
            // 添加服务标识到请求头
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-Service-Id", validApiKeys.get(apiKey))
                    .build();
            
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("headerName");
    }

    public static class Config {
        private String headerName = "X-API-Key";

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }
    }
}
