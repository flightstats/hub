package com.flightstats.hub.resilient;

import com.flightstats.hub.resilient.helm.ReleaseDelete;
import com.flightstats.hub.resilient.helm.ReleaseInstall;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Named;

@Slf4j
public class HelmReleaseTest extends ResilientBaseTest {

    private String releaseName = "ddt-" + generateRandomString();
    @Inject
    private ReleaseInstall releaseInstall;
    @Inject
    private ReleaseDelete releaseDelete;

    @Inject
    @Named("helm.chart.path")
    private String chartPath;

    @Test
    public void test() {
        this.releaseInstall.install(this.releaseName, this.chartPath);
    }

    @After
    public void cleanup() {
        this.releaseDelete.delete(this.releaseName);
    }
}
