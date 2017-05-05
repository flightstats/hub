package com.flightstats.hub.spoke;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.cluster.Cluster;
import com.flightstats.hub.cluster.DynamicSpokeCluster;
import com.flightstats.hub.dao.ContentKeyUtil;
import com.flightstats.hub.dao.ContentMarshaller;
import com.flightstats.hub.dao.QueryResult;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"Convert2streamapi", "Convert2Lambda"})
public class RemoteSpokeStore {

    private final static Logger logger = LoggerFactory.getLogger(RemoteSpokeStore.class);

    private final static Client write_client = RestClient.createClient(1, 5, true, false);
    private final static Client query_client = RestClient.createClient(5, 15, true, true);

    private final Cluster cluster;
    private final MetricsService metricsService;
    private final ExecutorService executorService;
    private final int stableSeconds = HubProperties.getProperty("app.stable_seconds", 5);

    @Inject
    public RemoteSpokeStore(DynamicSpokeCluster cluster, MetricsService metricsService) {
        this.cluster = cluster;
        this.metricsService = metricsService;
        executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("RemoteSpokeStore-%d").build());
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
                        if (insert(path + key.toUrl(), key.toUrl().getBytes(), server, traces, "payload", path)) {
                            quorumLatch.countDown();
                        } else {
                            traces.log(logger);
                        }
                    } catch (Exception e) {
                        logger.warn("unexpected exception " + server, e);
                    }
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

    boolean testAll() throws UnknownHostException {
        Collection<String> servers = cluster.getServers(RandomStringUtils.randomAlphabetic(3));
        servers.addAll(cluster.getLocalServer());
        logger.info("*********************************************");
        logger.info("testing servers {}", servers);
        logger.info("*********************************************");
        String path = HubHost.getLocalAddressPort();
        for (String server : servers) {
            try {
                logger.info("calling server {} path {}", server, path);
                ClientResponse response = query_client.resource(HubHost.getScheme() + server + "/internal/spoke/test/" + path)
                        .get(ClientResponse.class);
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

    public boolean insert(String path, byte[] payload, String spokeApi, String channel) throws InterruptedException {
        return insert(path, payload, cluster.getServers(channel), ActiveTraces.getLocal(), spokeApi, channel);
    }

    private boolean insert(String path, byte[] payload, Collection<String> servers, Traces traces,
                           String spokeApi, String channel) throws InterruptedException {
        int quorum = getQuorum(servers.size());
        CountDownLatch quorumLatch = new CountDownLatch(quorum);
        AtomicBoolean firstComplete = new AtomicBoolean();
        for (final String server : servers) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    setThread(path);
                    String uri = HubHost.getScheme() + server + "/internal/spoke/" + spokeApi + "/" + path;
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

                }
            });
        }
        quorumLatch.await(stableSeconds, TimeUnit.SECONDS);
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

    public Content get(String channelName, String path, ContentKey key) {
        Collection<String> servers = cluster.randomize(cluster.getServers(channelName, key.getTime()));
        for (String server : servers) {
            ClientResponse response = null;
            try {
                setThread(path);
                response = query_client.resource(HubHost.getScheme() + server + "/internal/spoke/payload/" + path)
                        .get(ClientResponse.class);
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

    QueryResult readTimeBucket(TimeQuery query) throws InterruptedException {
        DateTime endTime = query.getStartTime().plus(query.getUnit().getDuration());
        Set<String> servers = cluster.getServers(query.getChannelName(), query.getStartTime(), endTime);
        String timePath = query.getUnit().format(query.getStartTime());
        return getKeys("/internal/spoke/time/" + query.getChannelName() + "/" + timePath, servers);
    }

    SortedSet<ContentKey> getNext(String channel, int count, ContentKey startKey) throws InterruptedException {
        Set<String> servers = cluster.getServers(channel, startKey.getTime(), TimeUtil.now());
        return getKeys("/internal/spoke/next/" + channel + "/" + count + "/" + startKey.toUrl(), servers).getContentKeys();
    }

    private QueryResult getKeys(final String path, Collection<String> servers) throws InterruptedException {
        Traces traces = ActiveTraces.getLocal();
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
                }
            });
        }
        countDownLatch.await(20, TimeUnit.SECONDS);
        return queryResult;
    }

    public Optional<ContentKey> getLatest(String channel, String path, Traces traces) throws InterruptedException {
        //todo - gfm - this needs to use the time interface
        //todo - gfm - how much of the range should we use?
        Collection<String> servers = cluster.getServers(channel);
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
                }
            });
        }
        countDownLatch.await(5, TimeUnit.SECONDS);
        if (orderedKeys.isEmpty()) {
            return Optional.absent();
        }
        return Optional.of(orderedKeys.last());
    }

    public boolean delete(String channel) throws Exception {
        //todo - gfm - this needs to use the time interval interface
        //todo - gfm - this should use all in Spoke range
        Collection<String> servers = cluster.getServers(channel);
        int quorum = servers.size();
        CountDownLatch countDownLatch = new CountDownLatch(quorum);
        for (final String server : servers) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    ClientResponse response = null;
                    try {
                        setThread(channel);
                        response = query_client.resource(HubHost.getScheme() + server + "/internal/spoke/payload/" + channel)
                                .delete(ClientResponse.class);
                        if (response.getStatus() < 400) {
                            countDownLatch.countDown();
                        }
                        logger.trace("server {} path {} response {}", server, channel, response);
                    } catch (Exception e) {
                        logger.warn("unable to delete " + channel, e);
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
