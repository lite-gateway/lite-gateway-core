package com.litegateway.core.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 基于请求内容的路由过滤器
 * 用于根据请求体或参数进行路由决策
 */
@Component
public class ContentBasedRoutingFilter extends AbstractGatewayFilterFactory<ContentBasedRoutingFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(ContentBasedRoutingFilter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ContentBasedRoutingFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // 检查是否是POST或PUT请求
            if (request.getMethod() == HttpMethod.POST || request.getMethod() == HttpMethod.PUT) {
                // 读取请求体
                return DataBufferUtils.join(request.getBody())
                        .flatMap(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            DataBufferUtils.release(dataBuffer);
                            
                            try {
                                // 解析JSON请求体
                                Map<String, Object> requestBody = objectMapper.readValue(bytes, Map.class);
                                
                                // 检查是否包含指定的键值对
                                if (requestBody.containsKey(config.getKey()) && 
                                    config.getValue().equals(requestBody.get(config.getKey()))) {
                                    logger.debug("Content-based routing matched: {}={}", config.getKey(), config.getValue());
                                    // 重写请求URI到目标服务
                                    try {
                                        URI uri = new URI("http", null, config.getTargetHost(), config.getTargetPort(), request.getPath().value(), request.getURI().getQuery(), null);
                                        ServerHttpRequest modifiedRequest = request.mutate()
                                                .uri(uri)
                                                .build();
                                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                                    } catch (URISyntaxException e) {
                                        logger.error("Invalid URI syntax", e);
                                        return chain.filter(exchange);
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("Error processing request body: {}", e.getMessage());
                            }
                            
                            // 不匹配时继续原始路由
                            return chain.filter(exchange);
                        });
            } else {
                // 对于GET请求，检查查询参数
                String paramValue = request.getQueryParams().getFirst(config.getKey());
                if (config.getValue().equals(paramValue)) {
                    logger.debug("Query parameter routing matched: {}={}", config.getKey(), config.getValue());
                    // 重写请求URI到目标服务
                    try {
                        URI uri = new URI("http", null, config.getTargetHost(), config.getTargetPort(), request.getPath().value(), request.getURI().getQuery(), null);
                        ServerHttpRequest modifiedRequest = request.mutate()
                                .uri(uri)
                                .build();
                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    } catch (URISyntaxException e) {
                        logger.error("Invalid URI syntax", e);
                        return chain.filter(exchange);
                    }
                }
                return chain.filter(exchange);
            }
        };
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("key", "value", "targetHost", "targetPort");
    }

    public static class Config {
        private String key;
        private String value;
        private String targetHost = "localhost";
        private int targetPort = 8080;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getTargetHost() {
            return targetHost;
        }

        public void setTargetHost(String targetHost) {
            this.targetHost = targetHost;
        }

        public int getTargetPort() {
            return targetPort;
        }

        public void setTargetPort(int targetPort) {
            this.targetPort = targetPort;
        }
    }
}
