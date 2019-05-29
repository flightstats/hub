package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.CuratorCluster;
import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.util.HubUtils;
import com.google.common.annotations.VisibleForTesting;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Slf4j
public class InternalWebhookClient {
    private final Client client;
    private final CuratorCluster hubCluster;
    private final String uriScheme;

    @Inject
    public InternalWebhookClient(@Named("HubCuratorCluster") CuratorCluster hubCluster,
                                 @Named("WebhookClient") Client client,
                                 LocalHostProperties localHostProperties) {
        this.hubCluster = hubCluster;
        this.client = client;
        this.uriScheme = localHostProperties.getUriScheme();
    }

    List<String> remove(String name, Collection<String> servers) {
        return servers.stream()
                .filter(server -> remove(name, server))
                .collect(toList());
    }

    boolean remove(String name, String server) {
        return put(server + "/internal/webhook/delete/" + name);
    }

    Optional<String> runOnServerWithFewestWebhooks(String name) {
        return runOnOneServer(name, getOrderedServers());
    }

    Optional<String> runOnOneServer(String name, Collection<String> servers) {
        return servers.stream()
                .filter(server -> put(server + "/internal/webhook/run/" + name))
                .findFirst();
    }

    /**
     * We want this to return this list in order from fewest to most
     */
    @VisibleForTesting
    Collection<String> getOrderedServers() {
        return hubCluster.getRandomServers().stream()
                .sorted(Comparator.comparingInt(this::getCount))
                .collect(toList());
    }

    private Integer getCount(String server) {
        return get(server + "/internal/webhook/count");
    }

    private boolean put(String url) {
        String hubUrl = uriScheme + url;
        ClientResponse response = null;
        try {
            log.info("calling {}", hubUrl);
            response = client.resource(hubUrl).put(ClientResponse.class);
            if (response.getStatus() == 200) {
                log.debug("success {}", response);
                return true;
            } else {
                log.warn("unexpected response {}", response);
            }
        } catch (Exception e) {
            log.warn("unable to put " + hubUrl, e);
        } finally {
            HubUtils.close(response);
        }
        return false;
    }

    private int get(String url) {
        ClientResponse response = null;
        String hubUrl = uriScheme + url;
        try {
            log.info("calling {}", hubUrl);
            response = client.resource(hubUrl).get(ClientResponse.class);
            if (response.getStatus() == 200) {
                log.debug("success {}", response);
                return Integer.parseInt(response.getEntity(String.class));
            } else {
                log.warn("unexpected response {}", response);
            }
        } catch (Exception e) {
            log.warn("unable to get " + hubUrl, e);
        } finally {
            HubUtils.close(response);
        }
        return 0;
    }
}
