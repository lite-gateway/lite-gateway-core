package com.litegateway.core.dto;

/**
 * 灰度规则 DTO
 */
public class CanaryRuleDTO {

    private String ruleId;
    private String ruleName;
    private String routeId;
    private Integer canaryWeight;
    private String canaryVersion;
    private String stableVersion;
    private String matchType;
    private String matchConfig;
    private String headerName;
    private String headerValue;
    private String cookieName;
    private String queryParam;
    private Boolean enabled;

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public Integer getCanaryWeight() {
        return canaryWeight;
    }

    public void setCanaryWeight(Integer canaryWeight) {
        this.canaryWeight = canaryWeight;
    }

    public String getCanaryVersion() {
        return canaryVersion;
    }

    public void setCanaryVersion(String canaryVersion) {
        this.canaryVersion = canaryVersion;
    }

    public String getStableVersion() {
        return stableVersion;
    }

    public void setStableVersion(String stableVersion) {
        this.stableVersion = stableVersion;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public String getMatchConfig() {
        return matchConfig;
    }

    public void setMatchConfig(String matchConfig) {
        this.matchConfig = matchConfig;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getHeaderValue() {
        return headerValue;
    }

    public void setHeaderValue(String headerValue) {
        this.headerValue = headerValue;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public String getQueryParam() {
        return queryParam;
    }

    public void setQueryParam(String queryParam) {
        this.queryParam = queryParam;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
