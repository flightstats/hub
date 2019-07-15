package com.flightstats.hub.system.config;

import lombok.Value;

import javax.inject.Inject;
import javax.inject.Named;

@Value
public class HelmProperties {
    private final String releaseName;
    private final String chartPath;
    private final String hubDockerImage;
    private final boolean shouldDeleteRelease;
    private final boolean isClusteredHubInstall;
    private final boolean hubInstalledByHelm;
    private final boolean localstackInstalledByHelm;
    private final boolean callbackServerInstalledByHelm;
    private final boolean releaseDeletable;
    private final ServiceProperties serviceProperties;

    @Inject
    public HelmProperties(@Named(PropertiesName.HELM_RELEASE_NAME) String releaseName,
                          @Named(PropertiesName.HELM_CHART_PATH) String chartPath,
                          @Named(PropertiesName.HELM_CLUSTERED_HUB) boolean isClusteredHubInstall,
                          @Named(PropertiesName.HELM_RELEASE_DELETE) boolean shouldDeleteRelease,
                          @Named(PropertiesName.HUB_DOCKER_IMAGE) String hubDockerImage,
                          ServiceProperties serviceProperties) {
        this.releaseName = releaseName;
        this.chartPath = chartPath;
        this.isClusteredHubInstall = isClusteredHubInstall;
        this.shouldDeleteRelease = shouldDeleteRelease;
        this.hubDockerImage = hubDockerImage;
        this.hubInstalledByHelm = serviceProperties.getHubUrl().contains(releaseName);
        this.localstackInstalledByHelm = serviceProperties.getHubUrl().contains(releaseName);
        this.callbackServerInstalledByHelm = serviceProperties.getCallbackUrl().contains(releaseName);
        this.releaseDeletable = shouldDeleteRelease;
        this.serviceProperties = serviceProperties;
    }

    public boolean isHubInstallClustered() { return isClusteredHubInstall && isHubInstalledByHelm(); }

    public boolean isZookeeperInstalledByHelm() { return isHubInstallClustered(); }

}
