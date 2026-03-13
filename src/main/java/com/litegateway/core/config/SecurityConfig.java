package com.litegateway.core.config;

import com.litegateway.core.security.JwtAuthenticationManager;
import com.litegateway.core.security.UnauthorizedHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * 安全配置类
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway.core
 * 配置 Spring Security 和 OAuth2 Resource Server
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Autowired
    private UnauthorizedHandler unauthorizedHandler;

    @Autowired
    private JwtAuthenticationManager jwtAuthenticationManager;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(ServerHttpSecurity.CorsSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .exceptionHandling(exceptionHandlingSpec ->
                exceptionHandlingSpec.authenticationEntryPoint(unauthorizedHandler)
            )
            .authorizeExchange(exchange -> exchange
                // 允许健康检查端点
                .pathMatchers("/actuator/**", "/health").permitAll()
                // 允许路由管理API
                .pathMatchers("/api/gateway/route/**").permitAll()
                // 其他请求需要认证
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwtSpec -> jwtSpec.authenticationManager(jwtAuthenticationManager))
                .authenticationEntryPoint(unauthorizedHandler)
            );

        return http.build();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
