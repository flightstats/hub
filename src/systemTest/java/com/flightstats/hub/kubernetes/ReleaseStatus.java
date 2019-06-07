package com.flightstats.hub.kubernetes;

import hapi.services.tiller.Tiller.ListReleasesRequest;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.SneakyThrows;
import org.microbean.helm.ReleaseManager;
import org.microbean.helm.Tiller;

public class ReleaseStatus {
    @SneakyThrows
    public boolean releaseExists(String releaseName) {
        try (DefaultKubernetesClient client = new DefaultKubernetesClient();
             Tiller tiller = new Tiller(client);
             ReleaseManager releaseManager = new ReleaseManager(tiller)) {

            ListReleasesRequest request = ListReleasesRequest.newBuilder()
                    .setFilter(releaseName)
                    .setLimit(1)
                    .build();

            return releaseManager.list(request).hasNext();
        }
    }
}
