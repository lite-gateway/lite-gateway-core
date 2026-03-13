package com.litegateway.core.service;

import com.litegateway.core.cache.IpListCache;
import com.litegateway.core.cache.WhiteListCache;
import com.litegateway.core.client.AdminConfigClient;
import com.litegateway.core.dto.GatewayConfigDTO;
import com.litegateway.core.dto.IpBlackDTO;
import com.litegateway.core.dto.RouteDTO;
import com.litegateway.core.dto.WhiteListDTO;
import com.litegateway.core.route.DynamicRouteDefinitionRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 配置同步服务
 * 从 Admin 模块拉取配置并同步到本地缓存
 */
@Service
public class ConfigSyncService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigSyncService.class);

    @Autowired
    private AdminConfigClient adminConfigClient;

    @Autowired
    private DynamicRouteDefinitionRepository routeDefinitionRepository;

    // 当前配置版本号
    private final AtomicLong currentVersion = new AtomicLong(0);

    @PostConstruct
    public void init() {
        logger.info("Initializing config sync service...");
        // 启动时立即同步一次配置
        syncConfig();
    }

    /**
     * 定时轮询检查配置更新（每30秒）
     */
    @Scheduled(fixedRate = 30000)
    public void scheduledSync() {
        logger.debug("Scheduled config sync check...");
        checkAndSync();
    }

    /**
     * 强制同步配置（启动时或收到Redis通知时调用）
     */
    public void syncConfig() {
        logger.info("Syncing gateway config from admin...");
        GatewayConfigDTO config = adminConfigClient.getGatewayConfig();
        
        if (config == null) {
            logger.error("Failed to fetch gateway config from admin");
            return;
        }
        
        applyConfig(config);
        currentVersion.set(config.getVersion());
        logger.info("Gateway config synced successfully, version: {}", config.getVersion());
    }

    /**
     * 检查并同步配置（版本号对比）
     */
    public void checkAndSync() {
        Long serverVersion = adminConfigClient.getConfigVersion();
        
        if (serverVersion == null) {
            logger.warn("Failed to get config version from admin");
            return;
        }
        
        if (serverVersion.equals(currentVersion.get())) {
            logger.debug("Config is up to date, version: {}", currentVersion.get());
            return;
        }
        
        logger.info("Config version changed: {} -> {}, syncing...", currentVersion.get(), serverVersion);
        
        // 使用 checkConfigUpdate API 获取更新
        GatewayConfigDTO config = adminConfigClient.checkConfigUpdate(currentVersion.get());
        
        if (config != null) {
            applyConfig(config);
            currentVersion.set(config.getVersion());
            logger.info("Config synced to version: {}", config.getVersion());
        }
    }

    /**
     * 应用配置到本地
     */
    private void applyConfig(GatewayConfigDTO config) {
        // 同步路由
        if (config.getRoutes() != null) {
            syncRoutes(config.getRoutes());
        }
        
        // 同步IP黑名单
        if (config.getIpBlacklist() != null) {
            syncIpBlacklist(config.getIpBlacklist());
        }
        
        // 同步白名单
        if (config.getWhiteList() != null) {
            syncWhiteList(config.getWhiteList());
        }
    }

    /**
     * 同步路由配置
     */
    private void syncRoutes(List<RouteDTO> routes) {
        List<RouteDefinition> routeDefinitions = new ArrayList<>();
        
        for (RouteDTO route : routes) {
            RouteDefinition definition = routeDefinitionRepository.buildRouteDefinition(
                    route.getRouteId(),
                    route.getUri(),
                    route.getPath(),
                    route.getStripPrefix(),
                    route.getWeight(),
                    route.getWeightName(),
                    route.getReplenishRate(),
                    route.getBurstCapacity(),
                    route.getTargetProtocol(),
                    route.getUrlPattern(),
                    route.getUrlReplacement()
            );
            routeDefinitions.add(definition);
        }
        
        routeDefinitionRepository.refreshRoutes(routeDefinitions);
        logger.info("Synced {} routes", routes.size());
    }

    /**
     * 同步IP黑名单
     */
    private void syncIpBlacklist(List<IpBlackDTO> ipBlacklist) {
        IpListCache.clear();
        for (IpBlackDTO item : ipBlacklist) {
            IpListCache.put(item.getIp(), item.getRemark());
        }
        logger.info("Synced {} blacklisted IPs", ipBlacklist.size());
    }

    /**
     * 同步白名单
     */
    private void syncWhiteList(List<WhiteListDTO> whiteList) {
        WhiteListCache.clear();
        for (WhiteListDTO item : whiteList) {
            WhiteListCache.put(item.getPath(), item.getDescription());
        }
        logger.info("Synced {} white list items", whiteList.size());
    }

    /**
     * 获取当前配置版本
     */
    public Long getCurrentVersion() {
        return currentVersion.get();
    }
}
