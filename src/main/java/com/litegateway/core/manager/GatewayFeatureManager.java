package com.litegateway.core.manager;

import com.litegateway.core.dto.*;
import com.litegateway.core.service.ConfigSyncService;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 网关功能管理器
 * 管理所有可配置功能的开关状态和配置
 */
@Component
public class GatewayFeatureManager {

    private static final Logger logger = LoggerFactory.getLogger(GatewayFeatureManager.class);

    // 功能配置缓存
    private final Map<String, FeatureConfigDTO> featureCache = new ConcurrentHashMap<>();

    // 熔断规则缓存
    private final Map<String, CircuitBreakerRuleDTO> circuitBreakerCache = new ConcurrentHashMap<>();

    // 灰度规则缓存
    private final Map<String, List<CanaryRuleDTO>> canaryRuleCache = new ConcurrentHashMap<>();

    @Autowired
    private ConfigSyncService configSyncService;

    @PostConstruct
    public void init() {
        logger.info("Initializing GatewayFeatureManager...");
        refreshAllConfigs();
    }

    /**
     * 刷新所有配置
     */
    public void refreshAllConfigs() {
        logger.info("Refreshing all gateway feature configs...");
        GatewayConfigDTO config = configSyncService.syncConfig();
        if (config != null) {
            updateFeatureConfigs(config.getFeatureConfigs());
            updateCircuitBreakerRules(config.getCircuitBreakerRules());
            updateCanaryRules(config.getCanaryRules());
            logger.info("All configs refreshed successfully");
        } else {
            logger.warn("Failed to sync config, using existing cache");
        }
    }

    /**
     * 更新功能配置
     */
    public void updateFeatureConfigs(List<FeatureConfigDTO> configs) {
        if (configs == null) {
            return;
        }
        featureCache.clear();
        for (FeatureConfigDTO config : configs) {
            if (config.getFeatureCode() != null) {
                featureCache.put(config.getFeatureCode(), config);
            }
        }
        logger.info("Updated {} feature configs", configs.size());
    }

    /**
     * 更新熔断规则
     */
    public void updateCircuitBreakerRules(List<CircuitBreakerRuleDTO> rules) {
        if (rules == null) {
            return;
        }
        circuitBreakerCache.clear();
        for (CircuitBreakerRuleDTO rule : rules) {
            String key = StringUtils.isNotBlank(rule.getRouteId()) ? rule.getRouteId() : "global";
            circuitBreakerCache.put(key, rule);
        }
        logger.info("Updated {} circuit breaker rules", rules.size());
    }

    /**
     * 更新灰度规则
     */
    public void updateCanaryRules(List<CanaryRuleDTO> rules) {
        if (rules == null) {
            return;
        }
        canaryRuleCache.clear();
        Map<String, List<CanaryRuleDTO>> grouped = rules.stream()
                .filter(r -> StringUtils.isNotBlank(r.getRouteId()))
                .collect(Collectors.groupingBy(CanaryRuleDTO::getRouteId));
        canaryRuleCache.putAll(grouped);
        logger.info("Updated {} canary rules for {} routes", rules.size(), grouped.size());
    }

    /**
     * 检查功能是否启用
     *
     * @param featureCode 功能编码
     * @param routeId     路由ID
     * @return 是否启用
     */
    public boolean isFeatureEnabled(String featureCode, String routeId) {
        FeatureConfigDTO config = featureCache.get(featureCode);
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return false;
        }
        return matchesRoutePattern(routeId, config.getRoutePatterns());
    }

    /**
     * 检查功能是否启用（全局检查，不限制路由）
     *
     * @param featureCode 功能编码
     * @return 是否启用
     */
    public boolean isFeatureEnabled(String featureCode) {
        return isFeatureEnabled(featureCode, null);
    }

    /**
     * 获取功能配置
     *
     * @param featureCode 功能编码
     * @return 功能配置
     */
    public FeatureConfigDTO getFeatureConfig(String featureCode) {
        return featureCache.get(featureCode);
    }

    /**
     * 获取功能配置JSON
     *
     * @param featureCode 功能编码
     * @return 配置JSON字符串
     */
    public String getFeatureConfigJson(String featureCode) {
        FeatureConfigDTO config = featureCache.get(featureCode);
        return config != null ? config.getConfigJson() : null;
    }

    /**
     * 获取所有启用的功能编码
     *
     * @return 功能编码集合
     */
    public Set<String> getEnabledFeatureCodes() {
        return featureCache.values().stream()
                .filter(config -> Boolean.TRUE.equals(config.getEnabled()))
                .map(FeatureConfigDTO::getFeatureCode)
                .collect(Collectors.toSet());
    }

    /**
     * 获取路由的熔断规则
     *
     * @param routeId 路由ID
     * @return 熔断规则
     */
    public CircuitBreakerRuleDTO getCircuitBreakerRule(String routeId) {
        // 优先返回路由特定配置
        CircuitBreakerRuleDTO rule = circuitBreakerCache.get(routeId);
        if (rule != null && Boolean.TRUE.equals(rule.getEnabled())) {
            return rule;
        }
        // 返回全局配置
        return circuitBreakerCache.get("global");
    }

    /**
     * 获取全局熔断规则
     *
     * @return 全局熔断规则
     */
    public CircuitBreakerRuleDTO getGlobalCircuitBreakerRule() {
        return circuitBreakerCache.get("global");
    }

    /**
     * 获取路由的灰度规则
     *
     * @param routeId 路由ID
     * @return 灰度规则列表
     */
    public List<CanaryRuleDTO> getCanaryRules(String routeId) {
        return canaryRuleCache.getOrDefault(routeId, Collections.emptyList());
    }

    /**
     * 获取所有灰度规则
     *
     * @return 所有灰度规则
     */
    public List<CanaryRuleDTO> getAllCanaryRules() {
        return canaryRuleCache.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * 匹配路由模式
     *
     * @param routeId  路由ID
     * @param patterns 路由模式，逗号分隔支持通配符
     * @return 是否匹配
     */
    private boolean matchesRoutePattern(String routeId, String patterns) {
        if (StringUtils.isBlank(patterns)) {
            return true; // 空模式匹配所有
        }
        if (StringUtils.isBlank(routeId)) {
            return true; // 空路由ID匹配所有
        }
        return Arrays.stream(patterns.split(","))
                .map(String::trim)
                .anyMatch(pattern -> {
                    if (pattern.contains("*")) {
                        String regex = pattern.replace("*", ".*");
                        return Pattern.matches(regex, routeId);
                    }
                    return pattern.equals(routeId);
                });
    }

    /**
     * 获取功能缓存统计
     */
    public Map<String, Integer> getCacheStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("featureConfigs", featureCache.size());
        stats.put("circuitBreakerRules", circuitBreakerCache.size());
        stats.put("canaryRoutes", canaryRuleCache.size());
        return stats;
    }
}
