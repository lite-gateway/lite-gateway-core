package com.litegateway.core.listener.event;

import org.springframework.context.ApplicationEvent;

/**
 * 灰度规则变更事件
 */
public class CanaryRuleChangeEvent extends ApplicationEvent {

    private final String ruleId;
    private final String routeId;

    public CanaryRuleChangeEvent(Object source, String ruleId, String routeId) {
        super(source);
        this.ruleId = ruleId;
        this.routeId = routeId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getRouteId() {
        return routeId;
    }
}
