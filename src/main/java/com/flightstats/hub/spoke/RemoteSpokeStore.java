package com.flightstats.hub.spoke;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.cluster.Cluster;
import com.flightstats.hub.cluster.CuratorCluster;
import com.flightstats.hub.config.ContentProperties;
import com.flightstats.hub.dao.ContentKeyUtil;
import com.flightstats.hub.dao.ContentMarshaller;
import com.flightstats.hub.dao.QueryResult;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"Convert2streamapi", "Convert2Lambda"})
@Slf4j
public class RemoteSpokeStore {

    private final static Client write_client = RestClient.createClient(1, 5, true, false);
    private final static Client query_client = RestClient.createClient(5, 15, true, true);

    private final CuratorCluster cluster;
    private final StatsdReporter statsdReporter;
    private final ExecutorService executorService;
    private final ContentProperties contentProperties;

    @Inject
    public RemoteSpokeStore(@Named("SpokeCuratorCluster") CuratorCluster cluster,
                            StatsdReporter statsdReporter,
                            ContentProperties contentProperties) {
        this.cluster = cluster;
        this.statsdReporter = statsdReporter;
        this.contentProperties = contentProperties;
        executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("RemoteSpokeStore-%d").build());
    }

    static int getQuorum(int size) {
        return (int) Math.max(1, Math.ceil(size / 2.0));
    }

    void testOne(Collection<String> server) throws InterruptedException {
        String path = "Internal-Spoke-Health-Hook/";
        Traces traces = new Traces(path);
        int calls = 10;
        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        CountDownLatch quorumLatch = new CountDownLatch(calls);
        for (int i = 0; i < calls; i++) {
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        ContentKey key = new ContentKey();
                        if (insert(SpokeStore.WRITE, path + key.toUrl(), key.toUrl().getBytes(), server, traces, "payload", path)) {
                            quorumLatch.countDown();
                        } else {
                            traces.log(log);
                        }
                    } catch (Exception e) {
                        log.warn("unexpected exception " + server, e);
                    }
                }
            });
        }
        if (quorumLatch.await(5, TimeUnit.SECONDS)) {
            threadPool.shutdown();
            log.info("completed warmup calls to Spoke {}", server);
        } else {
            threadPool.shutdown();
            throw new RuntimeException("unable to properly connect to Spoke " + server);
        }
    }

    boolean testAll() throws UnknownHostException {
        Collection<String> servers = cluster.getRandomServers();
        servers.addAll(Cluster.getLocalServer());
        log.info("*********************************************");
        log.info("testing servers {}", servers);
        log.info("*********************************************");
        String path = HubHost.getLocalAddressPort();
        for (String server : servers) {
            try {
                log.info("calling server {} path {}", server, path);
                String url = HubHost.getScheme() + server + "/internal/spoke/test/" + path;
                ClientResponse response = query_client.resource(url).get(ClientResponse.class);
                if (response.getStatus() == 200) {
                    log.info("success calling {}", response);
                } else if (response.getStatus() == 404) {
                    log.warn("test not yet implemented {}", response);
                } else {
                    log.warn("failed response {}", response);
                    return false;
                }
            } catch (Exception e) {
                log.warn("unable to test " + path + " with " + server, e);
                return false;
            }
        }
        log.info("all startup tests succeeded  " + path);
        return true;
    }

    public boolean insert(SpokeStore spokeStore, String path, byte[] payload, String spokeApi, String channel) {
        return insert(spokeStore, path, payload, cluster.getWriteServers(), ActiveTraces.getLocal(), spokeApi, channel);
    }

    public boolean insert(SpokeStore spokeStore, String path, byte[] payload, Collection<String> servers, Traces traces,
                          String spokeApi, String channel) {
        int quorum = getQuorum(servers.size());
        CountDownLatch quorumLatch = new CountDownLatch(quorum);
        AtomicBoolean firstComplete = new AtomicBoolean();
        for (final String server : servers) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    setThread(path);
                    String uri = HubHost.getScheme() + server + "/internal/spoke/" + spokeStore + "/" + spokeApi + "/" + path;
                    traces.add(uri);
                    ClientResponse response = null;
                    try {
                        response = write_client.resource(uri).put(ClientResponse.class, payload);
                        traces.add(server, response.getEntity(String.class));
                        if (response.getStatus() == 201) {
                            if (firstComplete.compareAndSet(false, true)) {
                                statsdReporter.time(channel, "heisenberg", traces.getStart());
                            }
                            quorumLatch.countDown();
                            log.trace("server {} path {} response {}", server, path, response);
                        } else {
                            log.info("write failed: server {} path {} response {}", server, path, response);
                        }
                    } catch (Exception e) {
                        traces.add(server, e.getMessage());
                        log.warn("write failed: " + server + " " + path, e);
                    } finally {
                        HubUtils.close(response);
                        resetThread();
                    }

                }
            });
        }
        try {
            quorumLatch.await(contentProperties.getStableSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
        statsdReporter.time(channel, "consistent", traces.getStart());
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

    public Content get(SpokeStore spokeStore, String path, ContentKey key) {
        Collection<String> servers = cluster.getRandomServers();
        for (String server : servers) {
            ClientResponse response = null;
            try {
                setThread(path);
                String url = HubHost.getScheme() + server + "/internal/spoke/" + spokeStore + "/payload/" + path;
                response = query_client.resource(url).get(ClientResponse.class);
                log.trace("server {} path {} response {}", server, path, response);
                if (response.getStatus() == 200) {
                    byte[] entity = response.getEntity(byte[].class);
                    if (entity.length > 0) {
                        return ContentMarshaller.toContent(entity, key);
                    }
                }
            } catch (JsonMappingException e) {
                log.info("JsonMappingException for " + path);
            } catch (ClientHandlerException e) {
                if (e.getCause() != null && e.getCause() instanceof ConnectException) {
                    log.warn("connection exception " + server);
                } else {
                    log.warn("unable to get content " + server + " " + path, e);
                }
            } catch (Exception e) {
                log.warn("unable to get content " + path, e);
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
            executorService.submit(new Runnable() {
                @Override
                public void run() {
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
                            log.warn("connection exception " + server);
                        } else {
                            log.warn("unable to get content " + path, e);
                        }
                        traces.add("ClientHandlerException", e.getMessage(), server);
                    } catch (Exception e) {
                        log.warn("unable to handle " + server + " " + path, e);
                        traces.add("unable to handle ", server, path, e);
                    } finally {
                        HubUtils.close(response);
                        resetThread();
                        countDownLatch.countDown();
                    }
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
            executorService.submit(new Runnable() {
                @Override
                public void run() {
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
                            log.warn("connection exception " + server);
                        } else {
                            log.warn("unable to get content " + path, e);
                        }
                        traces.add("ClientHandlerException", e.getMessage(), server);
                    } catch (Exception e) {
                        log.warn("unable to handle " + server + " " + channel, e);
                        traces.add("unable to handle ", server, channel, e);
                    } finally {
                        HubUtils.close(response);
                        resetThread();
                        countDownLatch.countDown();
                    }
                }
            });
        }
        countDownLatch.await(5, TimeUnit.SECONDS);
        if (orderedKeys.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(orderedKeys.last());
    }

    public boolean delete(SpokeStore spokeStore, String path) throws Exception {
        Collection<String> servers = cluster.getAllServers();
        int quorum = servers.size();
        CountDownLatch countDownLatch = new CountDownLatch(quorum);
        for (final String server : servers) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    ClientResponse response = null;
                    try {
                        setThread(path);
                        response = query_client.resource(HubHost.getScheme() + server + "/internal/spoke/" + spokeStore + "/payload/" + path)
                                .delete(ClientResponse.class);

                        if (response.getStatus() < 400) {
                            countDownLatch.countDown();
                        }
                        log.trace("server {} path {} response {}", server, path, response);
                    } catch (Exception e) {
                        log.warn("unable to delete " + path, e);
                    } finally {
                        HubUtils.close(response);
                        resetThread();
                    }
                }
            });
        }

        return countDownLatch.await(60, TimeUnit.SECONDS);
    }

}
