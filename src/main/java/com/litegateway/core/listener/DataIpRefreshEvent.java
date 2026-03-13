package com.litegateway.core.listener;

import org.springframework.context.ApplicationEvent;

/**
 * IP 黑名单刷新事件
 */
public class DataIpRefreshEvent extends ApplicationEvent {

    public DataIpRefreshEvent(Object source) {
        super(source);
    }
}
