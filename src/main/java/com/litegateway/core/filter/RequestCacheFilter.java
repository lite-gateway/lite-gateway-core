package com.litegateway.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 请求缓存过滤器
 * 用于缓存请求数据以提高性能
 */
@Component
public class RequestCacheFilter extends AbstractGatewayFilterFactory<RequestCacheFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(RequestCacheFilter.class);

    // 缓存存储
    private final ConcurrentHashMap<String, CachedRequest> requestCache = new ConcurrentHashMap<>();

    public RequestCacheFilter() {
        super(Config.class);
        // 启动清理过期缓存的线程
        startCleanupThread();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String cacheKey = generateCacheKey(request);
            
            // 检查缓存中是否存在请求
            CachedRequest cachedRequest = requestCache.get(cacheKey);
            if (cachedRequest != null && !cachedRequest.isExpired()) {
                logger.debug("Request cache hit for: {}", cacheKey);
                // 使用缓存的请求体
                ServerHttpRequestDecorator decoratedRequest = new ServerHttpRequestDecorator(request) {
                    @Override
                    public Flux<DataBuffer> getBody() {
                        return Flux.just(cachedRequest.getBody());
                    }
                };
                return chain.filter(exchange.mutate().request(decoratedRequest).build());
            }
            
            // 缓存未命中，读取并缓存请求体
            return DataBufferUtils.join(request.getBody())
                    .flatMap(dataBuffer -> {
                        // 复制请求体以便后续使用
                        DataBuffer cachedBody = dataBuffer.factory().allocateBuffer(dataBuffer.readableByteCount());
                        cachedBody.write(dataBuffer);
                        dataBuffer.readPosition(0); // 重置读取位置
                        
                        // 缓存请求
                        CachedRequest newCachedRequest = new CachedRequest(cachedBody, System.currentTimeMillis() + config.getCacheTtl() * 1000);
                        requestCache.put(cacheKey, newCachedRequest);
                        logger.debug("Request cached for: {}", cacheKey);
                        
                        // 装饰请求以使用原始请求体
                        ServerHttpRequestDecorator decoratedRequest = new ServerHttpRequestDecorator(request) {
                            @Override
                            public Flux<DataBuffer> getBody() {
                                return Flux.just(dataBuffer);
                            }
                        };
                        
                        return chain.filter(exchange.mutate().request(decoratedRequest).build());
                    });
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
                    requestCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
                    logger.debug("Request cache cleanup completed, current size: {}", requestCache.size());
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

    private static class CachedRequest {
        private final DataBuffer body;
        private final long expirationTime;

        public CachedRequest(DataBuffer body, long expirationTime) {
            this.body = body;
            this.expirationTime = expirationTime;
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
