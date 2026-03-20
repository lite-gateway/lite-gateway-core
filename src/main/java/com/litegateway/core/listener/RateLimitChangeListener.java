package com.litegateway.core.listener;

import com.litegateway.core.listener.event.RateLimitChangeEvent;
import com.litegateway.core.listener.event.RateLimitRouteRelationChangeEvent;
import com.litegateway.core.route.DynamicRouteDefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 限流规则变更监听器
 * 处理限流规则变更事件，实现热更新机制
 */
@Component
public class RateLimitChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitChangeListener.class);

    @Autowired
    private DynamicRouteDefinitionRepository routeRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 监听限流规则变更事件
     * 限流变更时，仅刷新限流器参数，不中断连接
     */
    @EventListener
    public void onRateLimitChange(RateLimitChangeEvent event) {
        String ruleId = event.getRuleId();
        logger.info("Received rate limit change event for rule: {}", ruleId);

        try {
            // 1. 清除该限流规则相关的 Redis 缓存
            // 限流器在 Redis 中的键格式通常为：request_rate_limiter.{key}.tokens 和 request_rate_limiter.{key}.timestamp
            String rateLimitKeyPattern = "request_rate_limiter.*" + ruleId + "*";
            // 注意：这里使用 keys 命令在生产环境可能不合适，建议使用 scan 或维护一个规则到路由的映射
            // 实际实现中，应该通过 ruleId 查询到所有使用该规则的路由ID

            // 2. 发布限流规则刷新消息到 Redis，通知所有 Gateway 实例
            // 这里使用 Redis Pub/Sub 机制
            if (redisTemplate != null) {
                redisTemplate.convertAndSend("rate_limit:refresh", ruleId);
                logger.info("Published rate limit refresh message for rule: {}", ruleId);
            }

            // 3. 本地刷新（如果是单机部署）
            refreshLocalRateLimiter(ruleId);

        } catch (Exception e) {
            logger.error("Failed to handle rate limit change event for rule: {}", ruleId, e);
        }
    }

    /**
     * 监听路由-限流关联变更事件
     * 关联变更需要重建路由（低频操作）
     */
    @EventListener
    public void onRelationChange(RateLimitRouteRelationChangeEvent event) {
        String routeId = event.getRouteId();
        String ruleId = event.getRuleId();
        RateLimitRouteRelationChangeEvent.ChangeType changeType = event.getChangeType();

        logger.info("Received rate limit relation change event: route={}, rule={}, type={}",
                routeId, ruleId, changeType);

        try {
            // 关联变更需要重建路由
            // 这里触发路由刷新，可以通过 Redis 发布消息通知所有 Gateway 实例
            if (redisTemplate != null) {
                redisTemplate.convertAndSend("route:refresh", routeId);
                logger.info("Published route refresh message for route: {}", routeId);
            }

        } catch (Exception e) {
            logger.error("Failed to handle relation change event for route: {}", routeId, e);
        }
    }

    /**
     * 刷新本地限流器
     * 清除本地缓存，下次请求时会重新从 Redis 获取令牌桶状态
     */
    private void refreshLocalRateLimiter(String ruleId) {
        // Spring Cloud Gateway 的 RedisRateLimiter 使用 Redis 存储令牌桶状态
        // 清除 Redis 中的令牌桶状态后，下次请求会自动重新初始化
        // 这里可以实现更细粒度的控制，如只清除特定路由的限流器

        logger.info("Refreshing local rate limiter for rule: {}", ruleId);

        // 实际实现中，可以通过 ruleId 查询到所有使用该规则的路由ID
        // 然后清除这些路由对应的限流器缓存
    }

    /**
     * 处理 Redis 订阅的限流刷新消息
     * 用于集群环境下同步限流规则变更
     */
    public void handleRefreshMessage(String ruleId) {
        logger.info("Handling refresh message for rule: {}", ruleId);
        refreshLocalRateLimiter(ruleId);
    }
}
