package com.litegateway.core.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 路由数据传输对象
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway
 */
@Data
public class RouteDTO {

    /** id */
    private Long id;

    /** 系统代号 */
    private String systemCode;

    /** 名称 */
    private String name;

    /** 服务地址 */
    private String uri;

    /** 断言地址 */
    private String path;

    /** 断言截取 */
    private Integer stripPrefix;

    /** 断言主机 */
    private String host;

    /** 断言远程地址 */
    private String remoteAddr;

    /** 断言Headers */
    private String header;

    /** 限流器 */
    private String filterRateLimiterName;

    /** 每秒流量 */
    private Integer replenishRate;

    /** 令牌总量 */
    private Integer burstCapacity;

    /** 状态，0启用，1禁用 */
    private String status;

    /** 请求参数 */
    private String requestParameter;

    /** 重写Path路径 */
    private String rewritePath;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新人 */
    private String updateBy;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 权重名称（隐藏） */
    private String weightName;

    /** 权重（隐藏） */
    private Integer weight;

    public String getTargetProtocol() {
        return null;
    }

    public String getUrlPattern() {
        return null;
    }

    public String getUrlReplacement() {
        return null;
    }
}