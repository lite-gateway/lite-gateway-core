package com.litegateway.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 响应缓存过滤器
 * 用于缓存响应数据以提高性能
 */
@Component
public class ResponseCacheFilter extends AbstractGatewayFilterFactory<ResponseCacheFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(ResponseCacheFilter.class);

    // 缓存存储
    private final ConcurrentHashMap<String, CachedResponse> responseCache = new ConcurrentHashMap<>();

    public ResponseCacheFilter() {
        super(Config.class);
        // 启动清理过期缓存的线程
        startCleanupThread();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String cacheKey = generateCacheKey(request);
            
            // 检查缓存中是否存在响应
            CachedResponse cachedResponse = responseCache.get(cacheKey);
            if (cachedResponse != null && !cachedResponse.isExpired()) {
                logger.debug("Response cache hit for: {}", cacheKey);
                // 使用缓存的响应
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(cachedResponse.getStatusCode());
                response.getHeaders().putAll(cachedResponse.getHeaders());
                return response.writeWith(Flux.just(cachedResponse.getBody()));
            }
            
            // 缓存未命中，读取并缓存响应
            ServerHttpResponse originalResponse = exchange.getResponse();
            ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
                public Mono<Void> writeWith(Flux<DataBuffer> body) {
                    return DataBufferUtils.join(body)
                            .flatMap(dataBuffer -> {
                                // 复制响应体以便缓存
                                DataBuffer cachedBody = dataBuffer.factory().allocateBuffer(dataBuffer.readableByteCount());
                                cachedBody.write(dataBuffer);
                                dataBuffer.readPosition(0); // 重置读取位置
                                
                                // 缓存响应
                                CachedResponse newCachedResponse = new CachedResponse(
                                        originalResponse.getStatusCode(),
                                        originalResponse.getHeaders(),
                                        cachedBody,
                                        System.currentTimeMillis() + config.getCacheTtl() * 1000
                                );
                                responseCache.put(cacheKey, newCachedResponse);
                                logger.debug("Response cached for: {}", cacheKey);
                                
                                // 返回原始响应体
                                return super.writeWith(Flux.just(dataBuffer));
                            });
                }
            };
            
            return chain.filter(exchange.mutate().response(decoratedResponse).build());
        };
    }

    private String generateCacheKey(ServerHttpRequest request) {
        // 基于请求方法、路径和查询参数生成缓存键
        StringBuilder key = new StringBuilder();
        key.append(request.getMethod().name()).append("_")
           .append(request.getPath().value());
        
        if (!request.getQueryParams().isEmpty()) {
            key.append("_")
               .append(request.getQueryParams().toString());
        }
        
        return key.toString();
    }

    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // 每分钟清理一次
                    long currentTime = System.currentTimeMillis();
                    responseCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
                    logger.debug("Response cache cleanup completed, current size: {}", responseCache.size());
                } catch (InterruptedException e) {
                    logger.error("Cleanup thread interrupted: {}", e.getMessage());
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("cacheTtl");
    }

    public static class Config {
        private int cacheTtl = 60; // 默认缓存60秒

        public int getCacheTtl() {
            return cacheTtl;
        }

        public void setCacheTtl(int cacheTtl) {
            this.cacheTtl = cacheTtl;
        }
    }

    private static class CachedResponse {
        private final org.springframework.http.HttpStatusCode statusCode;
        private final org.springframework.http.HttpHeaders headers;
        private final DataBuffer body;
        private final long expirationTime;

        public CachedResponse(org.springframework.http.HttpStatusCode statusCode, 
                             org.springframework.http.HttpHeaders headers, 
                             DataBuffer body, 
                             long expirationTime) {
            this.statusCode = statusCode;
            this.headers = headers;
            this.body = body;
            this.expirationTime = expirationTime;
        }

        public org.springframework.http.HttpStatusCode getStatusCode() {
            return statusCode;
        }

        public org.springframework.http.HttpHeaders getHeaders() {
            return headers;
        }

        public DataBuffer getBody() {
            // 返回一个新的DataBuffer以避免多次读取
            DataBuffer newBuffer = body.factory().allocateBuffer(body.readableByteCount());
            body.readPosition(0); // 重置读取位置
            newBuffer.write(body);
            body.readPosition(0); // 再次重置读取位置
            return newBuffer;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
}
