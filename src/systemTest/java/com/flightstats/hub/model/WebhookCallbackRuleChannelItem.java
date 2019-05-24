package com.flightstats.hub.model;

import lombok.Builder;

import java.beans.ConstructorProperties;

@Builder
public class WebhookCallbackRuleChannelItem {
    Integer triesUntilSuccess;
    Integer failureStatusCode;

    @ConstructorProperties({"triesUntilSuccess", "failureStatusCode"})
    public WebhookCallbackRuleChannelItem(Integer triesUntilSuccess, Integer failureStatusCode) {
        this.triesUntilSuccess = triesUntilSuccess;
        this.failureStatusCode = failureStatusCode;
    }
}
