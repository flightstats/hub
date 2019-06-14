package com.flightstats.hub.kubernetes;

import com.flightstats.hub.system.config.HelmProperties;
import com.flightstats.hub.system.config.PropertiesName;
import com.google.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Singleton
public class HubLifecycle {
    private final AtomicBoolean isSetupFinished = new AtomicBoolean(false);
    @Inject
    @Named(PropertiesName.HELM_RELEASE_NAME)
    private String releaseName;

    @Inject
    private HelmProperties helmProperties;

    @Inject
    private ReleaseInstall releaseInstall;

    @Inject
    private ReleaseDelete releaseDelete;

    @Inject
    private ReleaseStatus releaseStatus;

    @Inject
    private ServiceDelete serviceDelete;

    @SneakyThrows
    public void setup() {
        Thread.sleep(20000);
        log.info("would've installed " + releaseName);
//        /*
//        if (releaseStatus.releaseExists(getReleaseName())) {
//            log.info("Release is already installed; skipping");
//        } else {
//            releaseInstall.install();
//        }
//        */
    }

    public void serviceDelete(List<String> serviceName) {
        serviceDelete.execute(getReleaseName(), serviceName);
    }

    public void cleanup() {
        if (helmProperties.isReleaseDeletable()) {
            releaseDelete.delete(getReleaseName());
        } else {
            log.info("skipping release deletion");
        }
    }

    private String getReleaseName() {
        return helmProperties.getReleaseName();
    }

}
