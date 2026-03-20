package com.litegateway.core.filter;

import com.litegateway.core.cache.IpListCache;
import com.litegateway.core.manager.GatewayFeatureManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.gateway.support.ipresolver.RemoteAddressResolver;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * IP 黑名单过滤器（可配置版）
 * 支持动态开关
 */
@Component
public class IpBlackListFilter implements GlobalFilter, Ordered, ConfigurableFilter {

    private static final Logger logger = LoggerFactory.getLogger(IpBlackListFilter.class);

    private static final String FEATURE_CODE = "ip_blacklist";

    private final RemoteAddressResolver remoteAddressResolver = XForwardedRemoteAddressResolver.maxTrustedIndex(1);

    @Autowired
    private GatewayFeatureManager gatewayFeatureManager;

    @Override
    public String getFeatureCode() {
        return FEATURE_CODE;
    }

    @Override
    public int getPriority() {
        return 20;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取当前路由
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = route != null ? route.getId() : "default";

        // 检查功能是否启用
        if (!gatewayFeatureManager.isFeatureEnabled(FEATURE_CODE, routeId)) {
            logger.debug("IP blacklist check is disabled for route: {}", routeId);
            return chain.filter(exchange);
        }

        try {
            InetSocketAddress remoteAddress = remoteAddressResolver.resolve(exchange);
            String clientIp = remoteAddress.getAddress().getHostAddress();

            if (IpListCache.contains(clientIp)) {
                logger.warn("Blocked request from blacklisted IP: {}", clientIp);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        } catch (Exception e) {
            logger.error("IpBlackListFilter error: {}", e.getMessage());
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 最高优先级，最先执行
        return -1;
    }
}
