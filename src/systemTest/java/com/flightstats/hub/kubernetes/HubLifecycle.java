package com.flightstats.hub.kubernetes;

import com.flightstats.hub.system.config.DependencyInjector;
import com.flightstats.hub.system.config.PropertiesName;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@Slf4j
@Singleton
public class HubLifecycle extends DependencyInjector {

    @Inject
    @Named(PropertiesName.HELM_RELEASE_NAME)
    private String releaseName;

    @Inject
    @Named(PropertiesName.HELM_CHART_PATH)
    private String chartPath;

    @Inject
    @Named(PropertiesName.HELM_RELEASE_DELETE)
    private boolean isHelmReleaseDeletable;

    @Inject
    private ReleaseInstall releaseInstall;

    @Inject
    private ReleaseDelete releaseDelete;

    @Inject
    private ReleaseStatus releaseStatus;

    @Inject
    private ServiceDelete serviceDelete;

    public void setup() {
        log.info("would've installed " + releaseName);
        /*
        if (releaseStatus.releaseExists(releaseName)) {
            log.info("Release is already installed; skipping");
        } else {
            releaseInstall.install(releaseName, chartPath);
        }
        */
    }

    public void serviceDelete(List<String> serviceName) {
        serviceDelete.execute(releaseName, serviceName);
    }

    public void cleanup() {
        if (isHelmReleaseDeletable) {
            releaseDelete.delete(releaseName);
        } else {
            log.info("skipping release deletion");
        }
    }

}
