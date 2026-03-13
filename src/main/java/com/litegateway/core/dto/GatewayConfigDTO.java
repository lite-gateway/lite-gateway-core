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
}
