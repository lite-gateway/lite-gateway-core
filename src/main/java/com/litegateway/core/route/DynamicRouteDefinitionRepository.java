package com.litegateway.core.route;

import com.litegateway.core.constants.RouteConstants;
import com.litegateway.core.constants.StringConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;

/**
 * 动态路由定义仓库
 * 从旧项目 MysqlRouteDefinitionRepository 迁移
 * 支持从内存/数据库/Nacos 加载路由配置
 * 包名从 com.jtyjy.gateway 改为 com.litegateway.core
 */
@Component
public class DynamicRouteDefinitionRepository implements RouteDefinitionRepository {

    private static final Logger logger = LoggerFactory.getLogger(DynamicRouteDefinitionRepository.class);

    // 内存中的路由定义
    private List<RouteDefinition> routeDefinitions = new ArrayList<>();

    public DynamicRouteDefinitionRepository() {
        // 初始化时加载默认路由
        loadDefaultRoutes();
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        if (routeDefinitions.isEmpty()) {
            loadDefaultRoutes();
        }
        return Flux.fromIterable(routeDefinitions);
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return route.flatMap(routeDefinition -> {
            synchronized (this) {
                boolean exists = false;
                for (int i = 0; i < routeDefinitions.size(); i++) {
                    if (routeDefinitions.get(i).getId().equals(routeDefinition.getId())) {
                        routeDefinitions.set(i, routeDefinition);
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    routeDefinitions.add(routeDefinition);
                }
                logger.info("Route saved: {}", routeDefinition.getId());
                // 这里可以添加持久化逻辑，如保存到Redis或数据库
                return Mono.empty();
            }
        });
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        return routeId.flatMap(id -> {
            synchronized (this) {
                boolean removed = routeDefinitions.removeIf(route -> route.getId().equals(id));
                if (removed) {
                    logger.info("Route deleted: {}", id);
                    // 这里可以添加持久化逻辑，如从Redis或数据库删除
                }
                return Mono.empty();
            }
        });
    }

    /**
     * 根据ID获取路由定义
     */
    public Mono<RouteDefinition> getRouteDefinition(String routeId) {
        return Flux.fromIterable(routeDefinitions)
                .filter(route -> route.getId().equals(routeId))
                .next();
    }

    /**
     * 清空所有路由
     */
    public synchronized void clearRoutes() {
        routeDefinitions.clear();
        logger.info("All routes cleared");
    }

    /**
     * 获取路由数量
     */
    public int getRouteCount() {
        return routeDefinitions.size();
    }

    /**
     * 刷新路由
     */
    public synchronized void refreshRoutes(List<RouteDefinition> newRoutes) {
        this.routeDefinitions = new ArrayList<>(newRoutes);
        logger.info("Routes refreshed, total: {}", routeDefinitions.size());
    }

    /**
     * 加载默认路由（示例）
     */
    private void loadDefaultRoutes() {
        // 这里可以从配置文件或数据库加载
        // 默认提供一些示例路由
        logger.info("Loading default routes...");
        
        // 示例路由1：测试路由
        RouteDefinition testRoute = buildRouteDefinition(
                "test-route",
                "http://localhost:8080",
                "/test/**",
                1,  // stripPrefix
                null,  // weight
                null,  // weightName
                10,  // replenishRate
                20   // burstCapacity
        );
        routeDefinitions.add(testRoute);
        
        // 示例路由2：API路由
        RouteDefinition apiRoute = buildRouteDefinition(
                "api-route",
                "http://localhost:8081",
                "/api/**",
                1,
                null,
                null,
                20,
                40
        );
        routeDefinitions.add(apiRoute);
        
        // 示例路由3：灰度发布路由
        addGrayReleaseRoutes();
        
        logger.info("Default routes loaded, total: {}", routeDefinitions.size());
    }
    
    /**
     * 添加灰度发布路由示例
     */
    private void addGrayReleaseRoutes() {
        // 创建灰度发布路由组，权重比例为 80:20
        String weightGroup = "gray-group";
        
        // 主版本路由（80% 流量）
        RouteDefinition mainRoute = buildRouteDefinition(
                "gray-main-route",
                "http://localhost:8080",
                "/gray/**",
                1,  // stripPrefix
                80,  // weight
                weightGroup,  // weightName
                20,  // replenishRate
                40   // burstCapacity
        );
        routeDefinitions.add(mainRoute);
        
        // 灰度版本路由（20% 流量）
        RouteDefinition grayRoute = buildRouteDefinition(
                "gray-version-route",
                "http://localhost:8082",
                "/gray/**",
                1,  // stripPrefix
                20,  // weight
                weightGroup,  // weightName
                20,  // replenishRate
                40   // burstCapacity
        );
        routeDefinitions.add(grayRoute);
        
        logger.info("Gray release routes added: main (80%) and gray (20%)");
    }

    /**
     * 构建路由定义
     */
    public RouteDefinition buildRouteDefinition(String id, String uri, String path,
                                                 Integer stripPrefix, Integer weight,
                                                 String weightName, Integer replenishRate,
                                                 Integer burstCapacity, String targetProtocol,
                                                 String urlPattern, String urlReplacement,
                                                 boolean enableApiKeyAuth, String requiredPermission,
                                                 String contentBasedKey, String contentBasedValue,
                                                 String contentBasedTargetHost, Integer contentBasedTargetPort,
                                                 String conditionalType, String conditionalKey, String conditionalValue,
                                                 String conditionalTargetHost, Integer conditionalTargetPort,
                                                 String trafficColor, String trafficSource, String trafficEnvironment,
                                                 Integer rateLimitPerSecond, Integer concurrencyLimit,
                                                 Integer requestCacheTtl, Integer responseCacheTtl,
                                                 boolean enableWebSocket, boolean enableGrpc, boolean enableGraphql,
                                                 boolean enableServiceMesh, String meshVersion, String targetService, String targetVersion,
                                                 boolean enableBlueGreen, String activeEnvironment, String serviceBaseUrl, String blueEnvironmentUrl, String greenEnvironmentUrl,
                                                 boolean enableCanary, int canaryWeight, String canaryVersion, String stableVersion, String canaryServiceUrl, String stableServiceUrl) {
        RouteDefinition routeDefinition = new RouteDefinition();
        routeDefinition.setId(id);
        routeDefinition.setUri(getURI(uri));

        // 设置断言
        List<PredicateDefinition> predicates = new ArrayList<>();
        routeDefinition.setPredicates(predicates);

        // 设置过滤器
        List<FilterDefinition> filters = new ArrayList<>();
        routeDefinition.setFilters(filters);

        // 权重
        if (weight != null && weight > 0) {
            predicates.add(buildPredicate(RouteConstants.WEIGHT, weightName, String.valueOf(weight)));
        }

        // 路径断言
        if (path != null && !path.isEmpty()) {
            predicates.add(buildPredicate(RouteConstants.PATH, path));
        }

        // StripPrefix 过滤器
        if (stripPrefix != null && stripPrefix > 0) {
            filters.add(buildFilter(RouteConstants.STRIP_PREFIX, String.valueOf(stripPrefix)));
        }

        // API密钥认证过滤器
        if (enableApiKeyAuth) {
            FilterDefinition apiKeyFilter = new FilterDefinition();
            apiKeyFilter.setName("ApiKeyAuthentication");
            Map<String, String> apiKeyArgs = new LinkedHashMap<>();
            apiKeyArgs.put("headerName", "X-API-Key");
            apiKeyFilter.setArgs(apiKeyArgs);
            filters.add(apiKeyFilter);
        }

        // 细粒度权限控制过滤器
        if (requiredPermission != null && !requiredPermission.isEmpty()) {
            FilterDefinition permissionFilter = new FilterDefinition();
            permissionFilter.setName("FineGrainedPermission");
            Map<String, String> permissionArgs = new LinkedHashMap<>();
            permissionArgs.put("requiredPermission", requiredPermission);
            permissionFilter.setArgs(permissionArgs);
            filters.add(permissionFilter);
        }

        // 基于请求内容的路由过滤器
        if (contentBasedKey != null && contentBasedValue != null) {
            FilterDefinition contentFilter = new FilterDefinition();
            contentFilter.setName("ContentBasedRouting");
            Map<String, String> contentArgs = new LinkedHashMap<>();
            contentArgs.put("key", contentBasedKey);
            contentArgs.put("value", contentBasedValue);
            contentArgs.put("targetHost", contentBasedTargetHost != null ? contentBasedTargetHost : "localhost");
            contentArgs.put("targetPort", contentBasedTargetPort != null ? String.valueOf(contentBasedTargetPort) : "8080");
            contentFilter.setArgs(contentArgs);
            filters.add(contentFilter);
        }

        // 条件路由过滤器
        if (conditionalType != null && conditionalValue != null) {
            FilterDefinition conditionalFilter = new FilterDefinition();
            conditionalFilter.setName("ConditionalRouting");
            Map<String, String> conditionalArgs = new LinkedHashMap<>();
            conditionalArgs.put("conditionType", conditionalType);
            if (conditionalKey != null) {
                conditionalArgs.put("conditionKey", conditionalKey);
            }
            conditionalArgs.put("conditionValue", conditionalValue);
            conditionalArgs.put("targetHost", conditionalTargetHost != null ? conditionalTargetHost : "localhost");
            conditionalArgs.put("targetPort", conditionalTargetPort != null ? String.valueOf(conditionalTargetPort) : "8080");
            conditionalFilter.setArgs(conditionalArgs);
            filters.add(conditionalFilter);
        }

        // 流量染色过滤器
        if (trafficColor != null) {
            FilterDefinition trafficFilter = new FilterDefinition();
            trafficFilter.setName("TrafficColoring");
            Map<String, String> trafficArgs = new LinkedHashMap<>();
            trafficArgs.put("color", trafficColor);
            trafficArgs.put("source", trafficSource != null ? trafficSource : "gateway");
            trafficArgs.put("environment", trafficEnvironment != null ? trafficEnvironment : "production");
            trafficFilter.setArgs(trafficArgs);
            filters.add(trafficFilter);
        }

        // 流量控制过滤器
        if (rateLimitPerSecond != null || concurrencyLimit != null) {
            FilterDefinition controlFilter = new FilterDefinition();
            controlFilter.setName("TrafficControl");
            Map<String, String> controlArgs = new LinkedHashMap<>();
            controlArgs.put("rateLimitPerSecond", rateLimitPerSecond != null ? String.valueOf(rateLimitPerSecond) : "100");
            controlArgs.put("concurrencyLimit", concurrencyLimit != null ? String.valueOf(concurrencyLimit) : "100");
            controlFilter.setArgs(controlArgs);
            filters.add(controlFilter);
        }

        // 请求缓存过滤器
        if (requestCacheTtl != null) {
            FilterDefinition requestCacheFilter = new FilterDefinition();
            requestCacheFilter.setName("RequestCache");
            Map<String, String> requestCacheArgs = new LinkedHashMap<>();
            requestCacheArgs.put("cacheTtl", String.valueOf(requestCacheTtl));
            requestCacheFilter.setArgs(requestCacheArgs);
            filters.add(requestCacheFilter);
        }

        // 响应缓存过滤器
        if (responseCacheTtl != null) {
            FilterDefinition responseCacheFilter = new FilterDefinition();
            responseCacheFilter.setName("ResponseCache");
            Map<String, String> responseCacheArgs = new LinkedHashMap<>();
            responseCacheArgs.put("cacheTtl", String.valueOf(responseCacheTtl));
            responseCacheFilter.setArgs(responseCacheArgs);
            filters.add(responseCacheFilter);
        }

        // WebSocket支持过滤器
        if (enableWebSocket) {
            FilterDefinition webSocketFilter = new FilterDefinition();
            webSocketFilter.setName("WebSocketSupport");
            filters.add(webSocketFilter);
        }

        // gRPC支持过滤器
        if (enableGrpc) {
            FilterDefinition grpcFilter = new FilterDefinition();
            grpcFilter.setName("GrpcSupport");
            filters.add(grpcFilter);
        }

        // GraphQL支持过滤器
        if (enableGraphql) {
            FilterDefinition graphqlFilter = new FilterDefinition();
            graphqlFilter.setName("GraphQLSupport");
            filters.add(graphqlFilter);
        }

        // 服务网格集成过滤器
        if (enableServiceMesh && targetService != null) {
            FilterDefinition serviceMeshFilter = new FilterDefinition();
            serviceMeshFilter.setName("ServiceMeshIntegration");
            Map<String, String> serviceMeshArgs = new LinkedHashMap<>();
            serviceMeshArgs.put("meshVersion", meshVersion != null ? meshVersion : "1.10.0");
            serviceMeshArgs.put("targetService", targetService);
            serviceMeshArgs.put("targetVersion", targetVersion != null ? targetVersion : "v1");
            serviceMeshFilter.setArgs(serviceMeshArgs);
            filters.add(serviceMeshFilter);
        }

        // 蓝绿部署过滤器
        if (enableBlueGreen && serviceBaseUrl != null && blueEnvironmentUrl != null && greenEnvironmentUrl != null) {
            FilterDefinition blueGreenFilter = new FilterDefinition();
            blueGreenFilter.setName("BlueGreenDeployment");
            Map<String, String> blueGreenArgs = new LinkedHashMap<>();
            blueGreenArgs.put("activeEnvironment", activeEnvironment != null ? activeEnvironment : "blue");
            blueGreenArgs.put("serviceBaseUrl", serviceBaseUrl);
            blueGreenArgs.put("blueEnvironmentUrl", blueEnvironmentUrl);
            blueGreenArgs.put("greenEnvironmentUrl", greenEnvironmentUrl);
            blueGreenFilter.setArgs(blueGreenArgs);
            filters.add(blueGreenFilter);
        }

        // 金丝雀发布过滤器
        if (enableCanary && serviceBaseUrl != null && canaryServiceUrl != null && stableServiceUrl != null) {
            FilterDefinition canaryFilter = new FilterDefinition();
            canaryFilter.setName("CanaryDeployment");
            Map<String, String> canaryArgs = new LinkedHashMap<>();
            canaryArgs.put("canaryWeight", String.valueOf(canaryWeight));
            canaryArgs.put("canaryVersion", canaryVersion != null ? canaryVersion : "v2");
            canaryArgs.put("stableVersion", stableVersion != null ? stableVersion : "v1");
            canaryArgs.put("serviceBaseUrl", serviceBaseUrl);
            canaryArgs.put("canaryServiceUrl", canaryServiceUrl);
            canaryArgs.put("stableServiceUrl", stableServiceUrl);
            canaryFilter.setArgs(canaryArgs);
            filters.add(canaryFilter);
        }

        // 请求重写过滤器
        if (urlPattern != null && urlReplacement != null) {
            FilterDefinition rewriteFilter = new FilterDefinition();
            rewriteFilter.setName("RequestRewrite");
            Map<String, String> rewriteArgs = new LinkedHashMap<>();
            rewriteArgs.put("urlPattern", urlPattern);
            rewriteArgs.put("urlReplacement", urlReplacement);
            rewriteFilter.setArgs(rewriteArgs);
            filters.add(rewriteFilter);
        }

        // 协议转换过滤器
        if (targetProtocol != null && !"http".equals(targetProtocol)) {
            FilterDefinition protocolFilter = new FilterDefinition();
            protocolFilter.setName("ProtocolTransform");
            Map<String, String> protocolArgs = new LinkedHashMap<>();
            protocolArgs.put("targetProtocol", targetProtocol);
            protocolFilter.setArgs(protocolArgs);
            filters.add(protocolFilter);
        }

        // 重试过滤器
        FilterDefinition retryFilter = new FilterDefinition();
        retryFilter.setName("Retry");
        Map<String, String> retryArgs = new LinkedHashMap<>();
        retryArgs.put("retries", "3");
        retryArgs.put("statuses", "INTERNAL_SERVER_ERROR,BAD_GATEWAY,SERVICE_UNAVAILABLE,GATEWAY_TIMEOUT");
        retryArgs.put("methods", "GET,POST,PUT,DELETE");
        retryArgs.put("backoff.firstBackoff", "100ms");
        retryArgs.put("backoff.maxBackoff", "1s");
        retryArgs.put("backoff.factor", "2");
        retryArgs.put("backoff.basedOnPreviousValue", "false");
        retryFilter.setArgs(retryArgs);
        filters.add(retryFilter);

        // 限流
        if (replenishRate != null && burstCapacity != null) {
            FilterDefinition filter = new FilterDefinition();
            filter.setName(RouteConstants.Limiter.CUSTOM_REQUEST_RATE_LIMITER);
            Map<String, String> args = new LinkedHashMap<>();
            args.put(RouteConstants.Limiter.KEY_RESOLVER, RouteConstants.Limiter.HOST_ADDR_KEY_RESOLVER);
            args.put(RouteConstants.Limiter.REPLENISH_RATE, String.valueOf(replenishRate));
            args.put(RouteConstants.Limiter.BURS_CAPACITY, String.valueOf(burstCapacity));
            filter.setArgs(args);
            filters.add(filter);
        }

        return routeDefinition;
    }
    
    /**
     * 构建路由定义（重载方法，默认不进行流量管理、缓存、高级协议支持、服务网格集成和高级部署特性）
     */
    public RouteDefinition buildRouteDefinition(String id, String uri, String path,
                                                 Integer stripPrefix, Integer weight,
                                                 String weightName, Integer replenishRate,
                                                 Integer burstCapacity, String targetProtocol,
                                                 String urlPattern, String urlReplacement,
                                                 boolean enableApiKeyAuth, String requiredPermission,
                                                 String contentBasedKey, String contentBasedValue,
                                                 String contentBasedTargetHost, Integer contentBasedTargetPort,
                                                 String conditionalType, String conditionalKey, String conditionalValue,
                                                 String conditionalTargetHost, Integer conditionalTargetPort,
                                                 String trafficColor, String trafficSource, String trafficEnvironment,
                                                 Integer rateLimitPerSecond, Integer concurrencyLimit,
                                                 Integer requestCacheTtl, Integer responseCacheTtl,
                                                 boolean enableWebSocket, boolean enableGrpc, boolean enableGraphql,
                                                 boolean enableServiceMesh, String meshVersion, String targetService, String targetVersion) {
        return buildRouteDefinition(id, uri, path, stripPrefix, weight, weightName, replenishRate, burstCapacity, targetProtocol, urlPattern, urlReplacement, enableApiKeyAuth, requiredPermission, contentBasedKey, contentBasedValue, contentBasedTargetHost, contentBasedTargetPort, conditionalType, conditionalKey, conditionalValue, conditionalTargetHost, conditionalTargetPort, trafficColor, trafficSource, trafficEnvironment, rateLimitPerSecond, concurrencyLimit, requestCacheTtl, responseCacheTtl, enableWebSocket, enableGrpc, enableGraphql, enableServiceMesh, meshVersion, targetService, targetVersion, false, null, null, null, null, false, 10, null, null, null, null);
    }
    
    /**
     * 构建路由定义（重载方法，默认不进行流量管理、缓存、高级协议支持和服务网格集成）
     */
    public RouteDefinition buildRouteDefinition(String id, String uri, String path,
                                                 Integer stripPrefix, Integer weight,
                                                 String weightName, Integer replenishRate,
                                                 Integer burstCapacity, String targetProtocol,
                                                 String urlPattern, String urlReplacement,
                                                 boolean enableApiKeyAuth, String requiredPermission,
                                                 String contentBasedKey, String contentBasedValue,
                                                 String contentBasedTargetHost, Integer contentBasedTargetPort,
                                                 String conditionalType, String conditionalKey, String conditionalValue,
                                                 String conditionalTargetHost, Integer conditionalTargetPort,
                                                 String trafficColor, String trafficSource, String trafficEnvironment,
                                                 Integer rateLimitPerSecond, Integer concurrencyLimit,
                                                 Integer requestCacheTtl, Integer responseCacheTtl,
                                                 boolean enableWebSocket, boolean enableGrpc, boolean enableGraphql) {
        return buildRouteDefinition(id, uri, path, stripPrefix, weight, weightName, replenishRate, burstCapacity, targetProtocol, urlPattern, urlReplacement, enableApiKeyAuth, requiredPermission, contentBasedKey, contentBasedValue, contentBasedTargetHost, contentBasedTargetPort, conditionalType, conditionalKey, conditionalValue, conditionalTargetHost, conditionalTargetPort, trafficColor, trafficSource, trafficEnvironment, rateLimitPerSecond, concurrencyLimit, requestCacheTtl, responseCacheTtl, enableWebSocket, enableGrpc, enableGraphql, false, null, null, null, false, null, null, null, null, false, 10, null, null, null, null);
    }
    
    /**
     * 构建路由定义（重载方法，默认不进行流量管理、缓存和高级协议支持）
     */
    public RouteDefinition buildRouteDefinition(String id, String uri, String path,
                                                 Integer stripPrefix, Integer weight,
                                                 String weightName, Integer replenishRate,
                                                 Integer burstCapacity, String targetProtocol,
                                                 String urlPattern, String urlReplacement,
                                                 boolean enableApiKeyAuth, String requiredPermission,
                                                 String contentBasedKey, String contentBasedValue,
                                                 String contentBasedTargetHost, Integer contentBasedTargetPort,
                                                 String conditionalType, String conditionalKey, String conditionalValue,
                                                 String conditionalTargetHost, Integer conditionalTargetPort,
                                                 String trafficColor, String trafficSource, String trafficEnvironment,
                                                 Integer rateLimitPerSecond, Integer concurrencyLimit,
                                                 Integer requestCacheTtl, Integer responseCacheTtl) {
        return buildRouteDefinition(id, uri, path, stripPrefix, weight, weightName, replenishRate, burstCapacity, targetProtocol, urlPattern, urlReplacement, enableApiKeyAuth, requiredPermission, contentBasedKey, contentBasedValue, contentBasedTargetHost, contentBasedTargetPort, conditionalType, conditionalKey, conditionalValue, conditionalTargetHost, conditionalTargetPort, trafficColor, trafficSource, trafficEnvironment, rateLimitPerSecond, concurrencyLimit, requestCacheTtl, responseCacheTtl, false, false, false, false, null, null, null, false, null, null, null, null, false, 10, null, null, null, null);
    }
    
    /**
     * 构建路由定义（重载方法，默认不进行流量管理和缓存）
     */
    public RouteDefinition buildRouteDefinition(String id, String uri, String path,
                                                 Integer stripPrefix, Integer weight,
                                                 String weightName, Integer replenishRate,
                                                 Integer burstCapacity, String targetProtocol,
                                                 String urlPattern, String urlReplacement,
                                                 boolean enableApiKeyAuth, String requiredPermission,
                                                 String contentBasedKey, String contentBasedValue,
                                                 String contentBasedTargetHost, Integer contentBasedTargetPort,
                                                 String conditionalType, String conditionalKey, String conditionalValue,
                                                 String conditionalTargetHost, Integer conditionalTargetPort,
                                                 String trafficColor, String trafficSource, String trafficEnvironment,
                                                 Integer rateLimitPerSecond, Integer concurrencyLimit) {
        return buildRouteDefinition(id, uri, path, stripPrefix, weight, weightName, replenishRate, burstCapacity, targetProtocol, urlPattern, urlReplacement, enableApiKeyAuth, requiredPermission, contentBasedKey, contentBasedValue, contentBasedTargetHost, contentBasedTargetPort, conditionalType, conditionalKey, conditionalValue, conditionalTargetHost, conditionalTargetPort, trafficColor, trafficSource, trafficEnvironment, rateLimitPerSecond, concurrencyLimit, null, null, false, false, false, false, null, null, null, false, null, null, null, null, false, 10, null, null, null, null);
    }
    
    /**
     * 构建路由定义（重载方法，默认不进行流量管理）
     */
    public RouteDefinition buildRouteDefinition(String id, String uri, String path,
                                                 Integer stripPrefix, Integer weight,
                                                 String weightName, Integer replenishRate,
                                                 Integer burstCapacity, String targetProtocol,
                                                 String urlPattern, String urlReplacement,
                                                 boolean enableApiKeyAuth, String requiredPermission,
                                                 String contentBasedKey, String contentBasedValue,
                                                 String contentBasedTargetHost, Integer contentBasedTargetPort,
                                                 String conditionalType, String conditionalKey, String conditionalValue,
                                                 String conditionalTargetHost, Integer conditionalTargetPort) {
        return buildRouteDefinition(id, uri, path, stripPrefix, weight, weightName, replenishRate, burstCapacity, targetProtocol, urlPattern, urlReplacement, enableApiKeyAuth, requiredPermission, contentBasedKey, contentBasedValue, contentBasedTargetHost, contentBasedTargetPort, conditionalType, conditionalKey, conditionalValue, conditionalTargetHost, conditionalTargetPort, null, null, null, null, null, null, null, false, false, false, false, null, null, null, false, null, null, null, null, false, 10, null, null, null, null);
    }
    
    /**
     * 构建路由定义（重载方法，默认不进行高级路由）
     */
    public RouteDefinition buildRouteDefinition(String id, String uri, String path,
                                                 Integer stripPrefix, Integer weight,
                                                 String weightName, Integer replenishRate,
                                                 Integer burstCapacity, String targetProtocol,
                                                 String urlPattern, String urlReplacement,
                                                 boolean enableApiKeyAuth, String requiredPermission) {
        return buildRouteDefinition(id, uri, path, stripPrefix, weight, weightName, replenishRate, burstCapacity, targetProtocol, urlPattern, urlReplacement, enableApiKeyAuth, requiredPermission, null, null, null, null, null, null, null, null, null);
    }
    
    /**
     * 构建路由定义（重载方法，默认不进行权限控制）
     */
    public RouteDefinition buildRouteDefinition(String id, String uri, String path,
                                                 Integer stripPrefix, Integer weight,
                                                 String weightName, Integer replenishRate,
                                                 Integer burstCapacity, String targetProtocol,
                                                 String urlPattern, String urlReplacement) {
        return buildRouteDefinition(id, uri, path, stripPrefix, weight, weightName, replenishRate, burstCapacity, targetProtocol, urlPattern, urlReplacement, false, null);
    }
    
    /**
     * 构建路由定义（重载方法，默认不进行请求重写）
     */
    public RouteDefinition buildRouteDefinition(String id, String uri, String path,
                                                 Integer stripPrefix, Integer weight,
                                                 String weightName, Integer replenishRate,
                                                 Integer burstCapacity, String targetProtocol) {
        return buildRouteDefinition(id, uri, path, stripPrefix, weight, weightName, replenishRate, burstCapacity, targetProtocol, null, null);
    }
    
    /**
     * 构建路由定义（重载方法，默认不进行协议转换）
     */
    public RouteDefinition buildRouteDefinition(String id, String uri, String path,
                                                 Integer stripPrefix, Integer weight,
                                                 String weightName, Integer replenishRate,
                                                 Integer burstCapacity) {
        return buildRouteDefinition(id, uri, path, stripPrefix, weight, weightName, replenishRate, burstCapacity, null);
    }

    private PredicateDefinition buildPredicate(String name, String... values) {
        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName(name);
        Map<String, String> args = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            args.put(RouteConstants._GENKEY_ + i, values[i]);
        }
        predicate.setArgs(args);
        return predicate;
    }

    private FilterDefinition buildFilter(String name, String... values) {
        FilterDefinition filter = new FilterDefinition();
        filter.setName(name);
        Map<String, String> args = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i++) {
            args.put(RouteConstants._GENKEY_ + i, values[i]);
        }
        filter.setArgs(args);
        return filter;
    }

    private URI getURI(String uriStr) {
        if (uriStr.startsWith(StringConstants.HTTP) || uriStr.startsWith(StringConstants.HTTPS)) {
            return UriComponentsBuilder.fromHttpUrl(uriStr).build().toUri();
        } else {
            return URI.create(uriStr);
        }
    }
}
