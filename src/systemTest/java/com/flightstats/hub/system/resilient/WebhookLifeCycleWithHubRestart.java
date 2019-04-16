package com.flightstats.hub.system.resilient;

import com.flightstats.hub.system.BaseTest;
import com.flightstats.hub.system.functional.WebhookLifecycleTest;
import com.flightstats.hub.helm.PodRestart;
import com.flightstats.hub.helm.ReleaseDelete;
import com.flightstats.hub.helm.ReleaseInstall;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.JUnitCore;

import javax.inject.Inject;
import javax.inject.Named;

@Slf4j
public class WebhookLifeCycleWithHubRestart extends BaseTest {

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

    @Test
    public void run() {
        this.releaseInstall.install(this.releaseName, this.chartPath);
        new JUnitCore().run(WebhookLifecycleTest.class);
        new PodRestart().execute(releaseName, "hub-0");
        new JUnitCore().run(WebhookLifecycleTest.class);
        this.releaseDelete.delete(this.releaseName);
    }

}
