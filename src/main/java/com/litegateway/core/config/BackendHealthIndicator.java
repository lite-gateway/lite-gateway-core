package com.litegateway.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 后端服务健康检查指示器
 * 监控后端服务的状态
 */
@Component
public class BackendHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(BackendHealthIndicator.class);

    private final WebClient webClient;

    public BackendHealthIndicator(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean allHealthy = true;

        // 检查主要后端服务
        try {
            // 检查测试服务
            checkService("test-service", "http://localhost:8080/actuator/health", details);
            
            // 检查API服务
            checkService("api-service", "http://localhost:8081/actuator/health", details);
            
            // 检查灰度服务
            checkService("gray-service", "http://localhost:8082/actuator/health", details);
        } catch (Exception e) {
            logger.error("Error checking backend service health", e);
            return Health.down(e).build();
        }

        // 检查是否所有服务都健康
        for (Object status : details.values()) {
            if (status instanceof Map) {
                Map<?, ?> serviceDetails = (Map<?, ?>) status;
                if (!"UP".equals(serviceDetails.get("status"))) {
                    allHealthy = false;
                    break;
                }
            }
        }

        if (allHealthy) {
            return Health.up().withDetails(details).build();
        } else {
            return Health.down().withDetails(details).build();
        }
    }

    private void checkService(String serviceName, String url, Map<String, Object> details) {
        try {
            // 发送健康检查请求，超时时间为1秒
            Health serviceHealth = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Health.class)
                    .timeout(java.time.Duration.ofSeconds(1))
                    .onErrorReturn(Health.down().build())
                    .block();

            Map<String, Object> serviceDetails = new HashMap<>();
            serviceDetails.put("status", serviceHealth.getStatus());
            serviceDetails.put("details", serviceHealth.getDetails());
            details.put(serviceName, serviceDetails);
        } catch (Exception e) {
            logger.warn("Error checking {} health: {}", serviceName, e.getMessage());
            Map<String, Object> serviceDetails = new HashMap<>();
            serviceDetails.put("status", "DOWN");
            serviceDetails.put("error", e.getMessage());
            details.put(serviceName, serviceDetails);
        }
    }
}