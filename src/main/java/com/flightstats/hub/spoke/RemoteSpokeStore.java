package com.flightstats.hub.spoke;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.cluster.CuratorCluster;
import com.flightstats.hub.dao.ContentKeyUtil;
import com.flightstats.hub.dao.ContentMarshaller;
import com.flightstats.hub.dao.QueryResult;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.ConnectException;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RemoteSpokeStore {

    private final static Logger logger = LoggerFactory.getLogger(RemoteSpokeStore.class);

    private final static Client write_client = RestClient.createClient(1, 5, true, false);
    private final static Client query_client = RestClient.createClient(5, 15, true, true);

    private final CuratorCluster cluster;
    private final MetricsService metricsService;
    private final ExecutorService executorService;
    private final int stableSeconds;
    private final HubProperties hubProperties;

    @Inject
    public RemoteSpokeStore(@Named("SpokeCluster") CuratorCluster cluster,
                            MetricsService metricsService,
                            HubProperties hubProperties)
    {
        this.cluster = cluster;
        this.metricsService = metricsService;
        this.stableSeconds = hubProperties.getProperty("app.stable_seconds", 5);
        this.hubProperties = hubProperties;
        executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("RemoteSpokeStore-%d").build());
    }

    void testOne(Collection<String> server) throws InterruptedException {
        String path = "Internal-Spoke-Health-Hook/";
        Traces traces = new Traces(hubProperties, path);
        int calls = 10;
        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        CountDownLatch quorumLatch = new CountDownLatch(calls);
        for (int i = 0; i < calls; i++) {
            threadPool.submit(() -> {
                try {
                    ContentKey key = new ContentKey();
                    if (insert(SpokeStore.WRITE, path + key.toUrl(), key.toUrl().getBytes(), server, traces, "payload", path)) {
                        quorumLatch.countDown();
                    } else {
                        traces.log(logger);
                    }
                } catch (Exception e) {
                    logger.warn("unexpected exception " + server, e);
                }
            });
        }
        if (quorumLatch.await(5, TimeUnit.SECONDS)) {
            threadPool.shutdown();
            logger.info("completed warmup calls to Spoke {}", server);
        } else {
            threadPool.shutdown();
            throw new RuntimeException("unable to properly connect to Spoke " + server);
        }
    }

    boolean testAll() {
        Collection<String> servers = cluster.getRandomServers();
        servers.addAll(cluster.getLocalServer());
        logger.info("*********************************************");
        logger.info("testing servers {}", servers);
        logger.info("*********************************************");
        String path = HubHost.getLocalAddressPort();
        for (String server : servers) {
            try {
                logger.info("calling server {} path {}", server, path);
                String url = HubHost.getScheme() + server + "/internal/spoke/test/" + path;
                ClientResponse response = query_client.resource(url).get(ClientResponse.class);
                if (response.getStatus() == 200) {
                    logger.info("success calling {}", response);
                } else if (response.getStatus() == 404) {
                    logger.warn("test not yet implemented {}", response);
                } else {
                    logger.warn("failed response {}", response);
                    return false;
                }
            } catch (Exception e) {
                logger.warn("unable to test " + path + " with " + server, e);
                return false;
            }
        }
        logger.info("all startup tests succeeded  " + path);
        return true;
    }

    public boolean insert(SpokeStore spokeStore, String path, byte[] payload, String spokeApi, String channel) {
        return insert(spokeStore, path, payload, cluster.getWriteServers(), ActiveTraces.getLocal(), spokeApi, channel);
    }

    public boolean insert(SpokeStore spokeStore, String path, byte[] payload, Collection<String> servers, Traces traces, String spokeApi, String channel) {
        int quorum = getQuorum(servers.size());
        CountDownLatch quorumLatch = new CountDownLatch(quorum);
        AtomicBoolean firstComplete = new AtomicBoolean();
        for (final String server : servers) {
            executorService.submit(() -> {
                setThread(path);
                String uri = HubHost.getScheme() + server + "/internal/spoke/" + spokeStore + "/" + spokeApi + "/" + path;
                traces.add(uri);
                ClientResponse response = null;
                try {
                    response = write_client.resource(uri).put(ClientResponse.class, payload);
                    traces.add(server, response.getEntity(String.class));
                    if (response.getStatus() == 201) {
                        if (firstComplete.compareAndSet(false, true)) {
                            metricsService.time(channel, "heisenberg", traces.getStart());
                        }
                        quorumLatch.countDown();
                        logger.trace("server {} path {} response {}", server, path, response);
                    } else {
                        logger.info("write failed: server {} path {} response {}", server, path, response);
                    }
                } catch (Exception e) {
                    traces.add(server, e.getMessage());
                    logger.warn("write failed: " + server + " " + path, e);
                } finally {
                    HubUtils.close(response);
                    resetThread();
                }

            });
        }
        try {
            quorumLatch.await(stableSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
        metricsService.time(channel, "consistent", traces.getStart());
        return quorumLatch.getCount() != quorum;
    }

    private void setThread(String name) {
        Thread thread = Thread.currentThread();
        thread.setName(thread.getName() + "|" + name);
    }

    private void resetThread() {
        Thread thread = Thread.currentThread();
        thread.setName(StringUtils.substringBefore(thread.getName(), "|"));
    }

    static int getQuorum(int size) {
        return (int) Math.max(1, Math.ceil(size / 2.0));
    }

    public Content get(SpokeStore spokeStore, String path, ContentKey key) {
        Collection<String> servers = cluster.getRandomServers();
        for (String server : servers) {
            ClientResponse response = null;
            try {
                setThread(path);
                String url = HubHost.getScheme() + server + "/internal/spoke/" + spokeStore + "/payload/" + path;
                response = query_client.resource(url).get(ClientResponse.class);
                logger.trace("server {} path {} response {}", server, path, response);
                if (response.getStatus() == 200) {
                    byte[] entity = response.getEntity(byte[].class);
                    if (entity.length > 0) {
                        return ContentMarshaller.toContent(entity, key);
                    }
                }
            } catch (JsonMappingException e) {
                logger.info("JsonMappingException for " + path);
            } catch (ClientHandlerException e) {
                if (e.getCause() != null && e.getCause() instanceof ConnectException) {
                    logger.warn("connection exception " + server);
                } else {
                    logger.warn("unable to get content " + server + " " + path, e);
                }
            } catch (Exception e) {
                logger.warn("unable to get content " + path, e);
            } finally {
                HubUtils.close(response);
                resetThread();
            }
        }
        return null;
    }

    QueryResult readTimeBucket(SpokeStore spokeStore, String channel, String timePath) throws InterruptedException {
        return getKeys("/internal/spoke/" + spokeStore + "/time/" + channel + "/" + timePath);
    }

    SortedSet<ContentKey> getNext(String channel, int count, String startKey) throws InterruptedException {
        return getKeys("/internal/spoke/next/" + channel + "/" + count + "/" + startKey).getContentKeys();
    }

    private QueryResult getKeys(final String path) throws InterruptedException {
        Traces traces = ActiveTraces.getLocal();
        Collection<String> servers = cluster.getAllServers();
        CountDownLatch countDownLatch = new CountDownLatch(servers.size());
        QueryResult queryResult = new QueryResult(servers.size());
        for (final String server : servers) {
            executorService.submit(() -> {
                ClientResponse response = null;
                try {
                    setThread(path);
                    traces.add("spoke calling", server, path);
                    response = query_client.resource(HubHost.getScheme() + server + path).get(ClientResponse.class);
                    traces.add("spoke server response", server, response);
                    if (response.getStatus() == 200) {
                        SortedSet<ContentKey> keySet = new TreeSet<>();
                        String keysString = response.getEntity(String.class);
                        ContentKeyUtil.convertKeyStrings(keysString, keySet);
                        traces.add(server, keySet);
                        queryResult.addKeys(keySet);
                    }
                } catch (ClientHandlerException e) {
                    if (e.getCause() != null && e.getCause() instanceof ConnectException) {
                        logger.warn("connection exception " + server);
                    } else {
                        logger.warn("unable to get content " + path, e);
                    }
                    traces.add("ClientHandlerException", e.getMessage(), server);
                } catch (Exception e) {
                    logger.warn("unable to handle " + server + " " + path, e);
                    traces.add("unable to handle ", server, path, e);
                } finally {
                    HubUtils.close(response);
                    resetThread();
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await(20, TimeUnit.SECONDS);
        return queryResult;
    }

    public Optional<ContentKey> getLatest(String channel, String path, Traces traces) throws InterruptedException {
        Collection<String> servers = cluster.getAllServers();
        CountDownLatch countDownLatch = new CountDownLatch(servers.size());
        SortedSet<ContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        for (final String server : servers) {
            executorService.submit(() -> {
                ClientResponse response = null;
                try {
                    setThread(path);
                    traces.add("spoke calling", server, channel);
                    response = query_client.resource(HubHost.getScheme() + server + "/internal/spoke/latest/" + path)
                            .get(ClientResponse.class);
                    traces.add("spoke server response", server, response);
                    if (response.getStatus() == 200) {
                        String key = response.getEntity(String.class);
                        if (StringUtils.isNotEmpty(key)) {
                            orderedKeys.add(ContentKeyUtil.convertKey(key).get());
                        }
                        traces.add(server, key);
                    }
                } catch (ClientHandlerException e) {
                    if (e.getCause() != null && e.getCause() instanceof ConnectException) {
                        logger.warn("connection exception " + server);
                    } else {
                        logger.warn("unable to get content " + path, e);
                    }
                    traces.add("ClientHandlerException", e.getMessage(), server);
                } catch (Exception e) {
                    logger.warn("unable to handle " + server + " " + channel, e);
                    traces.add("unable to handle ", server, channel, e);
                } finally {
                    HubUtils.close(response);
                    resetThread();
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await(5, TimeUnit.SECONDS);
        if (orderedKeys.isEmpty()) {
            return Optional.absent();
        }
        return Optional.of(orderedKeys.last());
    }

    public boolean delete(SpokeStore spokeStore, String path) throws Exception {
        Collection<String> servers = cluster.getAllServers();
        int quorum = servers.size();
        CountDownLatch countDownLatch = new CountDownLatch(quorum);
        for (final String server : servers) {
            executorService.submit(() -> {
                ClientResponse response = null;
                try {
                    setThread(path);
                    response = query_client.resource(HubHost.getScheme() + server + "/internal/spoke/" + spokeStore + "/payload/" + path)
                            .delete(ClientResponse.class);

                    if (response.getStatus() < 400) {
                        countDownLatch.countDown();
                    }
                    logger.trace("server {} path {} response {}", server, path, response);
                } catch (Exception e) {
                    logger.warn("unable to delete " + path, e);
                } finally {
                    HubUtils.close(response);
                    resetThread();
                }
            });
        }

        return countDownLatch.await(60, TimeUnit.SECONDS);
    }

}
