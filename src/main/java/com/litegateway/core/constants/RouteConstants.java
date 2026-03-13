package com.litegateway.core.constants;

/**
 * 路由常量
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway.core
 */
public class RouteConstants {

    // 断言类型
    public static final String PATH = "Path";
    public static final String HOST = "Host";
    public static final String REMOTE_ADDR = "RemoteAddr";
    public static final String HEADER = "Header";
    public static final String WEIGHT = "Weight";

    // 过滤器类型
    public static final String STRIP_PREFIX = "StripPrefix";
    public static final String ADD_REQUEST_PARAMETER = "AddRequestParameter";
    public static final String REWRITE_PATH = "RewritePath";

    // 限流相关
    public static final class Limiter {
        public static final String CUSTOM_REQUEST_RATE_LIMITER = "RequestRateLimiter";
        public static final String KEY_RESOLVER = "key-resolver";
        public static final String REPLENISH_RATE = "redis-rate-limiter.replenishRate";
        public static final String BURS_CAPACITY = "redis-rate-limiter.burstCapacity";
        public static final String HOST_ADDR_KEY_RESOLVER = "#{@hostAddrKeyResolver}";
        public static final String URI_KEY_RESOLVER = "#{@uriKeyResolver}";
        public static final String REQUEST_ID_KEY_RESOLVER = "#{@requestIdKeyResolver}";
    }

    public static final String IP = "ip";
    public static final String URI = "uri";
    public static final String REQUEST_ID = "requestId";

    public static final String _GENKEY_ = "_genkey_";
    public static final String SEPARATOR_SIGN = ",";
}
