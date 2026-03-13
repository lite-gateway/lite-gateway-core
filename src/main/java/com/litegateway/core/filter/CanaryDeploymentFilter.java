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
import java.util.Random;

/**
 * 金丝雀发布过滤器
 * 用于在金丝雀发布过程中控制流量的分配
 */
@Component
public class CanaryDeploymentFilter extends AbstractGatewayFilterFactory<CanaryDeploymentFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(CanaryDeploymentFilter.class);
    private final Random random = new Random();

    public CanaryDeploymentFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String targetVersion = determineTargetVersion(config);
            
            logger.debug("Canary deployment: routing to {} version", targetVersion);
            
            // 根据目标版本重写请求URI
            String rewrittenUri = rewriteUri(request.getURI().toString(), targetVersion, config);
            
            try {
                URI uri = new URI(rewrittenUri);
                ServerHttpRequest modifiedRequest = request.mutate()
                        .uri(uri)
                        .header("X-Version", targetVersion)
                        .header("X-Deployment-Type", "canary")
                        .build();
                
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } catch (URISyntaxException e) {
                logger.error("Invalid URI syntax: {}", rewrittenUri, e);
                return chain.filter(exchange);
            }
        };
    }

    private String determineTargetVersion(Config config) {
        // 根据权重决定目标版本
        int randomValue = random.nextInt(100);
        if (randomValue < config.getCanaryWeight()) {
            return config.getCanaryVersion();
        } else {
            return config.getStableVersion();
        }
    }

    private String rewriteUri(String originalUri, String version, Config config) {
        // 根据版本重写URI
        if (config.getCanaryVersion().equals(version)) {
            return originalUri.replace(config.getServiceBaseUrl(), config.getCanaryServiceUrl());
        } else {
            return originalUri.replace(config.getServiceBaseUrl(), config.getStableServiceUrl());
        }
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("canaryWeight", "canaryVersion", "stableVersion", "serviceBaseUrl", "canaryServiceUrl", "stableServiceUrl");
    }

    public static class Config {
        private int canaryWeight = 10; // 默认10%流量到金丝雀版本
        private String canaryVersion = "v2";
        private String stableVersion = "v1";
        private String serviceBaseUrl;
        private String canaryServiceUrl;
        private String stableServiceUrl;

        public int getCanaryWeight() {
            return canaryWeight;
        }

        public void setCanaryWeight(int canaryWeight) {
            this.canaryWeight = canaryWeight;
        }

        public String getCanaryVersion() {
            return canaryVersion;
        }

        public void setCanaryVersion(String canaryVersion) {
            this.canaryVersion = canaryVersion;
        }

        public String getStableVersion() {
            return stableVersion;
        }

        public void setStableVersion(String stableVersion) {
            this.stableVersion = stableVersion;
        }

        public String getServiceBaseUrl() {
            return serviceBaseUrl;
        }

        public void setServiceBaseUrl(String serviceBaseUrl) {
            this.serviceBaseUrl = serviceBaseUrl;
        }

        public String getCanaryServiceUrl() {
            return canaryServiceUrl;
        }

        public void setCanaryServiceUrl(String canaryServiceUrl) {
            this.canaryServiceUrl = canaryServiceUrl;
        }

        public String getStableServiceUrl() {
            return stableServiceUrl;
        }

        public void setStableServiceUrl(String stableServiceUrl) {
            this.stableServiceUrl = stableServiceUrl;
        }
    }
}
