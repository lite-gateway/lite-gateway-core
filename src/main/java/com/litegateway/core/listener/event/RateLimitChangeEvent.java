package com.litegateway.core.listener.event;

import org.springframework.context.ApplicationEvent;

/**
 * 限流规则变更事件
 * 当限流规则的配置（如 replenishRate、burstCapacity 等）发生变更时触发
 * 用于通知 Gateway 刷新限流器参数，不重建路由
 */
public class RateLimitChangeEvent extends ApplicationEvent {

    private final String ruleId;

    public RateLimitChangeEvent(Object source, String ruleId) {
        super(source);
        this.ruleId = ruleId;
    }

    public String getRuleId() {
        return ruleId;
    }
}
