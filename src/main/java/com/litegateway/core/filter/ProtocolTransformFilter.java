package com.litegateway.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * 协议转换过滤器
 * 支持 HTTP 到其他协议的转换
 */
@Component
public class ProtocolTransformFilter extends AbstractGatewayFilterFactory<ProtocolTransformFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(ProtocolTransformFilter.class);
    private final WebClient webClient;

    public ProtocolTransformFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClient = webClientBuilder.build();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // 检查是否需要协议转换
            if (config.getTargetProtocol() != null && !"http".equals(config.getTargetProtocol())) {
                logger.info("Transforming protocol to: {}", config.getTargetProtocol());
                return transformProtocol(exchange, config);
            }
            return chain.filter(exchange);
        };
    }

    private Mono<Void> transformProtocol(ServerWebExchange exchange, Config config) {
        // 这里实现协议转换逻辑
        // 例如：HTTP -> gRPC, HTTP -> WebSocket 等
        // 目前实现一个简单的示例，返回转换信息
        
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(
                        ("Protocol transformed to: " + config.getTargetProtocol()).getBytes()
                ))
        ).then(Mono.fromRunnable(() -> {
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
        }));
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("targetProtocol");
    }

    public static class Config {
        private String targetProtocol;

        public String getTargetProtocol() {
            return targetProtocol;
        }

        public void setTargetProtocol(String targetProtocol) {
            this.targetProtocol = targetProtocol;
        }
    }
}