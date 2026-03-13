package com.litegateway.core.cache;

import com.litegateway.core.dto.WhiteListDTO;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 白名单缓存
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway.core
 */
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
