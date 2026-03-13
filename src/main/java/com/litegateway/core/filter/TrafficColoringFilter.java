package com.litegateway.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 流量染色过滤器
 * 用于为请求添加标记以便跟踪和分析
 */
@Component
public class TrafficColoringFilter extends AbstractGatewayFilterFactory<TrafficColoringFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(TrafficColoringFilter.class);

    public TrafficColoringFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // 为请求添加染色标记
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-Traffic-Color", config.getColor())
                    .header("X-Traffic-Trace-Id", generateTraceId())
                    .header("X-Traffic-Source", config.getSource())
                    .header("X-Traffic-Environment", config.getEnvironment())
                    .build();
            
            logger.debug("Traffic colored: color={}, source={}, environment={}", 
                        config.getColor(), config.getSource(), config.getEnvironment());
            
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("color", "source", "environment");
    }

    public static class Config {
        private String color = "default";
        private String source = "gateway";
        private String environment = "production";

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getEnvironment() {
            return environment;
        }

        public void setEnvironment(String environment) {
            this.environment = environment;
        }
    }
}
