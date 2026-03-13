package com.litegateway.core.dto;

/**
 * IP黑名单 DTO
 */
public class IpBlackDTO {

    private String ip;
    private String remark;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
