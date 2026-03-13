# 缓存管理功能说明

## 功能概述

缓存管理是 Lite Gateway Core 的重要功能，用于缓存路由配置、白名单、IP 黑名单等数据，提高系统性能和响应速度。

## 实现方式

### 核心组件

1. **WhiteListCache**：白名单缓存，用于存储不需要认证的路径。

2. **IpListCache**：IP 黑名单缓存，用于存储需要被阻止的 IP 地址。

3. **RequestCacheFilter**：请求缓存过滤器，用于缓存请求结果。

4. **ResponseCacheFilter**：响应缓存过滤器，用于缓存响应结果。

### 关键实现细节

- **内存缓存**：使用 `ConcurrentHashMap` 实现内存缓存，提供线程安全的操作。

- **缓存操作**：提供了完整的缓存操作方法，包括添加、获取、删除、清空等。

- **缓存刷新**：支持通过事件机制刷新缓存，如 `WhiteListRefreshEvent` 和 `DataIpRefreshEvent`。

- **请求/响应缓存**：通过过滤器实现请求和响应的缓存，提高系统性能。

### 代码示例

```java
// WhiteListCache 核心实现
public class WhiteListCache {
    private static ConcurrentHashMap<String, Object> cacheMap = new ConcurrentHashMap<>();

    public static void put(final String key, final Object value) {
        Assert.notNull(key, "hash map key cannot be null");
        Assert.notNull(value, "hash map value cannot be null");
        cacheMap.put(key, value);
    }

    public static Object get(final String key) {
        return cacheMap.get(key);
    }

    public static synchronized void remove(final String key) {
        if (cacheMap.containsKey(key)) {
            cacheMap.remove(key);
        }
    }

    public static List<WhiteListDTO> getAll() {
        List<WhiteListDTO> list = new ArrayList<>();
        for (Map.Entry<String, Object> entity : cacheMap.entrySet()) {
            WhiteListDTO vo = new WhiteListDTO();
            vo.setPath(entity.getKey());
            vo.setDescription((String) entity.getValue());
            list.add(vo);
        }
        return list;
    }

    public static Set<String> getKeySet() {
        return cacheMap.keySet();
    }

    public static synchronized void clear() {
        cacheMap.clear();
    }

    public static boolean contains(String path) {
        return cacheMap.containsKey(path);
    }
}
```

## 应用场景

1. **白名单管理**：缓存不需要认证的路径，提高认证效率。

2. **IP 黑名单管理**：缓存需要被阻止的 IP 地址，提高访问控制效率。

3. **请求缓存**：缓存重复请求的结果，减少后端服务的负载。

4. **响应缓存**：缓存响应结果，提高系统响应速度。

5. **配置缓存**：缓存路由配置等信息，减少配置加载时间。

## 功能意义

1. **提高系统性能**：通过缓存减少重复计算和数据库查询，提高系统响应速度。

2. **减轻后端负担**：通过缓存请求和响应结果，减少后端服务的负载。

3. **提高系统可靠性**：在后端服务不可用时，缓存可以提供备用数据。

4. **简化系统设计**：通过缓存，系统可以更简单地处理数据访问逻辑。

5. **支持高并发**：缓存可以快速响应大量请求，支持高并发场景。

## 配置与使用

### 路由配置示例

```yaml
# 带缓存的路由
spring:
  cloud:
    gateway:
      routes:
        - id: api-route
          uri: http://localhost:8081
          predicates:
            - Path=/api/**
          filters:
            - name: RequestCache
              args:
                cacheTtl: 60
            - name: ResponseCache
              args:
                cacheTtl: 60
```

### 缓存操作示例

```java
// 添加白名单路径
WhiteListCache.put("/public/**", "公共路径，无需认证");

// 检查路径是否在白名单中
boolean isWhitelisted = WhiteListCache.contains("/public/api");

// 获取所有白名单
List<WhiteListDTO> whiteList = WhiteListCache.getAll();
```

## 注意事项

1. **缓存一致性**：确保缓存与数据源的一致性，避免数据过期或不一致。

2. **缓存容量**：合理设置缓存容量，避免缓存过大导致内存溢出。

3. **缓存过期**：为缓存设置合理的过期时间，避免缓存数据过时。

4. **缓存更新**：确保缓存能够及时更新，反映最新的数据状态。

5. **缓存监控**：监控缓存的使用情况，及时发现和解决缓存问题。

## 总结

缓存管理功能是 Lite Gateway Core 的重要特性，通过缓存数据提高系统性能和响应速度。它不仅可以减轻后端服务的负载，还可以提高系统的可靠性和并发处理能力，是构建高性能微服务架构的重要组成部分。