# Lite Gateway Core 迁移说明

## 概述
本文档记录了从旧项目 `jtyjy-gateway` 迁移代码到 `lite-gateway-core` 的过程和变更。

## 迁移清单

### 1. 包名修改
- **旧包名**: `com.jtyjy.gateway`
- **新包名**: `com.litegateway.core`

### 2. 核心组件迁移

#### 2.1 过滤器 (Filter)
| 旧文件 | 新文件 | 说明 |
|--------|--------|------|
| `oauth/filter/AuthGlobalFilter.java` | `filter/AuthGlobalFilter.java` | 认证全局过滤器 |
| `oauth/filter/IpBlackListFilter.java` | `filter/IpBlackListFilter.java` | IP黑名单过滤器 |

#### 2.2 配置类 (Config)
| 旧文件 | 新文件 | 说明 |
|--------|--------|------|
| `config/RouteConfig.java` | `config/RouteConfig.java` | 路由配置（限流KeyResolver） |
| `oauth/config/ResourceSecurityConfig.java` | `config/SecurityConfig.java` | 安全配置 |

#### 2.3 缓存 (Cache)
| 旧文件 | 新文件 | 说明 |
|--------|--------|------|
| `cache/IpListCache.java` | `cache/IpListCache.java` | IP黑名单缓存 |
| `cache/WhiteListCache.java` | `cache/WhiteListCache.java` | 白名单缓存 |

#### 2.4 监听器 (Listener)
| 旧文件 | 新文件 | 说明 |
|--------|--------|------|
| `listener/SyncRouteUpdateMessageListener.java` | `listener/SyncRouteUpdateMessageListener.java` | Redis消息监听 |
| `listener/DataIpApplicationEvent.java` | `listener/DataIpRefreshEvent.java` | IP刷新事件 |
| `listener/WhiteListApplicationEvent.java` | `listener/WhiteListRefreshEvent.java` | 白名单刷新事件 |

#### 2.5 路由管理
| 旧文件 | 新文件 | 说明 |
|--------|--------|------|
| `manager/MysqlRouteDefinitionRepository.java` | `route/DynamicRouteDefinitionRepository.java` | 动态路由仓库 |

### 3. 技术栈更新

#### 3.1 Spring Boot 版本
- **旧**: 2.x
- **新**: 3.2.0

#### 3.2 Spring Cloud 版本
- **旧**: 2021.x
- **新**: 2023.0.0

#### 3.3 JSON 处理
- **旧**: Fastjson
- **新**: Jackson

#### 3.4 安全框架
- **旧**: Spring Security OAuth2 (旧版)
- **新**: Spring Security 6 + OAuth2 Resource Server

### 4. 保留的核心逻辑

#### 4.1 Nacos 服务发现
```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:localhost:8848}
```

#### 4.2 Redis 限流
```java
@Bean("hostAddrKeyResolver")
public KeyResolver hostAddrKeyResolver() {
    return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
}
```

#### 4.3 全局过滤器
- **AuthGlobalFilter**: JWT 认证，提取用户信息
- **IpBlackListFilter**: IP 黑名单检查

#### 4.4 动态路由
- 支持从内存/数据库/Nacos 加载路由
- 支持 Redis 消息通知刷新路由

### 5. 新增的安全组件

#### 5.1 JwtAuthenticationManager
处理 JWT Token 认证，提取用户权限。

#### 5.2 UnauthorizedHandler
处理未认证请求，返回标准错误响应。

### 6. 配置文件变更

#### 6.1 application.yml
```yaml
server:
  port: 8088

spring:
  application:
    name: lite-gateway-core
  
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
  
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:localhost:8848}
    gateway:
      discovery:
        locator:
          enabled: true

jwt:
  secret: ${JWT_SECRET:lite-gateway-secret-key-for-jwt-signing}
  expiration: ${JWT_EXPIRATION:86400000}
```

### 7. 文件结构

```
lite-gateway-core/
├── src/main/java/com/litegateway/core/
│   ├── LiteGatewayCoreApplication.java
│   ├── cache/
│   │   ├── IpListCache.java
│   │   └── WhiteListCache.java
│   ├── config/
│   │   ├── RouteConfig.java
│   │   └── SecurityConfig.java
│   ├── constants/
│   │   ├── RedisTypeConstants.java
│   │   ├── RouteConstants.java
│   │   └── StringConstants.java
│   ├── dto/
│   │   ├── IpBlackDTO.java
│   │   ├── UserDTO.java
│   │   └── WhiteListDTO.java
│   ├── filter/
│   │   ├── AuthGlobalFilter.java
│   │   └── IpBlackListFilter.java
│   ├── listener/
│   │   ├── DataIpRefreshEvent.java
│   │   ├── SyncRouteUpdateMessageListener.java
│   │   └── WhiteListRefreshEvent.java
│   ├── route/
│   │   └── DynamicRouteDefinitionRepository.java
│   └── security/
│       ├── JwtAuthenticationManager.java
│       └── UnauthorizedHandler.java
├── src/main/resources/
│   ├── application.yml
│   └── bootstrap.yml
├── pom.xml
└── MIGRATION.md
```

## 后续步骤

1. **添加数据库支持**: 实现从 MySQL 加载路由配置
2. **完善限流配置**: 添加更多限流策略（用户级、API级）
3. **添加监控**: 集成 Micrometer + Prometheus
4. **添加日志链路**: 集成 Sleuth + Zipkin

## 注意事项

1. 当前使用内存存储路由，生产环境建议接入数据库
2. JWT Secret 必须通过环境变量配置
3. Nacos 和 Redis 连接信息建议通过环境变量配置
