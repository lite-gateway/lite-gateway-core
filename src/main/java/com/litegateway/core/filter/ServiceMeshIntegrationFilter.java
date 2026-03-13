package com.litegateway.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * 服务网格集成过滤器
 * 用于与Istio等服务网格进行集成
 */
@Component
public class ServiceMeshIntegrationFilter extends AbstractGatewayFilterFactory<ServiceMeshIntegrationFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(ServiceMeshIntegrationFilter.class);

    public ServiceMeshIntegrationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // 添加服务网格相关的头信息
            ServerHttpRequest modifiedRequest = request.mutate()
                    // Istio相关头信息
                    .header("x-request-id", generateRequestId())
                    .header("x-b3-traceid", generateTraceId())
                    .header("x-b3-spanid", generateSpanId())
                    .header("x-b3-parentspanid", "0")
                    .header("x-b3-sampled", "1")
                    // 服务网格路由信息
                    .header("x-service-mesh", "istio")
                    .header("x-mesh-version", config.getMeshVersion())
                    // 目标服务信息
                    .header("x-target-service", config.getTargetService())
                    .header("x-target-version", config.getTargetVersion())
                    .build();
            
            logger.debug("Service mesh integration enabled for: {}, target service: {}", 
                        request.getPath().value(), config.getTargetService());
            
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    private String generateRequestId() {
        return java.util.UUID.randomUUID().toString();
    }

    private String generateTraceId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    private String generateSpanId() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("meshVersion", "targetService", "targetVersion");
    }

    public static class Config {
        private String meshVersion = "1.10.0";
        private String targetService;
        private String targetVersion = "v1";

        public String getMeshVersion() {
            return meshVersion;
        }

        public void setMeshVersion(String meshVersion) {
            this.meshVersion = meshVersion;
        }

        public String getTargetService() {
            return targetService;
        }

        public void setTargetService(String targetService) {
            this.targetService = targetService;
        }

        public String getTargetVersion() {
            return targetVersion;
        }

        public void setTargetVersion(String targetVersion) {
            this.targetVersion = targetVersion;
        }
    }
}
