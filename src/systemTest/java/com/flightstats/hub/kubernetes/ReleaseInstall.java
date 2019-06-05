package com.flightstats.hub.kubernetes;

import hapi.chart.ChartOuterClass;
import hapi.release.ReleaseOuterClass;
import hapi.services.tiller.Tiller.InstallReleaseRequest;
import hapi.services.tiller.Tiller.InstallReleaseResponse;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.microbean.helm.ReleaseManager;
import org.microbean.helm.Tiller;
import org.microbean.helm.chart.URLChartLoader;

import java.net.URL;
import java.util.concurrent.Future;

import static junit.framework.TestCase.assertTrue;

@Slf4j
public class ReleaseInstall {

    @SneakyThrows
    public void install(String releaseName, String chartPath) {

        log.info("Hub release {} install begins", releaseName);

        long start = System.currentTimeMillis();
        ChartOuterClass.Chart.Builder chartBuilder;
        try (final URLChartLoader chartLoader = new URLChartLoader()) {
            log.info("Hub helm chart location {} ", chartPath);
            chartBuilder = chartLoader.load(new URL(chartPath));
        }

        try (final DefaultKubernetesClient client = new DefaultKubernetesClient();
             final Tiller tiller = new Tiller(client);
             final ReleaseManager releaseManager = new ReleaseManager(tiller)) {

            final InstallReleaseRequest.Builder requestBuilder = InstallReleaseRequest.newBuilder();
            requestBuilder.setTimeout(300L);
            requestBuilder.setName(releaseName);
            requestBuilder.setWait(true);
            requestBuilder.setDisableHooks(false);

            final Future<InstallReleaseResponse> releaseFuture = releaseManager.install(requestBuilder, chartBuilder);
            ReleaseOuterClass.Release release = releaseFuture.get().getRelease();
            assertTrue(release.hasChart());
            assertTrue(release.hasConfig());

        }

        log.info("Hub release {} install completed in {} ms", releaseName, (System.currentTimeMillis() - start));
    }
}

