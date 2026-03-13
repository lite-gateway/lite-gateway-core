package com.litegateway.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * 路由配置类
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway.core
 * 配置限流相关的 KeyResolver
 */
@Slf4j
@Configuration
public class RouteConfig {

    /**
     * 基于 IP 地址的限流 KeyResolver
     */
    @Primary
    @Bean("hostAddrKeyResolver")
    public KeyResolver hostAddrKeyResolver() {
        return exchange -> {
            String hostAddress = "unknown";
            if (exchange.getRequest().getRemoteAddress() != null
                    && exchange.getRequest().getRemoteAddress().getAddress() != null) {
                hostAddress = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            }
            return Mono.just(hostAddress);
        };
    }

    /**
     * 基于 URI 的限流 KeyResolver
     */
    @Bean("uriKeyResolver")
    public KeyResolver uriKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getURI().getPath());
    }

    /**
     * 基于请求参数的限流 KeyResolver
     */
    @Bean("requestIdKeyResolver")
    public KeyResolver requestIdKeyResolver() {
        return exchange -> {
            String requestId = exchange.getRequest().getQueryParams().getFirst("requestId");
            return Mono.just(requestId != null ? requestId : "default");
        };
    }

    /**
     * 创建自定义限流器
     * @param replenishRate 每秒补充令牌数
     * @param burstCapacity 令牌桶容量
     * @return RedisRateLimiter
     */
    public static RedisRateLimiter createRateLimiter(int replenishRate, int burstCapacity) {
        return new RedisRateLimiter(replenishRate, burstCapacity);
    }
}
