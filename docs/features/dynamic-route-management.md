# 动态路由管理功能说明

## 功能概述

动态路由管理是 Lite Gateway Core 的核心功能之一，允许在运行时动态添加、修改和删除路由配置，而无需重启网关服务。

## 实现方式

### 核心组件

1. **DynamicRouteDefinitionRepository**：实现了 Spring Cloud Gateway 的 `RouteDefinitionRepository` 接口，负责路由定义的存储和管理。

2. **RouteController**：提供 REST API 接口，用于管理路由配置。

### 关键实现细节

- **内存存储**：路由定义默认存储在内存中，支持从内存/数据库/Nacos 加载路由配置。

- **路由构建**：提供了多个重载的 `buildRouteDefinition` 方法，支持各种路由配置选项，包括：
  - 基本路由配置（ID、URI、路径）
  - 权重配置（用于灰度发布）
  - 过滤器配置（StripPrefix、限流等）
  - 高级路由特性（API 密钥认证、细粒度权限控制等）

- **路由操作 API**：提供了完整的 CRUD 操作 API，包括：
  - 分页查询路由列表
  - 查询路由详情
  - 添加路由
  - 更新路由
  - 删除路由
  - 刷新配置

### 代码示例

```java
// 构建路由定义
RouteDefinition routeDefinition = dynamicRouteDefinitionRepository.buildRouteDefinition(
    "test-route",
    "http://localhost:8080",
    "/test/**",
    1,  // stripPrefix
    null,  // weight
    null,  // weightName
    10,  // replenishRate
    20   // burstCapacity
);

// 保存路由
dynamicRouteDefinitionRepository.save(Mono.just(routeDefinition)).block();
```

## 应用场景

1. **动态服务发现**：当后端服务实例发生变化时，可以动态更新路由配置。

2. **灰度发布**：通过权重配置，实现流量的精细控制，支持灰度发布场景。

3. **A/B 测试**：可以为不同版本的服务配置不同的路由规则，实现 A/B 测试。

4. **紧急故障处理**：当某个服务出现故障时，可以快速修改路由配置，将流量转移到备用服务。

5. **多环境部署**：可以为不同环境（开发、测试、生产）配置不同的路由规则。

## 功能意义

1. **提高系统弹性**：无需重启即可更新路由配置，减少服务中断时间。

2. **简化运维**：通过 API 接口管理路由，无需修改配置文件和重启服务。

3. **支持复杂场景**：提供了丰富的路由配置选项，支持各种复杂的业务场景。

4. **提升系统可靠性**：当后端服务发生变化时，可以快速响应，确保系统的正常运行。

5. **便于集成**：与服务发现、配置中心等组件集成，实现自动化的路由管理。

## 配置与使用

### 路由配置示例

```yaml
# 示例路由配置
spring:
  cloud:
    gateway:
      routes:
        - id: test-route
          uri: http://localhost:8080
          predicates:
            - Path=/test/**
          filters:
            - StripPrefix=1
            - name: RequestRateLimiter
              args:
                key-resolver: '#{@hostAddrKeyResolver}'
                replenishRate: 10
                burstCapacity: 20
```

### API 接口使用

1. **查询路由列表**：`GET /gateway/route/list`

2. **添加路由**：`POST /gateway/route`

3. **更新路由**：`PUT /gateway/route/{id}`

4. **删除路由**：`DELETE /gateway/route/{id}`

5. **刷新配置**：`POST /gateway/route/reload`

## 注意事项

1. **性能考虑**：路由配置过多可能会影响网关性能，建议合理规划路由数量。

2. **安全性**：路由配置 API 应该进行适当的权限控制，防止未授权访问。

3. **一致性**：在分布式环境中，需要确保路由配置的一致性，可以考虑使用配置中心进行管理。

4. **备份**：建议定期备份路由配置，以防止配置丢失。

## 总结

动态路由管理功能是 Lite Gateway Core 的核心特性之一，通过提供灵活的路由配置和管理能力，为系统的可靠性、弹性和可维护性提供了有力保障。它不仅简化了运维工作，还支持各种复杂的业务场景，是构建现代化微服务架构的重要组成部分。