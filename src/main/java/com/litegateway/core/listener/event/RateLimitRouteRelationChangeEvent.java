package com.litegateway.core.listener.event;

import org.springframework.context.ApplicationEvent;

/**
 * 限流规则与路由关联变更事件
 * 当限流规则与路由的关联关系发生变更时触发
 * 用于通知 Gateway 重建路由（低频操作）
 */
public class RateLimitRouteRelationChangeEvent extends ApplicationEvent {

    private final String routeId;
    private final String ruleId;
    private final ChangeType changeType;

    public enum ChangeType {
        BIND,      // 绑定
        UNBIND     // 解绑
    }

    public RateLimitRouteRelationChangeEvent(Object source, String routeId, String ruleId, ChangeType changeType) {
        super(source);
        this.routeId = routeId;
        this.ruleId = ruleId;
        this.changeType = changeType;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public ChangeType getChangeType() {
        return changeType;
    }
}
