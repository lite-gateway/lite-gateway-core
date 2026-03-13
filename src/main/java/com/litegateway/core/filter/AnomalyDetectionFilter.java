package com.litegateway.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异常检测过滤器
 * 用于检测和处理异常情况
 */
@Component
public class AnomalyDetectionFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AnomalyDetectionFilter.class);

    // 异常计数器
    private final ConcurrentHashMap<String, AtomicInteger> errorCountByPath = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> errorCountByService = new ConcurrentHashMap<>();
    
    // 响应时间监控
    private final ConcurrentHashMap<String, Long> lastResponseTimeByPath = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> avgResponseTimeByPath = new ConcurrentHashMap<>();
    
    // 告警阈值
    private static final int ERROR_THRESHOLD = 10; // 每分钟错误数阈值
    private static final long RESPONSE_TIME_THRESHOLD = 5000; // 响应时间阈值（毫秒）
    
    // 告警记录，避免重复告警
    private final ConcurrentHashMap<String, Long> lastAlertTimeByPath = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastAlertTimeByService = new ConcurrentHashMap<>();
    
    // 告警间隔（毫秒）
    private static final long ALERT_INTERVAL = 60000; // 1分钟

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        Instant startTime = Instant.now();
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String service = extractServiceFromPath(path);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            long responseTime = Duration.between(startTime, Instant.now()).toMillis();
            
            // 更新响应时间
            updateResponseTime(path, responseTime);
            
            // 检查响应状态码
            if (response.getStatusCode() != null && response.getStatusCode().isError()) {
                // 增加错误计数
                incrementErrorCount(errorCountByPath, path);
                incrementErrorCount(errorCountByService, service);
                
                // 检查错误率是否超过阈值
                checkErrorThreshold(path, "path");
                checkErrorThreshold(service, "service");
            }
            
            // 检查响应时间是否超过阈值
            checkResponseTimeThreshold(path, responseTime);
        }));
    }

    private String extractServiceFromPath(String path) {
        // 从路径中提取服务名，假设路径格式为 /service/path
        String[] parts = path.split("/");
        return parts.length > 1 ? parts[1] : "unknown";
    }

    private void incrementErrorCount(ConcurrentHashMap<String, AtomicInteger> map, String key) {
        map.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
    }

    private void updateResponseTime(String path, long responseTime) {
        lastResponseTimeByPath.put(path, responseTime);
        
        // 简单移动平均
        long currentAvg = avgResponseTimeByPath.getOrDefault(path, 0L);
        long newAvg = (currentAvg * 9 + responseTime) / 10; // 10% 权重给新值
        avgResponseTimeByPath.put(path, newAvg);
    }

    private void checkErrorThreshold(String key, String type) {
        AtomicInteger errorCount = errorCountByPath.get(key);
        if (errorCount != null && errorCount.get() >= ERROR_THRESHOLD) {
            long lastAlertTime = lastAlertTimeByPath.getOrDefault(key, 0L);
            if (System.currentTimeMillis() - lastAlertTime > ALERT_INTERVAL) {
                sendAlert("Error threshold exceeded", 
                         "Type: " + type + ", Key: " + key + ", Error count: " + errorCount.get());
                lastAlertTimeByPath.put(key, System.currentTimeMillis());
                // 重置计数器
                errorCount.set(0);
            }
        }
    }

    private void checkResponseTimeThreshold(String path, long responseTime) {
        if (responseTime > RESPONSE_TIME_THRESHOLD) {
            long lastAlertTime = lastAlertTimeByPath.getOrDefault(path, 0L);
            if (System.currentTimeMillis() - lastAlertTime > ALERT_INTERVAL) {
                sendAlert("Response time exceeded threshold", 
                         "Path: " + path + ", Response time: " + responseTime + "ms");
                lastAlertTimeByPath.put(path, System.currentTimeMillis());
            }
        }
    }

    private void sendAlert(String title, String message) {
        // 这里可以实现实际的告警通知逻辑
        // 例如发送邮件、短信、或推送到监控系统
        logger.error("ALERT: {} - {}", title, message);
        
        // 模拟告警通知
        System.out.println("[ALERT] " + title + ": " + message);
    }

    @Override
    public int getOrder() {
        return -90; // 优先级低于流量分析过滤器
    }

    // 获取异常检测数据的方法，供监控系统使用
    public Map<String, Integer> getErrorCountByPath() {
        Map<String, Integer> result = new HashMap<>();
        errorCountByPath.forEach((path, count) -> result.put(path, count.get()));
        return result;
    }

    public Map<String, Integer> getErrorCountByService() {
        Map<String, Integer> result = new HashMap<>();
        errorCountByService.forEach((service, count) -> result.put(service, count.get()));
        return result;
    }

    public Map<String, Long> getAvgResponseTimeByPath() {
        return avgResponseTimeByPath;
    }
}
