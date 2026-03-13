package com.litegateway.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

/**
 * 条件路由过滤器
 * 用于根据请求的各种条件进行路由决策
 */
@Component
public class ConditionalRoutingFilter extends AbstractGatewayFilterFactory<ConditionalRoutingFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(ConditionalRoutingFilter.class);

    public ConditionalRoutingFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // 检查条件
            boolean conditionMet = false;
            
            switch (config.getConditionType()) {
                case "header":
                    conditionMet = checkHeaderCondition(request, config);
                    break;
                case "query":
                    conditionMet = checkQueryCondition(request, config);
                    break;
                case "method":
                    conditionMet = checkMethodCondition(request, config);
                    break;
                case "path":
                    conditionMet = checkPathCondition(request, config);
                    break;
                default:
                    logger.warn("Unknown condition type: {}", config.getConditionType());
                    break;
            }
            
            if (conditionMet) {
                logger.debug("Conditional routing matched: {} {}", config.getConditionType(), config.getConditionValue());
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
            
            // 不匹配时继续原始路由
            return chain.filter(exchange);
        };
    }

    private boolean checkHeaderCondition(ServerHttpRequest request, Config config) {
        String headerValue = request.getHeaders().getFirst(config.getConditionKey());
        return config.getConditionValue().equals(headerValue);
    }

    private boolean checkQueryCondition(ServerHttpRequest request, Config config) {
        String queryValue = request.getQueryParams().getFirst(config.getConditionKey());
        return config.getConditionValue().equals(queryValue);
    }

    private boolean checkMethodCondition(ServerHttpRequest request, Config config) {
        return config.getConditionValue().equals(request.getMethod().name());
    }

    private boolean checkPathCondition(ServerHttpRequest request, Config config) {
        return request.getPath().value().contains(config.getConditionValue());
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("conditionType", "conditionKey", "conditionValue", "targetHost", "targetPort");
    }

    public static class Config {
        private String conditionType; // header, query, method, path
        private String conditionKey;
        private String conditionValue;
        private String targetHost = "localhost";
        private int targetPort = 8080;

        public String getConditionType() {
            return conditionType;
        }

        public void setConditionType(String conditionType) {
            this.conditionType = conditionType;
        }

        public String getConditionKey() {
            return conditionKey;
        }

        public void setConditionKey(String conditionKey) {
            this.conditionKey = conditionKey;
        }

        public String getConditionValue() {
            return conditionValue;
        }

        public void setConditionValue(String conditionValue) {
            this.conditionValue = conditionValue;
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
