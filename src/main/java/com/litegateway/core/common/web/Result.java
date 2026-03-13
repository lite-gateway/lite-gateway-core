package com.litegateway.core.common.web;

/**
 * 统一响应结果封装
 * 与 Admin 模块保持一致
 */
public class Result<T> {

    private String code;
    private String message;
    private T data;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isOk() {
        //先写200个把
        return "00000".equals(this.code)|| "200".equals(this.code);
    }
    
    /**
     * 成功响应
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode("200");
        result.setMessage("success");
        return result;
    }
    
    /**
     * 成功响应带数据
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode("200");
        result.setMessage("success");
        result.setData(data);
        return result;
    }
    
    /**
     * 失败响应
     */
    public static <T> Result<T> fail(String message) {
        Result<T> result = new Result<>();
        result.setCode("500");
        result.setMessage(message);
        return result;
    }
}
