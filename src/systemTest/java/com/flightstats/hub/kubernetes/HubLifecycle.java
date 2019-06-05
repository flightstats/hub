package com.flightstats.hub.kubernetes;

import com.flightstats.hub.system.config.DependencyInjector;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@Slf4j
public class HubLifecycle extends DependencyInjector {

    @Inject
    @Named("helm.release.name")
    private String releaseName;

    @Inject
    @Named("helm.chart.path")
    private String chartPath;

    @Inject
    private ReleaseInstall releaseInstall;

    @Inject
    private ReleaseDelete releaseDelete;

    @Inject
    private ServiceDelete serviceDelete;

    public void setup() {
        this.releaseInstall.install(this.releaseName, this.chartPath);
    }

    public void serviceDelete(List<String> serviceName) {
        serviceDelete.execute(releaseName, serviceName);
    }

    public void cleanup() {
        this.releaseDelete.delete(this.releaseName);
    }

}
