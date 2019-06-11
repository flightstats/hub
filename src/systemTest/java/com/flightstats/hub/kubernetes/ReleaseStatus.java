package com.flightstats.hub.kubernetes;

import hapi.services.tiller.Tiller.GetReleaseStatusRequest;
import hapi.services.tiller.Tiller.GetReleaseStatusResponse;
import hapi.services.tiller.Tiller.ListReleasesRequest;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.microbean.helm.ReleaseManager;
import org.microbean.helm.Tiller;

public class ReleaseStatus {
    @SneakyThrows
    public boolean releaseExists(String releaseName) {
        if (StringUtils.isBlank(releaseName)) {
            return false;
        }
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
