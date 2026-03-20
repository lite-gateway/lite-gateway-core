package com.litegateway.core.listener;

import com.litegateway.core.cache.IpListCache;
import com.litegateway.core.cache.WhiteListCache;
import com.litegateway.core.constants.RedisTypeConstants;
import com.litegateway.core.dto.*;
import com.litegateway.core.listener.event.*;
import com.litegateway.core.manager.GatewayFeatureManager;
import com.litegateway.core.service.ConfigSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 路由同步消息监听器
 * 监听 Redis 消息，同步路由、IP黑名单、白名单、限流规则更新
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway.core
 */
@Component
public class SyncRouteUpdateMessageListener implements MessageListener, ApplicationEventPublisherAware {

    private static final Logger logger = LoggerFactory.getLogger(SyncRouteUpdateMessageListener.class);

    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ConfigSyncService configSyncService;

    @Autowired
    private GatewayFeatureManager gatewayFeatureManager;

    /**
     * 监听 Redis 消息
     * CHANNEL = "lite:gateway:sync:route:update"
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody());
        logger.info("Received Redis message: {}", body);

        switch (body) {
            case RedisTypeConstants.ROUTE_UPDATE:
                // 同步路由
                logger.info("Refreshing routes...");
                configSyncService.syncConfig();
                this.applicationEventPublisher.publishEvent(new RefreshRoutesEvent(this));
                break;
            case RedisTypeConstants.IP_UPDATE:
                // 同步 IP 黑名单
                logger.info("Refreshing IP blacklist...");
                configSyncService.syncConfig();
                this.applicationEventPublisher.publishEvent(new DataIpRefreshEvent(this));
                break;
            case RedisTypeConstants.WHITE_LIST_UPDATE:
                // 同步白名单
                logger.info("Refreshing whitelist...");
                configSyncService.syncConfig();
                this.applicationEventPublisher.publishEvent(new WhiteListRefreshEvent(this));
                break;
            case RedisTypeConstants.RATE_LIMIT_UPDATE:
                // 同步限流规则参数变更
                logger.info("Refreshing rate limit rules...");
                // 限流规则变更时，先同步配置，然后发布限流变更事件
                configSyncService.syncConfig();
                // 发布限流规则变更事件，通知 Gateway 刷新限流器参数
                this.applicationEventPublisher.publishEvent(new RateLimitChangeEvent(this, null));
                break;
            case RedisTypeConstants.RATE_LIMIT_ROUTE_RELATION_UPDATE:
                // 同步限流规则与路由关联变更
                logger.info("Refreshing rate limit route relations...");
                // 关联变更需要重建路由
                configSyncService.syncConfig();
                this.applicationEventPublisher.publishEvent(new RefreshRoutesEvent(this));
                break;
            case RedisTypeConstants.FEATURE_CONFIG_UPDATE:
                // 同步功能配置
                logger.info("Refreshing feature configs...");
                GatewayConfigDTO featureConfig = configSyncService.syncConfig();
                if (featureConfig != null && featureConfig.getFeatureConfigs() != null) {
                    gatewayFeatureManager.updateFeatureConfigs(featureConfig.getFeatureConfigs());
                    this.applicationEventPublisher.publishEvent(new FeatureConfigChangeEvent(this, null));
                }
                break;
            case RedisTypeConstants.CIRCUIT_BREAKER_UPDATE:
                // 同步熔断规则
                logger.info("Refreshing circuit breaker rules...");
                GatewayConfigDTO cbConfig = configSyncService.syncConfig();
                if (cbConfig != null && cbConfig.getCircuitBreakerRules() != null) {
                    gatewayFeatureManager.updateCircuitBreakerRules(cbConfig.getCircuitBreakerRules());
                    this.applicationEventPublisher.publishEvent(new CircuitBreakerRuleChangeEvent(this, null));
                }
                break;
            case RedisTypeConstants.CANARY_RULE_UPDATE:
                // 同步灰度规则
                logger.info("Refreshing canary rules...");
                GatewayConfigDTO canaryConfig = configSyncService.syncConfig();
                if (canaryConfig != null && canaryConfig.getCanaryRules() != null) {
                    gatewayFeatureManager.updateCanaryRules(canaryConfig.getCanaryRules());
                    this.applicationEventPublisher.publishEvent(new CanaryRuleChangeEvent(this, null, null));
                }
                break;
            default:
                logger.warn("Unknown message type: {}", body);
        }
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
