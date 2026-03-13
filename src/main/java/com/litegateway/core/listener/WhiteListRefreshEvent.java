package com.litegateway.core.listener;

import org.springframework.context.ApplicationEvent;

/**
 * 白名单刷新事件
 */
public class WhiteListRefreshEvent extends ApplicationEvent {

    public WhiteListRefreshEvent(Object source) {
        super(source);
    }
}
