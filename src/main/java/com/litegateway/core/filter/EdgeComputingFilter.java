package com.litegateway.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.reactivestreams.Publisher;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 边缘计算过滤器
 * 在网关层进行简单的计算处理
 */
@Component
public class EdgeComputingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(EdgeComputingFilter.class);

    // 简单的内存缓存，用于存储计算结果
    private final ConcurrentHashMap<String, Object> computationCache = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // 检查是否需要边缘计算
        if (request.getHeaders().containsKey("X-Edge-Compute")) {
            String computeType = request.getHeaders().getFirst("X-Edge-Compute");
            logger.info("Edge computing requested: {}", computeType);
            
            // 根据计算类型执行不同的计算
            switch (computeType) {
                case "data-aggregation":
                    return handleDataAggregation(exchange, chain);
                case "data-transformation":
                    return handleDataTransformation(exchange, chain);
                case "cache-lookup":
                    return handleCacheLookup(exchange, chain);
                default:
                    logger.warn("Unknown edge compute type: {}", computeType);
            }
        }
        
        return chain.filter(exchange);
    }

    private Mono<Void> handleDataAggregation(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 数据聚合处理
        logger.info("Performing data aggregation...");
        
        // 这里可以实现数据聚合逻辑
        // 例如：合并多个请求的结果，计算统计数据等
        
        return chain.filter(exchange);
    }

    private Mono<Void> handleDataTransformation(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 数据转换处理
        logger.info("Performing data transformation...");
        
        ServerHttpRequest request = exchange.getRequest();
        
        if (request.getMethod() == HttpMethod.POST || request.getMethod() == HttpMethod.PUT) {
            ServerHttpRequestDecorator decoratedRequest = new ServerHttpRequestDecorator(request) {
                @Override
                public Flux<DataBuffer> getBody() {
                    return super.getBody()
                            .flatMap(dataBuffer -> {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                DataBufferUtils.release(dataBuffer);
                                
                                String body = new String(bytes, StandardCharsets.UTF_8);
                                String transformedBody = transformData(body);
                                
                                logger.debug("Original body: {}", body);
                                logger.debug("Transformed body: {}", transformedBody);
                                
                                return Flux.just(exchange.getResponse().bufferFactory().wrap(transformedBody.getBytes(StandardCharsets.UTF_8)));
                            });
                }
            };
            
            return chain.filter(exchange.mutate().request(decoratedRequest).build());
        }
        
        return chain.filter(exchange);
    }

    private Mono<Void> handleCacheLookup(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 缓存查找处理
        logger.info("Performing cache lookup...");
        
        String cacheKey = exchange.getRequest().getURI().toString();
        Object cachedResult = computationCache.get(cacheKey);
        
        if (cachedResult != null) {
            logger.info("Cache hit for key: {}", cacheKey);
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return response.writeWith(
                    Flux.just(response.bufferFactory().wrap(cachedResult.toString().getBytes(StandardCharsets.UTF_8)))
            );
        } else {
            logger.info("Cache miss for key: {}", cacheKey);
            
            ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(exchange.getResponse()) {
                @Override
                public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                    return super.writeWith(Flux.from(body)
                            .flatMap(dataBuffer -> {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                DataBufferUtils.release(dataBuffer);
                                
                                String responseBody = new String(bytes, StandardCharsets.UTF_8);
                                // 缓存响应结果
                                computationCache.put(cacheKey, responseBody);
                                logger.info("Cached response for key: {}", cacheKey);
                                
                                return Flux.just(getDelegate().bufferFactory().wrap(bytes));
                            }));
                }
            };
            
            return chain.filter(exchange.mutate().response(decoratedResponse).build());
        }
    }

    private String transformData(String data) {
        // 简单的数据转换示例
        // 这里可以根据实际需求实现更复杂的数据转换逻辑
        try {
            // 假设数据是 JSON 格式
            // 这里只是一个简单的示例，实际应用中可能需要使用 JSON 解析库
            if (data.contains("temperature")) {
                // 将摄氏度转换为华氏度
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\"temperature\":)(\\d+\\.\\d+)");
                java.util.regex.Matcher matcher = pattern.matcher(data);
                StringBuffer sb = new StringBuffer();
                while (matcher.find()) {
                    double celsius = Double.parseDouble(matcher.group(2));
                    double fahrenheit = celsius * 9/5 + 32;
                    matcher.appendReplacement(sb, matcher.group(1) + String.format("%.2f", fahrenheit));
                }
                matcher.appendTail(sb);
                data = sb.toString();
            }
        } catch (Exception e) {
            logger.error("Error transforming data: {}", e.getMessage());
        }
        return data;
    }

    @Override
    public int getOrder() {
        // 设置过滤器顺序，确保在其他过滤器之后执行
        return Ordered.LOWEST_PRECEDENCE;
    }
}