package com.litegateway.core.dto;

import java.util.List;

/**
 * 网关配置 DTO
 */
public class GatewayConfigDTO {

    private Long version;
    private List<RouteDTO> routes;
    private List<IpBlackDTO> ipBlacklist;
    private List<WhiteListDTO> whiteList;
    private List<FeatureConfigDTO> featureConfigs;
    private List<CircuitBreakerRuleDTO> circuitBreakerRules;
    private List<CanaryRuleDTO> canaryRules;

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<RouteDTO> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteDTO> routes) {
        this.routes = routes;
    }

    public List<IpBlackDTO> getIpBlacklist() {
        return ipBlacklist;
    }

    public void setIpBlacklist(List<IpBlackDTO> ipBlacklist) {
        this.ipBlacklist = ipBlacklist;
    }

    public List<WhiteListDTO> getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(List<WhiteListDTO> whiteList) {
        this.whiteList = whiteList;
    }

    public List<FeatureConfigDTO> getFeatureConfigs() {
        return featureConfigs;
    }

    public void setFeatureConfigs(List<FeatureConfigDTO> featureConfigs) {
        this.featureConfigs = featureConfigs;
    }

    public List<CircuitBreakerRuleDTO> getCircuitBreakerRules() {
        return circuitBreakerRules;
    }

    public void setCircuitBreakerRules(List<CircuitBreakerRuleDTO> circuitBreakerRules) {
        this.circuitBreakerRules = circuitBreakerRules;
    }

    public List<CanaryRuleDTO> getCanaryRules() {
        return canaryRules;
    }

    public void setCanaryRules(List<CanaryRuleDTO> canaryRules) {
        this.canaryRules = canaryRules;
    }
}
