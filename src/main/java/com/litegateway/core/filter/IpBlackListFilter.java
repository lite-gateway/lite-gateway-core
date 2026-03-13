package com.litegateway.core.filter;

import com.litegateway.core.cache.IpListCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ipresolver.RemoteAddressResolver;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * IP 黑名单过滤器
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway.core
 */
@Component
public class IpBlackListFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(IpBlackListFilter.class);

    private final RemoteAddressResolver remoteAddressResolver = XForwardedRemoteAddressResolver.maxTrustedIndex(1);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
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
