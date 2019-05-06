package com.flightstats.hub.app;

import com.flightstats.hub.exception.ForbiddenRequestException;

public class PermissionsChecker {

    public static void checkReadOnlyPermission(String failureMessage) {
        if (HubProperties.isReadOnly()) {
            throw new ForbiddenRequestException(failureMessage);
        }
    }

    public static void checkWebhookLeadershipPermission(String failureMessage) {
        if (!HubProperties.isWebHookLeadershipEnabled()) {
            throw new ForbiddenRequestException(failureMessage);
        }
    }

}
