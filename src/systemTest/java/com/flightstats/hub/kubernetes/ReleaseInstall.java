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

    @Inject
    public ReleaseInstall(HelmProperties helmProperties) {
        this.helmProperties = helmProperties;
    }


    @SneakyThrows
    void install() {
        log.info("Hub release {} install begins", getReleaseName());

        long start = System.currentTimeMillis();
        ChartOuterClass.Chart.Builder chartBuilder;
        try (URLChartLoader chartLoader = new URLChartLoader()) {
            log.info("Hub helm chart location {} ", getChartPath());
            chartBuilder = chartLoader.load(new URL(getChartPath()));
        }

        try (DefaultKubernetesClient client = new DefaultKubernetesClient();
             Tiller tiller = new Tiller(client);
             ReleaseManager releaseManager = new ReleaseManager(tiller)) {

            InstallReleaseRequest.Builder requestBuilder = InstallReleaseRequest.newBuilder();
            requestBuilder.setTimeout(300L);
            requestBuilder.setName(getReleaseName());
            requestBuilder.setWait(true);
            requestBuilder.setDisableHooks(false);


            ConfigOuterClass.Config.Builder valuesBuilder = requestBuilder.getValuesBuilder();
            valuesBuilder.setRaw(getOverrideValuesYaml());
            requestBuilder.setValues(valuesBuilder.build());

            Future<InstallReleaseResponse> releaseFuture = releaseManager.install(requestBuilder, chartBuilder);
            ReleaseOuterClass.Release release = releaseFuture.get().getRelease();
            assertTrue(release.hasChart());
            assertTrue(release.hasConfig());

        }

        log.info("Hub release {} install completed in {} ms", getReleaseName(), (System.currentTimeMillis() - start));
    }

    private String getOverrideValuesYaml() {
        return "tags: \n" +
                "  installHub: " + helmProperties.isHubInstalledByHelm() + "\n" +
                "  installZookeeper: " + helmProperties.isZookeeperInstalledByHelm() + "\n" +
                "  installLocalstack: " + helmProperties.isLocalstackInstalledByHelm() + "\n" +
                "  installCallbackserver: " + helmProperties.isCallbackServerInstalledByHelm() + "\n" +
                "hub: \n" +
                "  hub: \n" +
                "    image: flightstats/hub:max-items-system-tests7 \n" +
                "    clusteredHub: \n" +
                "      enabled: " + helmProperties.isHubInstallClustered() + "\n";
    }

    private String getReleaseName() {
        return helmProperties.getReleaseName();
    }

    private String getChartPath() {
        return helmProperties.getChartPath();
    }
}
