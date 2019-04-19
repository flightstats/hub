package com.flightstats.hub.kubernetes;

import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class PodRestart {

    @SneakyThrows
    public void execute(String releaseName, List<String> podNames) {

        try (final DefaultKubernetesClient client = new DefaultKubernetesClient()) {

            final MixedOperation<Pod, PodList, DoneablePod, PodResource<Pod, DoneablePod>> podResource = client.inNamespace(releaseName).pods();
            int podCount = podResource.list().getItems().size();

            final List<Pod> pods = podResource.list().getItems();
            for (int i = 0; i < podCount; i++) {
                if (podNames.contains(pods.get(i).getMetadata().getName())) {
                    log.info("Deleting pod {} in namespace {} ", pods.get(i).getMetadata().getName(), pods.get(i).getMetadata().getNamespace());
                    boolean result = podResource.delete(pods.get(i));
                    log.info("Pod {} in namespace {} deletion status {} ", pods.get(i).getMetadata().getName(), pods.get(i).getMetadata().getNamespace(), result);
                }
            }
        }
    }

}
