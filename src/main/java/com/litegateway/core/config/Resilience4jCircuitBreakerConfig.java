package com.litegateway.core.config;

import com.litegateway.core.dto.CircuitBreakerRuleDTO;
import com.litegateway.core.manager.GatewayFeatureManager;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 熔断配置
 * 支持动态配置
 */
@Configuration
public class Resilience4jCircuitBreakerConfig {

    private static final Logger logger = LoggerFactory.getLogger(Resilience4jCircuitBreakerConfig.class);

    @Autowired
    private GatewayFeatureManager gatewayFeatureManager;

    /**
     * 配置熔断工厂
     */
    @Bean
    public ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory(CircuitBreakerRegistry circuitBreakerRegistry) {
        ReactiveResilience4JCircuitBreakerFactory factory = new ReactiveResilience4JCircuitBreakerFactory(circuitBreakerRegistry, null);

        // 动态配置
        factory.configureDefault(id -> {
            // 尝试从配置管理器获取路由特定配置
            CircuitBreakerRuleDTO rule = gatewayFeatureManager.getCircuitBreakerRule(id);
            if (rule != null) {
                logger.debug("Using dynamic circuit breaker config for route: {}", id);
                return buildConfigFromRule(id, rule);
            }
            // 使用默认配置
            logger.debug("Using default circuit breaker config for route: {}", id);
            return buildDefaultConfig(id);
        });

        return factory;
    }

    /**
     * 配置熔断注册表
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .slidingWindowSize(100)
                .minimumNumberOfCalls(10)
                .build();

        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }

    /**
     * 根据规则构建配置
     */
    private Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration buildConfigFromRule(String id, CircuitBreakerRuleDTO rule) {
        return new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .failureRateThreshold(rule.getFailureRateThreshold() != null ? rule.getFailureRateThreshold() : 50)
                        .waitDurationInOpenState(Duration.ofSeconds(rule.getWaitDurationInOpenState() != null ? rule.getWaitDurationInOpenState() : 60))
                        .slidingWindowSize(rule.getSlidingWindowSize() != null ? rule.getSlidingWindowSize() : 100)
                        .minimumNumberOfCalls(rule.getMinimumNumberOfCalls() != null ? rule.getMinimumNumberOfCalls() : 10)
                        .slowCallRateThreshold(rule.getSlowCallRateThreshold() != null ? rule.getSlowCallRateThreshold() : 50)
                        .slowCallDurationThreshold(Duration.ofSeconds(rule.getSlowCallDurationThreshold() != null ? rule.getSlowCallDurationThreshold() : 5))
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(rule.getTimeoutDuration() != null ? rule.getTimeoutDuration() : 5))
                        .build())
                .build();
    }

    /**
     * 构建默认配置
     */
    private Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration buildDefaultConfig(String id) {
        return new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(60))
                        .slidingWindowSize(100)
                        .minimumNumberOfCalls(10)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(5))
                        .build())
                .build();
    }
}
