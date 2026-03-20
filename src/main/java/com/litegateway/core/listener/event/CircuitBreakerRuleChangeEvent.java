package com.litegateway.core.listener.event;

import org.springframework.context.ApplicationEvent;

/**
 * 熔断规则变更事件
 */
public class CircuitBreakerRuleChangeEvent extends ApplicationEvent {

    private final String ruleId;

    public CircuitBreakerRuleChangeEvent(Object source, String ruleId) {
        super(source);
        this.ruleId = ruleId;
    }

    public String getRuleId() {
        return ruleId;
    }
}
