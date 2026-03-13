package com.litegateway.core.dto;

/**
 * 服务实例数据传输对象
 * 用于前后端数据传输
 */
public class InstanceDTO {
    private String instanceId;
    private String serviceName;
    private Boolean enabled;
    private Double weight;

    // Getters and Setters
    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }
}
