package com.flightstats.hub.system.config;

import javax.inject.Inject;
import javax.inject.Named;

public class ServiceProperties {
    private final String releaseName;
    private final String hubUrlTemplate;
    private final String callbackUrlTemplate;

    @Inject
    public ServiceProperties(
            @Named(PropertiesName.HELM_RELEASE_NAME) String releaseName,
            @Named(PropertiesName.HUB_URL_TEMPLATE) String hubUrlTemplate,
            @Named(PropertiesName.CALLBACK_URL_TEMPLATE) String callbackUrlTemplate) {
        this.releaseName = releaseName;
        this.hubUrlTemplate = hubUrlTemplate;
        this.callbackUrlTemplate = callbackUrlTemplate;
    }

    public String getHubUrl() {
        return String.format(hubUrlTemplate, releaseName);
    }

    public String getCallbackUrl() {
        return String.format(callbackUrlTemplate, releaseName);
    }
}
