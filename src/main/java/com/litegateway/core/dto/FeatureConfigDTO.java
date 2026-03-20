package com.litegateway.core.dto;

/**
 * 功能配置 DTO
 */
public class FeatureConfigDTO {

    private String featureCode;
    private String featureName;
    private Boolean enabled;
    private String configJson;
    private Integer priority;
    private String routePatterns;

    public String getFeatureCode() {
        return featureCode;
    }

    public void setFeatureCode(String featureCode) {
        this.featureCode = featureCode;
    }

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getRoutePatterns() {
        return routePatterns;
    }

    public void setRoutePatterns(String routePatterns) {
        this.routePatterns = routePatterns;
    }
}
