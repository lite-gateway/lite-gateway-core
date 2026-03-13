package com.litegateway.core.dto;

/**
 * 白名单 DTO
 */
public class WhiteListDTO {

    private String path;
    private String description;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
