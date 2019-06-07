package com.flightstats.hub.system.config;

import javax.inject.Inject;
import javax.inject.Named;

public class HelmProperties {
    private final String releaseName;
    private final String chartPath;
    private final boolean shouldDeleteRelease;
    private final boolean isClusteredHubInstall;
    private final ServiceProperties serviceProperties;

    @Inject
    public HelmProperties(@Named(PropertiesName.HELM_RELEASE_NAME) String releaseName,
                          @Named(PropertiesName.HELM_CHART_PATH) String chartPath,
                          @Named(PropertiesName.HELM_CLUSTERED_HUB) boolean isClusteredHubInstall,
                          @Named(PropertiesName.HELM_RELEASE_DELETE) boolean shouldDeleteRelease,
                          ServiceProperties serviceProperties) {
        this.releaseName = releaseName;
        this.chartPath = chartPath;
        this.isClusteredHubInstall = isClusteredHubInstall;
        this.shouldDeleteRelease = shouldDeleteRelease;
        this.serviceProperties = serviceProperties;
    }

    public String getReleaseName() {
        return releaseName;
    }

    public String getChartPath() {
        return chartPath;
    }

    public boolean isReleaseDeletable() {
        return shouldDeleteRelease;
    }

    public boolean isHubInstalledByHelm() {
        return serviceProperties.getHubUrl().contains(releaseName);
    }

    public boolean isHubInstallClustered() {
        return isClusteredHubInstall;
    }

    public boolean isZookeeperInstalledByHelm() {
        return isHubInstalledByHelm() && isHubInstallClustered();
    }

    public boolean isLocalstackInstalledByHelm() {
        return isHubInstalledByHelm();
    }

    public boolean isCallbackServerInstalledByHelm() {
        return serviceProperties.getCallbackUrl().contains(releaseName);
    }
}
