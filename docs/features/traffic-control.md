# 流量控制功能说明

## 功能概述

流量控制是 Lite Gateway Core 的重要功能，用于管理和限制请求流量，防止系统过载，确保服务的稳定性和可靠性。

## 实现方式

### 核心组件

1. **TrafficControlFilter**：流量控制过滤器，用于控制请求的流量，包括并发请求数和请求速率限制。

2. **RequestRateLimiter**：请求速率限制器，基于令牌桶算法实现。

### 关键实现细节

- **并发控制**：使用 `Semaphore` 实现并发请求数的限制。

- **速率控制**：使用 `AtomicInteger` 实现简单的请求速率限制，每秒重置计数器。

- **限流策略**：支持基于 IP 地址的限流，通过 `hostAddrKeyResolver` 实现。

- **响应处理**：当流量超过限制时，返回 `429 Too Many Requests` 响应。

### 代码示例

```java
// TrafficControlFilter 核心逻辑
@Override
public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
        ServerHttpRequest request = exchange.getRequest();
        
        // 检查并发限制
        if (!concurrencySemaphore.tryAcquire()) {
            logger.warn("Concurrent request limit exceeded");
            return handleTooManyRequests(exchange);
        }
        
        // 检查速率限制
        if (!checkRateLimit(config.getRateLimitPerSecond())) {
            logger.warn("Rate limit exceeded");
            return handleTooManyRequests(exchange);
        }
        
        return chain.filter(exchange).doFinally(signalType -> {
            // 释放并发信号量
            concurrencySemaphore.release();
        });
    };
}

private boolean checkRateLimit(int rateLimitPerSecond) {
    long currentTime = System.currentTimeMillis();
    
    // 每秒重置计数器
    if (currentTime - lastSecondTimestamp > 1000) {
        requestCount.set(0);
        lastSecondTimestamp = currentTime;
    }
    
    // 检查是否超过速率限制
    return requestCount.incrementAndGet() <= rateLimitPerSecond;
}
```

## 应用场景

1. **防止系统过载**：当请求量突然增加时，通过限流保护系统，避免系统崩溃。

2. **保障服务质量**：确保每个用户都能获得合理的服务资源，避免个别用户占用过多资源。

3. **应对流量峰值**：在促销、秒杀等活动期间，通过限流控制流量，确保系统稳定运行。

4. **保护后端服务**：防止后端服务被过多的请求压垮，确保服务的可用性。

5. **资源分配**：合理分配系统资源，提高资源利用率。

## 功能意义

1. **提高系统稳定性**：通过限流保护系统，避免系统因过载而崩溃。

2. **保障服务质量**：确保所有用户都能获得合理的服务响应时间。

3. **优化资源利用**：合理分配系统资源，提高资源利用率。

4. **防止恶意攻击**：防止恶意用户通过大量请求攻击系统。

5. **提供可预测的服务能力**：通过限流，系统的服务能力变得可预测，便于容量规划。

## 配置与使用

### 路由配置示例

```yaml
# 带限流的路由
spring:
  cloud:
    gateway:
      routes:
        - id: api-route
          uri: http://localhost:8081
          predicates:
            - Path=/api/**
          filters:
            - name: RequestRateLimiter
              args:
                key-resolver: '#{@hostAddrKeyResolver}'
                replenishRate: 10
                burstCapacity: 20
            - name: TrafficControl
              args:
                rateLimitPerSecond: 100
                concurrencyLimit: 50
```

### 速率限制器配置

```java
@Configuration
public class LoadBalancerConfig {

    @Bean
    public KeyResolver hostAddrKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
    }
}
```

## 注意事项

1. **限流策略选择**：根据实际业务场景选择合适的限流策略，如基于 IP、用户、路径等。

2. **限流参数调整**：根据系统的实际承载能力，调整限流参数，避免过度限流或限流不足。

3. **监控与告警**：监控限流情况，当触发限流时及时告警，便于及时处理。

4. **降级策略**：当触发限流时，提供合理的降级策略，如返回友好的错误信息或引导用户稍后重试。

5. **性能考虑**：限流实现应该高效，避免对系统性能造成额外的负担。

## 总结

流量控制功能是 Lite Gateway Core 的重要特性，通过限制请求流量，保护系统免受过载的影响，确保服务的稳定性和可靠性。它不仅可以防止系统崩溃，还可以保障服务质量，优化资源利用，是构建高可用微服务架构的重要组成部分。