# 认证授权功能说明

## 功能概述

认证授权是 Lite Gateway Core 的重要安全功能，负责验证用户身份并授权访问权限，确保系统的安全性和数据的保护。

## 实现方式

### 核心组件

1. **AuthGlobalFilter**：全局过滤器，负责处理 JWT 认证并将用户信息传递给后端服务。

2. **ApiKeyAuthenticationFilter**：API 密钥认证过滤器，用于基于 API 密钥的认证。

3. **FineGrainedPermissionFilter**：细粒度权限控制过滤器，用于基于权限的访问控制。

4. **JwtAuthenticationManager**：JWT 认证管理器，负责验证 JWT 令牌的有效性。

5. **UnauthorizedHandler**：未授权处理程序，处理未授权访问的情况。

### 关键实现细节

- **JWT 认证**：使用 Spring Security OAuth2 Resource Server 和 JWT 实现认证，支持从 JWT 令牌中提取用户信息。

- **用户信息传递**：将用户信息编码后放入请求头 `X-User-Info` 中，传递给后端服务。

- **IP 地址获取**：获取客户端真实 IP 地址，并放入请求头 `X-Real-IP` 和 `X-Forwarded-For` 中。

- **API 密钥认证**：基于请求头中的 API 密钥进行认证，适用于机器间的通信。

- **细粒度权限控制**：基于路由配置中的权限要求，验证用户是否具有相应的权限。

### 代码示例

```java
// AuthGlobalFilter 核心逻辑
@Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
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
```

## 应用场景

1. **用户认证**：验证用户身份，确保只有合法用户能够访问系统。

2. **权限控制**：基于用户角色和权限，控制对资源的访问。

3. **API 访问控制**：为第三方应用提供 API 密钥，控制其对 API 的访问。

4. **微服务间通信**：在微服务架构中，确保服务间通信的安全性。

5. **审计日志**：记录用户访问行为，用于审计和安全分析。

## 功能意义

1. **保护系统安全**：防止未授权访问，保护系统和数据的安全。

2. **实现细粒度访问控制**：基于用户权限，实现对资源的精细控制。

3. **支持多种认证方式**：支持 JWT 和 API 密钥等多种认证方式，适应不同的应用场景。

4. **简化后端服务**：将认证授权逻辑集中在网关，简化后端服务的实现。

5. **提高系统可维护性**：统一的认证授权机制，便于管理和维护。

## 配置与使用

### JWT 配置

```yaml
# JWT 配置
jwt:
  secret: ${JWT_SECRET:lite-gateway-secret-key-for-jwt-signing}
  expiration: ${JWT_EXPIRATION:86400000}
```

### 路由配置示例

```yaml
# 带 API 密钥认证的路由
spring:
  cloud:
    gateway:
      routes:
        - id: api-route
          uri: http://localhost:8081
          predicates:
            - Path=/api/**
          filters:
            - name: ApiKeyAuthentication
              args:
                headerName: X-API-Key
            - name: FineGrainedPermission
              args:
                requiredPermission: READ
```

### 安全配置

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/**").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(new JwtAuthenticationConverter())
                        )
                )
                .build();
    }
}
```

## 注意事项

1. **密钥管理**：JWT 密钥应该安全管理，避免泄露。

2. **令牌过期**：合理设置 JWT 令牌的过期时间，平衡安全性和用户体验。

3. **权限设计**：设计合理的权限体系，避免权限过于复杂或过于简单。

4. **性能考虑**：认证授权过程可能会影响系统性能，需要合理优化。

5. **安全审计**：定期审计认证授权日志，发现潜在的安全问题。

## 总结

认证授权功能是 Lite Gateway Core 的重要安全特性，通过提供统一的认证授权机制，确保系统的安全性和数据的保护。它支持多种认证方式和细粒度的权限控制，适应不同的应用场景，是构建安全可靠的微服务架构的重要组成部分。