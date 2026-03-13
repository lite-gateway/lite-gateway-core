package com.litegateway.core.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 熔断配置
 */
@Configuration
public class Resilience4jCircuitBreakerConfig {

    /**
     * 配置熔断工厂
     */
    @Bean
    public ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory(CircuitBreakerRegistry circuitBreakerRegistry) {
        ReactiveResilience4JCircuitBreakerFactory factory = new ReactiveResilience4JCircuitBreakerFactory(circuitBreakerRegistry, null);
        
        // 全局默认配置
        factory.configureDefault(id -> {
            return new Resilience4JConfigBuilder(id)
                    .circuitBreakerConfig(CircuitBreakerConfig.custom()
                            .failureRateThreshold(50) // 失败率阈值，超过50%触发熔断
                            .waitDurationInOpenState(Duration.ofSeconds(60)) // 熔断后60秒尝试半开
                            .slidingWindowSize(100) // 滑动窗口大小
                            .minimumNumberOfCalls(10) // 最小调用次数
                            .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(5)) // 超时时间
                            .build())
                    .build();
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
}
