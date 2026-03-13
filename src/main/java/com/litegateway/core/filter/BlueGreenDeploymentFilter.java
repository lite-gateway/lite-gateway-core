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
 * 蓝绿部署过滤器
 * 用于在蓝绿部署过程中控制流量的切换
 */
@Component
public class BlueGreenDeploymentFilter extends AbstractGatewayFilterFactory<BlueGreenDeploymentFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(BlueGreenDeploymentFilter.class);

    public BlueGreenDeploymentFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String targetEnvironment = config.getActiveEnvironment();
            
            logger.debug("Blue-green deployment: routing to {} environment", targetEnvironment);
            
            // 根据活动环境重写请求URI
            String rewrittenUri = rewriteUri(request.getURI().toString(), targetEnvironment, config);
            
            try {
                URI uri = new URI(rewrittenUri);
                ServerHttpRequest modifiedRequest = request.mutate()
                        .uri(uri)
                        .header("X-Environment", targetEnvironment)
                        .header("X-Deployment-Type", "blue-green")
                        .build();
                
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } catch (URISyntaxException e) {
                logger.error("Invalid URI syntax: {}", rewrittenUri, e);
                return chain.filter(exchange);
            }
        };
    }

    private String rewriteUri(String originalUri, String environment, Config config) {
        // 根据环境重写URI
        if ("blue".equals(environment)) {
            return originalUri.replace(config.getServiceBaseUrl(), config.getBlueEnvironmentUrl());
        } else if ("green".equals(environment)) {
            return originalUri.replace(config.getServiceBaseUrl(), config.getGreenEnvironmentUrl());
        }
        return originalUri;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("activeEnvironment", "serviceBaseUrl", "blueEnvironmentUrl", "greenEnvironmentUrl");
    }

    public static class Config {
        private String activeEnvironment = "blue"; // 默认活动环境
        private String serviceBaseUrl;
        private String blueEnvironmentUrl;
        private String greenEnvironmentUrl;

        public String getActiveEnvironment() {
            return activeEnvironment;
        }

        public void setActiveEnvironment(String activeEnvironment) {
            this.activeEnvironment = activeEnvironment;
        }

        public String getServiceBaseUrl() {
            return serviceBaseUrl;
        }

        public void setServiceBaseUrl(String serviceBaseUrl) {
            this.serviceBaseUrl = serviceBaseUrl;
        }

        public String getBlueEnvironmentUrl() {
            return blueEnvironmentUrl;
        }

        public void setBlueEnvironmentUrl(String blueEnvironmentUrl) {
            this.blueEnvironmentUrl = blueEnvironmentUrl;
        }

        public String getGreenEnvironmentUrl() {
            return greenEnvironmentUrl;
        }

        public void setGreenEnvironmentUrl(String greenEnvironmentUrl) {
            this.greenEnvironmentUrl = greenEnvironmentUrl;
        }
    }
}
