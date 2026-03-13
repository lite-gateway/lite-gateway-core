package com.litegateway.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 细粒度权限控制过滤器
 * 用于检查用户是否具有访问资源的权限
 */
@Component
public class FineGrainedPermissionFilter extends AbstractGatewayFilterFactory<FineGrainedPermissionFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(FineGrainedPermissionFilter.class);

    // 存储用户权限映射
    private final Map<String, List<String>> userPermissions = new ConcurrentHashMap<>();

    public FineGrainedPermissionFilter() {
        super(Config.class);
        // 初始化示例权限
        userPermissions.put("user1", Arrays.asList("read:resource", "write:resource"));
        userPermissions.put("user2", Arrays.asList("read:resource"));
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // 从请求头获取用户信息
            String userId = request.getHeaders().getFirst("X-User-Id");
            
            if (userId == null) {
                logger.warn("User ID not found in request");
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                response.getHeaders().add("Content-Type", "application/json");
                
                String errorMessage = "{\"error\": \"Unauthorized\", \"message\": \"User not authenticated\"}";
                DataBuffer buffer = response.bufferFactory().wrap(errorMessage.getBytes());
                return response.writeWith(Mono.just(buffer));
            }
            
            // 检查用户是否具有所需权限
            List<String> userPerms = userPermissions.get(userId);
            if (userPerms == null || !userPerms.contains(config.getRequiredPermission())) {
                logger.warn("User {} lacks required permission: {}", userId, config.getRequiredPermission());
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.FORBIDDEN);
                response.getHeaders().add("Content-Type", "application/json");
                
                String errorMessage = "{\"error\": \"Forbidden\", \"message\": \"Insufficient permissions\"}";
                DataBuffer buffer = response.bufferFactory().wrap(errorMessage.getBytes());
                return response.writeWith(Mono.just(buffer));
            }
            
            logger.debug("User {} has required permission: {}", userId, config.getRequiredPermission());
            return chain.filter(exchange);
        };
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("requiredPermission");
    }

    public static class Config {
        private String requiredPermission;

        public String getRequiredPermission() {
            return requiredPermission;
        }

        public void setRequiredPermission(String requiredPermission) {
            this.requiredPermission = requiredPermission;
        }
    }
}
