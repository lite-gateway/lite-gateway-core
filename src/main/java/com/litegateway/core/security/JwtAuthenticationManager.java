package com.litegateway.core.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 认证管理器
 * 验证 JWT Token 并构建认证信息
 */
@Slf4j
@Component
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    @Value("${jwt.secret:lite-gateway-secret-key-for-jwt-signing}")
    private String jwtSecret;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.just(authentication)
                .cast(JwtAuthenticationToken.class)
                .map(jwtAuth -> {
                    // 从 JWT 中提取权限
                    Collection<GrantedAuthority> authorities = extractAuthorities(jwtAuth);
                    return new UsernamePasswordAuthenticationToken(
                            jwtAuth.getPrincipal(),
                            jwtAuth.getCredentials(),
                            authorities
                    );
                });
    }

    private Collection<GrantedAuthority> extractAuthorities(JwtAuthenticationToken jwtAuth) {
        List<String> authorities = jwtAuth.getToken().getClaimAsStringList("authorities");
        if (authorities == null) {
            authorities = List.of();
        }
        return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
