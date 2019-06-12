package com.flightstats.hub.kubernetes;

import io.fabric8.kubernetes.api.model.DoneableService;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ServiceDelete {
    @SneakyThrows
    public void execute(String releaseName, List<String> serviceNames) {

        try (DefaultKubernetesClient client = new DefaultKubernetesClient()) {

            MixedOperation<Service, ServiceList, DoneableService, Resource<Service, DoneableService>> serviceResource = client.inNamespace(releaseName).services();
            int serviceCount = serviceResource.list().getItems().size();

            List<Service> services = serviceResource.list().getItems();
            for (int i = 0; i < serviceCount; i++) {
                if (serviceNames.contains(services.get(i).getMetadata().getName())) {
                    log.info("Deleting service {} in namespace {} ", services.get(i).getMetadata().getName(), services.get(i).getMetadata().getNamespace());
                    boolean result = serviceResource.delete(services.get(i));
                    log.info("Service {} in namespace {} deletion status {} ", services.get(i).getMetadata().getName(), services.get(i).getMetadata().getNamespace(), result);
                }
            }
        }
    }

}
