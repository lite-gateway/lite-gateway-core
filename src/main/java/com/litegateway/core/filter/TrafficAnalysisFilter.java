package com.litegateway.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 实时流量分析过滤器
 * 用于收集和分析请求流量数据
 */
@Component
public class TrafficAnalysisFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(TrafficAnalysisFilter.class);

    // 流量统计数据
    private final ConcurrentHashMap<String, AtomicLong> requestCountByPath = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> requestCountByMethod = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> errorCountByPath = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> totalResponseTimeByPath = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> requestCountByPathAndMethod = new ConcurrentHashMap<>();

    // 最近1分钟的请求数
    private final AtomicLong requestCountLastMinute = new AtomicLong(0);
    private long lastMinuteTimestamp = System.currentTimeMillis();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        Instant startTime = Instant.now();
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();
        String pathMethodKey = path + "_" + method;

        // 增加请求计数
        incrementCounter(requestCountByPath, path);
        incrementCounter(requestCountByMethod, method);
        incrementCounter(requestCountByPathAndMethod, pathMethodKey);
        incrementCounter(requestCountLastMinute);

        // 重置每分钟计数
        resetMinuteCounter();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            long responseTime = Duration.between(startTime, Instant.now()).toMillis();
            
            // 记录响应时间
            incrementCounter(totalResponseTimeByPath, path, responseTime);
            
            // 记录错误
            if (response.getStatusCode() != null && response.getStatusCode().isError()) {
                incrementCounter(errorCountByPath, path);
            }
            
            // 每1000个请求打印一次统计信息
            if (requestCountByPath.getOrDefault(path, new AtomicLong(0)).get() % 1000 == 0) {
                printTrafficStats();
            }
        }));
    }

    private void incrementCounter(AtomicLong counter) {
        counter.incrementAndGet();
    }

    private void incrementCounter(ConcurrentHashMap<String, AtomicLong> map, String key) {
        map.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }

    private void incrementCounter(ConcurrentHashMap<String, AtomicLong> map, String key, long value) {
        map.computeIfAbsent(key, k -> new AtomicLong(0)).addAndGet(value);
    }

    private void resetMinuteCounter() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMinuteTimestamp > 60000) { // 1分钟
            long count = requestCountLastMinute.getAndSet(0);
            logger.info("Requests in last minute: {}", count);
            lastMinuteTimestamp = currentTime;
        }
    }

    private void printTrafficStats() {
        logger.info("=== Traffic Statistics ===");
        logger.info("Requests by path:");
        requestCountByPath.forEach((path, count) -> {
            long totalTime = totalResponseTimeByPath.getOrDefault(path, new AtomicLong(0)).get();
            long avgTime = count.get() > 0 ? totalTime / count.get() : 0;
            long errors = errorCountByPath.getOrDefault(path, new AtomicLong(0)).get();
            logger.info("  {}: {} requests, avg response time: {}ms, errors: {}", path, count.get(), avgTime, errors);
        });
        logger.info("Requests by method:");
        requestCountByMethod.forEach((method, count) -> {
            logger.info("  {}: {} requests", method, count.get());
        });
        logger.info("========================");
    }

    @Override
    public int getOrder() {
        return -100; // 优先级较高，在其他过滤器之前执行
    }

    // 获取流量统计数据的方法，供监控系统使用
    public ConcurrentHashMap<String, AtomicLong> getRequestCountByPath() {
        return requestCountByPath;
    }

    public ConcurrentHashMap<String, AtomicLong> getRequestCountByMethod() {
        return requestCountByMethod;
    }

    public ConcurrentHashMap<String, AtomicLong> getErrorCountByPath() {
        return errorCountByPath;
    }

    public ConcurrentHashMap<String, AtomicLong> getTotalResponseTimeByPath() {
        return totalResponseTimeByPath;
    }

    public long getRequestCountLastMinute() {
        return requestCountLastMinute.get();
    }
}
