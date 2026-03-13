package com.litegateway.core.config;

import com.litegateway.core.client.AdminConfigClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Admin 客户端配置
 */
@Configuration
public class AdminClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(AdminClientConfig.class);
    
    @Value("${lite.gateway.admin.url:http://localhost:8080}")
    private String adminUrl;

    @Bean
    public WebClient adminWebClient() {
        logger.info("Admin client configured with URL: {}", adminUrl);
        return WebClient.builder()
                .baseUrl(adminUrl)
                .build();
    }
}
