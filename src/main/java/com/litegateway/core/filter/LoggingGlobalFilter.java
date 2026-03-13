package com.litegateway.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * 全局日志记录过滤器
 * 记录请求和响应的详细信息
 */
@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(LoggingGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 记录请求开始时间
        Instant startTime = Instant.now();
        ServerHttpRequest request = exchange.getRequest();
        
        // 记录请求信息
        logRequest(request);
        
        // 继续处理请求
        return chain.filter(exchange)
                .doFinally(signalType -> {
                    // 记录响应信息和处理时间
                    Instant endTime = Instant.now();
                    long duration = Duration.between(startTime, endTime).toMillis();
                    ServerHttpResponse response = exchange.getResponse();
                    logResponse(response, duration);
                });
    }

    private void logRequest(ServerHttpRequest request) {
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String query = request.getURI().getQuery();
        Map<String, String> headers = request.getHeaders().toSingleValueMap();
        
        logger.info("Request: {} {}?{}", method, path, query);
        logger.debug("Request Headers: {}", headers);
    }

    private void logResponse(ServerHttpResponse response, long duration) {
        int statusCode = response.getStatusCode().value();
        Map<String, String> headers = response.getHeaders().toSingleValueMap();
        
        logger.info("Response: {} ({}ms)", statusCode, duration);
        logger.debug("Response Headers: {}", headers);
    }

    @Override
    public int getOrder() {
        // 设置过滤器顺序，确保在其他过滤器之前执行
        return Ordered.HIGHEST_PRECEDENCE;
    }
}