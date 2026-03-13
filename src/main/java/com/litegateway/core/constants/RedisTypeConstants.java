package com.litegateway.core.constants;

/**
 * Redis 通道常量
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway.core
 */
public class RedisTypeConstants {

    public static final String CHANNEL = "lite:gateway:sync:route:update";
    public static final String IP_UPDATE = "ip_update";
    public static final String ROUTE_UPDATE = "route_update";
    public static final String WHITE_LIST_UPDATE = "white_list_update";
}
