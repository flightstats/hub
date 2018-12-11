package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.cluster.CuratorCluster;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.HubUtils;
import com.google.common.annotations.VisibleForTesting;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

public class InternalWebhookClient {
    private final static Logger logger = LoggerFactory.getLogger(InternalWebhookClient.class);

    private final Client client;
    private final CuratorCluster hubCluster;

    @Inject
    public InternalWebhookClient(@Named("HubCuratorCluster") CuratorCluster hubCluster) {
        this.hubCluster = hubCluster;
        this.client = RestClient.createClient(5, 15, true, true);
    }

    @VisibleForTesting
    InternalWebhookClient(Client client, CuratorCluster hubCluster) {
        this.hubCluster = hubCluster;
        this.client = client;
    }

    void remove(String name, Collection<String> servers) {
        for (String server : servers) {
            remove(name, server);
        }
    }

    void remove(String name, String server) {
        put(server + "/internal/webhook/delete/" + name);
    }

    void runOnServerWithFewestWebhooks(String name) {
        runOnOneServer(name, getOrderedServers());
    }

    void runOnOneServer(String name, Collection<String> servers) {
        for (String server : servers) {
            if (put(server + "/internal/webhook/run/" + name)) break;
        }
    }

    /**
     * We want this to return this list in order from fewest to most
     */
    @VisibleForTesting
    Collection<String> getOrderedServers() {
        TreeMap<Integer, String> orderedServers = new TreeMap<>();
        List<String> servers = hubCluster.getRandomServers();
        for (String server : servers) {
            int count = get(server + "/internal/webhook/count");
            orderedServers.put(count, server);
        }
        if (orderedServers.isEmpty()) {
            return servers;
        }
        return orderedServers.values();
    }

    private boolean put(String url) {
        String hubUrl = HubHost.getScheme() + url;
        ClientResponse response = null;
        try {
            logger.info("calling {}", hubUrl);
            response = client.resource(hubUrl).put(ClientResponse.class);
            if (response.getStatus() == 200) {
                logger.debug("success {}", response);
                return true;
            } else {
                logger.warn("unexpected response {}", response);
            }
        } catch (Exception e) {
            logger.warn("unable to put " + hubUrl, e);
        } finally {
            HubUtils.close(response);
        }
        return false;
    }

    private int get(String url) {
        ClientResponse response = null;
        String hubUrl = HubHost.getScheme() + url;
        try {
            logger.info("calling {}", hubUrl);
            response = client.resource(hubUrl).get(ClientResponse.class);
            if (response.getStatus() == 200) {
                logger.debug("success {}", response);
                return Integer.parseInt(response.getEntity(String.class));
            } else {
                logger.warn("unexpected response {}", response);
            }
        } catch (Exception e) {
            logger.warn("unable to get " + hubUrl, e);
        } finally {
            HubUtils.close(response);
        }
        return 0;
    }
}
