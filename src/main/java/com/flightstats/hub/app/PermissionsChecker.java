package com.flightstats.hub.app;

import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.WebhookProperties;
import com.flightstats.hub.exception.ForbiddenRequestException;

import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class PermissionsChecker {
    private final AppProperties appProperties;
    private final WebhookProperties webhookProperties;

    @Inject
    public PermissionsChecker(AppProperties appProperties, WebhookProperties webhookProperties) {
        this.appProperties = appProperties;
        this.webhookProperties = webhookProperties;
    }

    public void checkReadOnlyPermission(String failureMessage) {
        if (appProperties.isReadOnly()) {
            throw new ForbiddenRequestException(failureMessage);
        }
    }

    public void checkWebhookLeadershipPermission(String failureMessage) {
        if (!webhookProperties.isWebhookLeadershipEnabled()) {
            throw new ForbiddenRequestException(failureMessage);
        }
    }

}
