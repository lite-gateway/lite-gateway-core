package com.litegateway.core.dto;

/**
 * 接口信息数据传输对象
 * 用于前后端数据传输
 */
public class InterfaceDTO {
    private String path;
    private String summary;
    private String type;
    private String tag;
    private String ifAdd;

    // Getters and Setters
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getIfAdd() {
        return ifAdd;
    }

    public void setIfAdd(String ifAdd) {
        this.ifAdd = ifAdd;
    }
}
