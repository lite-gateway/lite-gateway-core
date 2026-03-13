package com.litegateway.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多租户隔离过滤器
 * 确保不同租户之间的资源隔离
 */
@Component
public class MultiTenantIsolationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(MultiTenantIsolationFilter.class);

    // 租户信息存储
    private final ConcurrentHashMap<String, TenantInfo> tenantMap = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // 从请求中提取租户信息
        String tenantId = extractTenantId(request);
        
        if (tenantId != null) {
            logger.info("Processing request for tenant: {}", tenantId);
            
            // 确保租户信息存在
            TenantInfo tenantInfo = tenantMap.computeIfAbsent(tenantId, id -> new TenantInfo(id));
            
            // 将租户信息添加到请求头中，以便后续过滤器使用
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-Tenant-Id", tenantId)
                    .build();
            
            // 为租户设置隔离的上下文
            exchange.getAttributes().put("tenantId", tenantId);
            exchange.getAttributes().put("tenantInfo", tenantInfo);
            
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        }
        
        // 没有租户信息的请求，使用默认租户
        logger.warn("No tenant information found in request");
        return chain.filter(exchange);
    }

    private String extractTenantId(ServerHttpRequest request) {
        // 从请求头中提取租户ID
        String tenantId = request.getHeaders().getFirst("X-Tenant-Id");
        
        // 如果请求头中没有，从请求参数中提取
        if (tenantId == null) {
            tenantId = request.getQueryParams().getFirst("tenantId");
        }
        
        // 如果请求参数中没有，从路径中提取
        if (tenantId == null) {
            String path = request.getURI().getPath();
            if (path.startsWith("/tenant/") && path.length() > 8) {
                int nextSlash = path.indexOf("/", 8);
                if (nextSlash != -1) {
                    tenantId = path.substring(8, nextSlash);
                } else {
                    tenantId = path.substring(8);
                }
            }
        }
        
        return tenantId;
    }

    @Override
    public int getOrder() {
        // 设置过滤器顺序，确保在其他过滤器之前执行
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    /**
     * 租户信息类
     */
    private static class TenantInfo {
        private final String tenantId;
        private long requestCount = 0;
        private long lastRequestTime = System.currentTimeMillis();

        public TenantInfo(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getTenantId() {
            return tenantId;
        }

        public long getRequestCount() {
            return requestCount;
        }

        public void incrementRequestCount() {
            this.requestCount++;
            this.lastRequestTime = System.currentTimeMillis();
        }

        public long getLastRequestTime() {
            return lastRequestTime;
        }
    }
}