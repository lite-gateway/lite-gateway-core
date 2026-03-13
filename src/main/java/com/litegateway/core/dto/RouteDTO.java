package com.litegateway.core.dto;

/**
 * 路由数据传输对象
 * 用于前后端数据传输
 */
public class RouteDTO {
    private Long id;
    private String systemCode;
    private String name;
    private String uri;
    private String path;
    private Integer stripPrefix;
    private String host;
    private String remoteAddr;
    private String header;
    private String filterRateLimiterName;
    private Integer replenishRate;
    private Integer burstCapacity;
    private String status;
    private String requestParameter;
    private String rewritePath;
    private String createBy;
    private String createTime;
    private String updateBy;
    private String updateTime;
    private String weightName;
    private Integer weight;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public void setSystemCode(String systemCode) {
        this.systemCode = systemCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getStripPrefix() {
        return stripPrefix;
    }

    public void setStripPrefix(Integer stripPrefix) {
        this.stripPrefix = stripPrefix;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getFilterRateLimiterName() {
        return filterRateLimiterName;
    }

    public void setFilterRateLimiterName(String filterRateLimiterName) {
        this.filterRateLimiterName = filterRateLimiterName;
    }

    public Integer getReplenishRate() {
        return replenishRate;
    }

    public void setReplenishRate(Integer replenishRate) {
        this.replenishRate = replenishRate;
    }

    public Integer getBurstCapacity() {
        return burstCapacity;
    }

    public void setBurstCapacity(Integer burstCapacity) {
        this.burstCapacity = burstCapacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRequestParameter() {
        return requestParameter;
    }

    public void setRequestParameter(String requestParameter) {
        this.requestParameter = requestParameter;
    }

    public String getRewritePath() {
        return rewritePath;
    }

    public void setRewritePath(String rewritePath) {
        this.rewritePath = rewritePath;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getWeightName() {
        return weightName;
    }

    public void setWeightName(String weightName) {
        this.weightName = weightName;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    // 新增方法
    public String getRouteId() {
        return id != null ? id.toString() : null;
    }

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
