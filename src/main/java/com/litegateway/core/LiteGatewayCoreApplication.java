package com.litegateway.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Lite Gateway Core 启动类
 * 轻量级网关核心服务
 * 从旧项目 jtyjy-gateway 迁移，包名从 com.jtyjy.gateway 改为 com.litegateway.core
 */
@SpringBootApplication
public class LiteGatewayCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiteGatewayCoreApplication.class, args);
    }
}
