package com.litegateway.core.client;

import com.litegateway.core.common.web.Result;
import com.litegateway.core.dto.GatewayConfigDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Admin 配置客户端
 * 从 Admin 模块获取网关配置
 */
@Component
public class AdminConfigClient {

    private static final Logger logger = LoggerFactory.getLogger(AdminConfigClient.class);

    @Autowired
    private WebClient adminWebClient;

    /**
     * 获取完整网关配置
     */
    public GatewayConfigDTO getGatewayConfig() {
        try {
            logger.info("Fetching gateway config from admin...");
            
            Result<GatewayConfigDTO> result = adminWebClient.get()
                    .uri("/gateway/config/gateway")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Result<GatewayConfigDTO>>() {})
                    .block();
            
            if (result != null && result.isOk()) {
                logger.info("Successfully fetched gateway config, version: {}", 
                        result.getData() != null ? result.getData().getVersion() : "null");
                return result.getData();
            } else {
                logger.warn("Failed to fetch gateway config: {}", 
                        result != null ? result.getMessage() : "null result");
                return null;
            }
        } catch (Exception e) {
            logger.error("Error fetching gateway config: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取配置版本号
     */
    public Long getConfigVersion() {
        try {
            Result<Long> result = adminWebClient.get()
                    .uri("/gateway/config/version")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Result<Long>>() {})
                    .block();
            
            if (result != null && result.isOk()) {
                return result.getData();
            }
            return null;
        } catch (Exception e) {
            logger.error("Error fetching config version: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查配置是否有更新
     * @param clientVersion 客户端当前版本号
     * @return 有更新返回最新配置，无更新返回 null
     */
    public GatewayConfigDTO checkConfigUpdate(Long clientVersion) {
        try {
            Result<GatewayConfigDTO> result = adminWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/gateway/config/check")
                            .queryParam("clientVersion", clientVersion)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Result<GatewayConfigDTO>>() {})
                    .block();
            
            if (result != null && result.isOk()) {
                return result.getData();
            }
            return null;
        } catch (Exception e) {
            logger.error("Error checking config update: {}", e.getMessage());
            return null;
        }
    }
}
