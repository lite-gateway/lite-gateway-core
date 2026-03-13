package com.litegateway.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * GraphQL支持过滤器
 * 用于处理GraphQL请求
 */
@Component
public class GraphQLSupportFilter extends AbstractGatewayFilterFactory<GraphQLSupportFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(GraphQLSupportFilter.class);

    public GraphQLSupportFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // 检查是否是GraphQL请求
            if (isGraphQLRequest(request)) {
                logger.debug("GraphQL request detected: {}", request.getPath().value());
                
                // 读取请求体以验证GraphQL格式
                return DataBufferUtils.join(request.getBody())
                        .flatMap(dataBuffer -> {
                            // 复制请求体以便后续使用
                            DataBuffer cachedBody = dataBuffer.factory().allocateBuffer(dataBuffer.readableByteCount());
                            cachedBody.write(dataBuffer);
                            dataBuffer.readPosition(0); // 重置读取位置
                            
                            // 装饰请求以使用原始请求体
                            ServerHttpRequestDecorator decoratedRequest = new ServerHttpRequestDecorator(request) {
                                @Override
                                public Flux<DataBuffer> getBody() {
                                    return Flux.just(dataBuffer);
                                }
                            };
                            
                            return chain.filter(exchange.mutate().request(decoratedRequest).build());
                        });
            }
            
            // 非GraphQL请求直接通过
            return chain.filter(exchange);
        };
    }

    private boolean isGraphQLRequest(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        String contentType = headers.getFirst("Content-Type");
        
        // 检查Content-Type是否为GraphQL
        if (contentType != null && (contentType.equals("application/graphql") || 
            contentType.equals("application/json"))) {
            // 对于application/json，需要检查请求体是否包含GraphQL查询
            // 这里简化处理，仅基于Content-Type判断
            return true;
        }
        
        // 检查路径是否包含/graphql
        return request.getPath().value().contains("/graphql");
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList();
    }

    public static class Config {
        // 可以添加GraphQL相关的配置参数
    }
}
