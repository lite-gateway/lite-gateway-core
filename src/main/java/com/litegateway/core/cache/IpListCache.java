package com.litegateway.core.cache;

import com.litegateway.core.dto.IpBlackDTO;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP 黑名单缓存
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway.core
 */
public class IpListCache {
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

    public static List<IpBlackDTO> getAll() {
        List<IpBlackDTO> ipList = new ArrayList<>();
        for (Map.Entry<String, Object> entity : cacheMap.entrySet()) {
            IpBlackDTO vo = new IpBlackDTO();
            vo.setIp(entity.getKey());
            vo.setRemark((String) entity.getValue());
            ipList.add(vo);
        }
        return ipList;
    }

    public static synchronized void clear() {
        cacheMap.clear();
    }

    public static boolean contains(String ip) {
        return cacheMap.containsKey(ip);
    }
}
