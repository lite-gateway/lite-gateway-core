package com.litegateway.core.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 可配置过滤器接口
 * 支持动态开关和配置的过滤器应实现此接口
 */
public interface ConfigurableFilter {

    /**
     * 获取功能编码
     *
     * @return 功能编码
     */
    String getFeatureCode();

    /**
     * 获取优先级
     *
     * @return 优先级数值，越小优先级越高
     */
    int getPriority();

    /**
     * 执行过滤逻辑
     *
     * @param exchange ServerWebExchange
     * @param chain    GatewayFilterChain
     * @return Mono<Void>
     */
    Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain);
}
