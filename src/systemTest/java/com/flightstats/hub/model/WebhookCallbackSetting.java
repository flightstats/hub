package com.flightstats.hub.model;

import lombok.Builder;

import java.beans.ConstructorProperties;

@Builder
public class WebhookCallbackSetting {
    Integer triesUntilSuccess;
    Integer failureStatusCode;

    @ConstructorProperties({"triesUntilSuccess", "failureStatusCode"})
    public WebhookCallbackSetting(Integer triesUntilSuccess, Integer failureStatusCode) {
        this.triesUntilSuccess = triesUntilSuccess;
        this.failureStatusCode = failureStatusCode;
    }
}
