package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.CuratorCluster;
import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.util.HubUtils;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.flightstats.hub.constant.NamedBinding.WEBHOOK_CLIENT;
import static java.util.stream.Collectors.toList;

@Slf4j
public class InternalWebhookClient {
    private final Client client;
    private final CuratorCluster hubCluster;
    private final String uriScheme;

    @Inject
    public InternalWebhookClient(@Named("HubCuratorCluster") CuratorCluster hubCluster,
                                 @Named(WEBHOOK_CLIENT) Client client,
                                 LocalHostProperties localHostProperties) {
        this.hubCluster = hubCluster;
        this.client = client;
        this.uriScheme = localHostProperties.getUriScheme();
    }

    List<String> stop(String name, Collection<String> servers) {
        return servers.stream()
                .filter(server -> stop(name, server))
                .collect(toList());
    }

    boolean stop(String name, String server) {
        return put(server + "/internal/webhook/stop/" + name);
    }

    Optional<String> runOnOnlyOneServer(String name, Collection<String> runningServers) {
        Optional<String> successfulServerRun = Optional.ofNullable(
                runOnOneServer(name, runningServers).orElseGet(
                        () -> runOnServerWithFewestWebhooksExcluding(name, runningServers).orElse(null)));

        runningServers.stream()
                .filter(server -> successfulServerRun
                        .map(successfulRun -> !server.equals(successfulRun))
                        .orElse(true))
                .forEach(server -> stop(name, server));

        return successfulServerRun;
    }

    Optional<String> runOnOneServer(String name, Collection<String> servers) {
        return runOnOneServer(name, servers.stream());
    }

    Optional<String> runOnServerWithFewestWebhooks(String name) {
        return runOnServerWithFewestWebhooksExcluding(name, Collections.emptyList());
    }

    private Optional<String> runOnOneServer(String name, Stream<String> servers) {
        return servers
                .filter(server -> put(server + "/internal/webhook/run/" + name))
                .findFirst();
    }

    private Optional<String> runOnServerWithFewestWebhooksExcluding(String name, Collection<String> servers) {
        Stream<String> availableServers = getOrderedServers().stream()
                .filter(server -> !servers.contains(server));
        return runOnOneServer(name, availableServers);
    }

    /**
     * We want this to return this list in order from fewest to most
     * If we get a non-200, return max int so it's tried last
     */
    private Collection<String> getOrderedServers() {
        return hubCluster.getRandomServers().stream()
                .sorted(Comparator.comparingInt(this::getCount))
                .collect(toList());
    }

    private Integer getCount(String server) {
        return get(server + "/internal/webhook/count")
                .orElse(Integer.MAX_VALUE);
    }

    private boolean put(String url) {
        String hubUrl = uriScheme + url;
        ClientResponse response = null;
        try {
            log.debug("calling {}", hubUrl);
            response = client.resource(hubUrl).put(ClientResponse.class);
            if (response.getStatus() == 200) {
                log.trace("success putting {}: {}", hubUrl, response);
                return true;
            } else {
                log.error("unexpected response putting {}: {}", hubUrl, response);
            }
        } catch (Exception e) {
            log.error("unable to put {}", hubUrl, e);
        } finally {
            HubUtils.close(response);
        }
        return false;
    }

    private Optional<Integer> get(String url) {
        ClientResponse response = null;
        String hubUrl = uriScheme + url;
        try {
            log.debug("calling {}", hubUrl);
            response = client.resource(hubUrl).get(ClientResponse.class);
            if (response.getStatus() == 200) {
                log.trace("success {}", response);
                Integer count = Integer.parseInt(response.getEntity(String.class));
                return Optional.of(count);
            } else {
                log.error("unexpected response {}", response);
            }
        } catch (Exception e) {
            log.error("unable to get " + hubUrl, e);
        } finally {
            HubUtils.close(response);
        }
        return Optional.empty();
    }
}
