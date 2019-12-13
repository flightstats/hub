package com.flightstats.hub.kubernetes;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Wither;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

@Slf4j
@Wither
@RequiredArgsConstructor
public class ExecPodCommand {
    private final String pod;
    private final String namespace;
    private final String[] command;

    public void run() throws InterruptedException {

        try (final DefaultKubernetesClient client = new DefaultKubernetesClient();
             ExecWatch watch = client.pods().inNamespace(namespace).withName(pod)
                     .readingInput(System.in)
                     .writingOutput(System.out)
                     .writingError(System.err)
                     .withTTY()
                     .usingListener(new SimpleListener())
                     .exec(command)){
            Thread.sleep(10 * 1000);
        }
    }

    private class SimpleListener implements ExecListener {

        @Override
        public void onOpen(Response response) {
            log.info("Running command {} in namespace: {} for pod: {}", command, namespace, pod);
        }

        @Override
        @SneakyThrows
        public void onFailure(Throwable t, Response response) {
            log.error(response.message());
            throw t;
        }

        @Override
        public void onClose(int code, String reason) {
            log.info("command complete exiting with code: {} reason: {}", code, reason);
        }
    }

}
