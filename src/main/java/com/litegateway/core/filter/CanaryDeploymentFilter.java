package com.litegateway.core.filter;

import com.litegateway.core.dto.CanaryRuleDTO;
import com.litegateway.core.manager.GatewayFeatureManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 金丝雀发布过滤器（增强版）
 * 支持多种灰度策略：权重、Header、Cookie、Query参数、用户特征
 */
@Component
public class CanaryDeploymentFilter extends AbstractGatewayFilterFactory<CanaryDeploymentFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(CanaryDeploymentFilter.class);

    private final Random random = new Random();

    @Autowired
    private GatewayFeatureManager gatewayFeatureManager;

    public CanaryDeploymentFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            String routeId = route != null ? route.getId() : "";

            // 检查功能是否启用
            if (!gatewayFeatureManager.isFeatureEnabled("canary", routeId)) {
                logger.debug("Canary deployment is disabled for route: {}", routeId);
                return chain.filter(exchange);
            }

            // 获取该路由的灰度规则
            List<CanaryRuleDTO> rules = gatewayFeatureManager.getCanaryRules(routeId);

            // 确定目标版本
            String targetVersion = determineTargetVersion(exchange, rules, config);

            // 重写URI
            String rewrittenUri = rewriteUri(exchange.getRequest().getURI().toString(), targetVersion, config);

            try {
                URI uri = new URI(rewrittenUri);
                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .uri(uri)
                        .header("X-Version", targetVersion)
                        .header("X-Deployment-Type", "canary")
                        .build();

                logger.debug("Canary deployment: routing to {} version for route {}", targetVersion, routeId);

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } catch (URISyntaxException e) {
                logger.error("Invalid URI syntax: {}", rewrittenUri, e);
                return chain.filter(exchange);
            }
        };
    }

    private String determineTargetVersion(ServerWebExchange exchange, List<CanaryRuleDTO> rules, Config defaultConfig) {
        // 按优先级尝试匹配规则
        for (CanaryRuleDTO rule : rules) {
            if (!Boolean.TRUE.equals(rule.getEnabled())) {
                continue;
            }

            boolean matches = false;
            String matchType = rule.getMatchType();

            if (matchType == null) {
                matchType = "weight";
            }

            switch (matchType) {
                case "weight":
                    matches = matchesWeight(rule);
                    break;
                case "header":
                    matches = matchesHeader(exchange, rule);
                    break;
                case "cookie":
                    matches = matchesCookie(exchange, rule);
                    break;
                case "query":
                    matches = matchesQuery(exchange, rule);
                    break;
                case "user":
                    matches = matchesUser(exchange, rule);
                    break;
                default:
                    matches = matchesWeight(rule);
            }

            if (matches) {
                logger.debug("Matched canary rule: {} with type: {}", rule.getRuleId(), matchType);
                return rule.getCanaryVersion();
            }
        }

        // 默认使用权重策略
        return matchesWeight(defaultConfig) ? defaultConfig.getCanaryVersion() : defaultConfig.getStableVersion();
    }

    private boolean matchesWeight(CanaryRuleDTO rule) {
        int weight = rule.getCanaryWeight() != null ? rule.getCanaryWeight() : 10;
        int randomValue = random.nextInt(100);
        return randomValue < weight;
    }

    private boolean matchesWeight(Config config) {
        int randomValue = random.nextInt(100);
        return randomValue < config.getCanaryWeight();
    }

    private boolean matchesHeader(ServerWebExchange exchange, CanaryRuleDTO rule) {
        if (rule.getHeaderName() == null) {
            return false;
        }
        String headerValue = exchange.getRequest().getHeaders().getFirst(rule.getHeaderName());
        if (headerValue == null) {
            return false;
        }
        // 支持精确匹配或包含匹配
        if (rule.getHeaderValue() != null) {
            return rule.getHeaderValue().equals(headerValue) || headerValue.contains(rule.getHeaderValue());
        }
        return true;
    }

    private boolean matchesCookie(ServerWebExchange exchange, CanaryRuleDTO rule) {
        if (rule.getCookieName() == null) {
            return false;
        }
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(rule.getCookieName());
        if (cookie == null) {
            return false;
        }
        // 支持精确匹配或包含匹配
        if (rule.getHeaderValue() != null) {
            return rule.getHeaderValue().equals(cookie.getValue()) || cookie.getValue().contains(rule.getHeaderValue());
        }
        return true;
    }

    private boolean matchesQuery(ServerWebExchange exchange, CanaryRuleDTO rule) {
        if (rule.getQueryParam() == null) {
            return false;
        }
        String queryValue = exchange.getRequest().getQueryParams().getFirst(rule.getQueryParam());
        return queryValue != null;
    }

    private boolean matchesUser(ServerWebExchange exchange, CanaryRuleDTO rule) {
        // 基于用户特征的灰度
        // 可以从Header或Cookie中获取用户ID，然后根据用户ID进行哈希分流
        String userId = null;

        // 尝试从Header获取
        if (rule.getHeaderName() != null) {
            userId = exchange.getRequest().getHeaders().getFirst(rule.getHeaderName());
        }

        // 尝试从Cookie获取
        if (userId == null && rule.getCookieName() != null) {
            HttpCookie cookie = exchange.getRequest().getCookies().getFirst(rule.getCookieName());
            if (cookie != null) {
                userId = cookie.getValue();
            }
        }

        if (userId == null) {
            return false;
        }

        // 使用一致性哈希，确保同一个用户总是路由到相同版本
        int hash = hashUserId(userId);
        int weight = rule.getCanaryWeight() != null ? rule.getCanaryWeight() : 10;
        return (hash % 100) < weight;
    }

    private int hashUserId(String userId) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(userId.getBytes());
            return Math.abs(Arrays.hashCode(hash));
        } catch (NoSuchAlgorithmException e) {
            return Math.abs(userId.hashCode());
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
