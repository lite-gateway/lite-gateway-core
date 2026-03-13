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
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 流量控制过滤器
 * 用于控制请求的流量，比如限制并发请求数、控制请求速率等
 */
@Component
public class TrafficControlFilter extends AbstractGatewayFilterFactory<TrafficControlFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(TrafficControlFilter.class);

    // 并发请求控制
    private final Semaphore concurrencySemaphore;
    // 请求速率控制
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private long lastSecondTimestamp = System.currentTimeMillis();

    public TrafficControlFilter() {
        super(Config.class);
        // 默认并发限制为100
        this.concurrencySemaphore = new Semaphore(100);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // 检查并发限制
            if (!concurrencySemaphore.tryAcquire()) {
                logger.warn("Concurrent request limit exceeded");
                return handleTooManyRequests(exchange);
            }
            
            // 检查速率限制
            if (!checkRateLimit(config.getRateLimitPerSecond())) {
                logger.warn("Rate limit exceeded");
                return handleTooManyRequests(exchange);
            }
            
            return chain.filter(exchange).doFinally(signalType -> {
                // 释放并发信号量
                concurrencySemaphore.release();
            });
        };
    }

    private boolean checkRateLimit(int rateLimitPerSecond) {
        long currentTime = System.currentTimeMillis();
        
        // 每秒重置计数器
        if (currentTime - lastSecondTimestamp > 1000) {
            requestCount.set(0);
            lastSecondTimestamp = currentTime;
        }
        
        // 检查是否超过速率限制
        return requestCount.incrementAndGet() <= rateLimitPerSecond;
    }

    private Mono<Void> handleTooManyRequests(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", "application/json");
        
        String errorMessage = "{\"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded\"}";
        DataBuffer buffer = response.bufferFactory().wrap(errorMessage.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("rateLimitPerSecond", "concurrencyLimit");
    }

    public static class Config {
        private int rateLimitPerSecond = 100; // 默认每秒100个请求
        private int concurrencyLimit = 100; // 默认并发100个请求

        public int getRateLimitPerSecond() {
            return rateLimitPerSecond;
        }

        public void setRateLimitPerSecond(int rateLimitPerSecond) {
            this.rateLimitPerSecond = rateLimitPerSecond;
        }

        public int getConcurrencyLimit() {
            return concurrencyLimit;
        }

        public void setConcurrencyLimit(int concurrencyLimit) {
            this.concurrencyLimit = concurrencyLimit;
        }
    }
}
