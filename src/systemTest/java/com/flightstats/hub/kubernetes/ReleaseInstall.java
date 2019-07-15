package com.flightstats.hub.kubernetes;

import com.flightstats.hub.system.config.HelmProperties;
import hapi.chart.ChartOuterClass;
import hapi.chart.ConfigOuterClass;
import hapi.release.ReleaseOuterClass;
import hapi.services.tiller.Tiller.InstallReleaseRequest;
import hapi.services.tiller.Tiller.InstallReleaseResponse;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.microbean.helm.ReleaseManager;
import org.microbean.helm.Tiller;
import org.microbean.helm.chart.URLChartLoader;

import javax.inject.Inject;
import java.net.URL;
import java.util.concurrent.Future;

import static junit.framework.TestCase.assertTrue;

@Slf4j
public class ReleaseInstall {
    private final HelmProperties helmProperties;
    private final HelmYamlOverride helmYamlOverride;

    @Inject
    public ReleaseInstall(HelmProperties helmProperties, HelmYamlOverride helmYamlOverride) {
        this.helmProperties = helmProperties;
        this.helmYamlOverride = helmYamlOverride;
    }

    private InstallReleaseRequest.Builder getRequestBuilder() {
        InstallReleaseRequest.Builder requestBuilder = InstallReleaseRequest.newBuilder();
        requestBuilder.setTimeout(1000L);
        requestBuilder.setName(getReleaseName());
        requestBuilder.setWait(true);
        requestBuilder.setDisableHooks(false);
        return requestBuilder;
    }

    private InstallReleaseRequest.Builder configureRequestBuilder() {
        InstallReleaseRequest.Builder requestBuilder = getRequestBuilder();
        ConfigOuterClass.Config.Builder valuesBuilder = requestBuilder.getValuesBuilder();
        valuesBuilder.setRaw(helmYamlOverride.getYaml());
        requestBuilder.setValues(valuesBuilder.build());
        return requestBuilder;
    }

    @SneakyThrows
    private ChartOuterClass.Chart.Builder getUrlChartBuilder() {
        try (URLChartLoader chartLoader = new URLChartLoader()) {
            log.info("Hub helm chart location {} ", getChartPath());
            return chartLoader.load(new URL(getChartPath()));
        }
    }

    @SneakyThrows
    void install() {
        install(configureRequestBuilder());
    }

    @SneakyThrows
    private void install(InstallReleaseRequest.Builder requestBuilder) {
        log.info("Hub release {} install begins", getReleaseName());

        long start = System.currentTimeMillis();
        ChartOuterClass.Chart.Builder chartBuilder = getUrlChartBuilder();
        try (DefaultKubernetesClient client = new DefaultKubernetesClient();
             Tiller tiller = new Tiller(client);
             ReleaseManager releaseManager = new ReleaseManager(tiller)) {
            Future<InstallReleaseResponse> releaseFuture = releaseManager.install(requestBuilder, chartBuilder);
            ReleaseOuterClass.Release release = releaseFuture.get().getRelease();
            assertTrue(release.hasChart());
            assertTrue(release.hasConfig());

        }

        log.info("Hub release {} install completed in {} ms", getReleaseName(), (System.currentTimeMillis() - start));
    }

    private String getReleaseName() {
        return helmProperties.getReleaseName();
    }

    private String getChartPath() {
        return helmProperties.getChartPath();
    }
}
