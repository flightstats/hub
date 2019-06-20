package com.flightstats.hub.kubernetes;

import com.flightstats.hub.system.config.HelmProperties;
import com.google.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.List;

@Slf4j
@Singleton
public class HubLifecycle {
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
        if (releaseStatus.releaseExists(getReleaseName())) {
            log.info("Release is already installed; skipping");
        } else {
            releaseInstall.install();
        }
    }

    public void setup(String customYaml) {
        if (releaseStatus.releaseExists(getReleaseName())) {
            log.info("Release is already installed; skipping");
        } else {
            releaseInstall.install(customYaml);
        }
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
