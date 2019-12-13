package com.flightstats.hub.kubernetes;

import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StatefulsetScaler {

    @SneakyThrows
    public void resize(String releaseName, String statefulsetName, int newCount, boolean wait) {

        try (DefaultKubernetesClient client = new DefaultKubernetesClient()) {
            StatefulSet statefulSet = client.inNamespace(releaseName)
                    .apps()
                    .statefulSets()
                    .withName(statefulsetName)
                    .scale(newCount, wait);
            log.info("statefulset {} now has {} replicas according to new request of {})",
                    statefulsetName,
                    statefulSet.getStatus().getReplicas(),
                    newCount);
        }
    }

}
