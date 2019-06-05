package com.flightstats.hub.kubernetes;

import hapi.services.tiller.Tiller.UninstallReleaseRequest;
import hapi.services.tiller.Tiller.UninstallReleaseResponse;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.microbean.helm.ReleaseManager;
import org.microbean.helm.Tiller;

import java.util.concurrent.Future;

@Slf4j
public class ReleaseDelete {

    @SneakyThrows
    public void delete(String releaseName) {

        log.info("Hub release {} delete begins", releaseName);

        long start = System.currentTimeMillis();

        try (DefaultKubernetesClient client = new DefaultKubernetesClient();
             Tiller tiller = new Tiller(client);
             ReleaseManager releaseManager = new ReleaseManager(tiller)) {

            UninstallReleaseRequest.Builder uninstallRequestBuilder = UninstallReleaseRequest.newBuilder();
            uninstallRequestBuilder.setName(releaseName);
            uninstallRequestBuilder.setPurge(true);
            uninstallRequestBuilder.setDisableHooks(false);
            uninstallRequestBuilder.setTimeout(300L);

            Future<UninstallReleaseResponse> releaseFuture = releaseManager.uninstall(uninstallRequestBuilder.build());
            releaseFuture.get();
            log.info("Hub release {} delete completed in {} ms", releaseName, System.currentTimeMillis() - start);
        }
    }
}
