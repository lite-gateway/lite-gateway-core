package com.litegateway.core.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.litegateway.core.dto.UserDTO;
import com.litegateway.core.manager.GatewayFeatureManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.Map;

/**
 * 认证全局过滤器（可配置版）
 * 将登录用户的 JWT 转化成用户信息，转发出去的 header 增加 json-user 属性
 * 支持动态开关
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered, ConfigurableFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthGlobalFilter.class);

    private static final String FEATURE_CODE = "auth_jwt";

    private static final String USER_HEADER = "X-User-Info";
    private static final String AUTHORIZATION = "Authorization";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private GatewayFeatureManager gatewayFeatureManager;

    @Override
    public String getFeatureCode() {
        return FEATURE_CODE;
    }

    @Override
    public int getPriority() {
        return 30;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取当前路由
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = route != null ? route.getId() : "default";

        // 检查功能是否启用
        if (!gatewayFeatureManager.isFeatureEnabled(FEATURE_CODE, routeId)) {
            logger.debug("JWT auth is disabled for route: {}", routeId);
            return chain.filter(exchange);
        }

        String urlPath = exchange.getRequest().getPath().value();

        // 获取真实 IP 写入头部
        String realIp = getClientIp(exchange);

        return exchange.getPrincipal()
                .cast(Principal.class)
                .defaultIfEmpty(() -> "anonymous")
                .flatMap(principal -> {
                    if ("anonymous".equals(principal.getName())) {
                        return chain.filter(createNewExchange(exchange, realIp, null));
                    }

                    String authHeader = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION);
                    if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
                        return chain.filter(createNewExchange(exchange, realIp, null));
                    }

                    try {
                        JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) principal;
                        Map<String, Object> claims = jwtToken.getToken().getClaims();

                        logger.debug("AuthGlobalFilter - path: {}, user: {}", urlPath, claims.get("sub"));

                        UserDTO userDTO = UserDTO.fromClaims(claims);
                        String userJson = objectMapper.writeValueAsString(userDTO);

                        // 将 user 信息插入 header
                        return chain.filter(createNewExchange(exchange, realIp, userJson));
                    } catch (Exception e) {
                        logger.warn("Failed to process JWT token: {}", e.getMessage());
                        return chain.filter(createNewExchange(exchange, realIp, null));
                    }
                });
    }

    private String getClientIp(ServerWebExchange exchange) {
        InetSocketAddress socketAddress = exchange.getRequest().getRemoteAddress();
        if (socketAddress != null && socketAddress.getAddress() != null) {
            return socketAddress.getAddress().getHostAddress();
        }
        return null;
    }

    private ServerWebExchange createNewExchange(ServerWebExchange exchange, String realIp, String userJson) {
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();

        if (userJson != null) {
            String encodedUser = Base64.getEncoder().encodeToString(userJson.getBytes(StandardCharsets.UTF_8));
            requestBuilder.header(USER_HEADER, encodedUser);
        }

        if (realIp != null) {
            requestBuilder.header("X-Real-IP", realIp);
            if (!exchange.getRequest().getHeaders().containsKey("X-Forwarded-For")) {
                requestBuilder.header("X-Forwarded-For", realIp);
            }
        }

        return exchange.mutate().request(requestBuilder.build()).build();
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
