package com.flightstats.hub.spoke;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.metrics.MetricsSender;
import com.flightstats.hub.model.*;
import com.flightstats.hub.rest.RestClient;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"Convert2streamapi", "Convert2Lambda"})
public class RemoteSpokeStore {

    private final static Logger logger = LoggerFactory.getLogger(RemoteSpokeStore.class);

    private final static Client write_client = RestClient.createClient(1, 2, true);
    private final static Client query_client = RestClient.createClient(5, 2 * 60, true);

    private final CuratorSpokeCluster cluster;
    private final MetricsSender sender;
    private final ExecutorService executorService;
    private final int stableSeconds = HubProperties.getProperty("app.stable_seconds", 5);

    @Inject
    public RemoteSpokeStore(CuratorSpokeCluster cluster, MetricsSender sender) {
        this.cluster = cluster;
        this.sender = sender;
        executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("RemoteSpokeStore-%d").build());
        HubServices.register(new SpokeHealthHook(), HubServices.TYPE.INITIAL_POST_START);
    }

    private class SpokeHealthHook extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            ClientResponse health = RestClient.defaultClient()
                    .resource(HubHost.getLocalUriRoot() + "/health")
                    .get(ClientResponse.class);
            logger.info("localhost health {}", health);
            Collection<String> server = CuratorSpokeCluster.getLocalServer();
            String path = "Internal-Spoke-Health-Hook/";
            TracesImpl traces = new TracesImpl();
            for (int i = 0; i < 100; i++) {
                ContentKey key = new ContentKey();
                if (!write(path + key.toUrl(), key.toUrl().getBytes(), server, traces)) {
                    traces.log(logger);
                    throw new RuntimeException("unable to properly start Spoke, should exit!");
                }
            }
            logger.info("completed warmup calls to Spoke!");
        }

        @Override
        protected void shutDown() throws Exception {
        }
    }

    public boolean write(String path, byte[] payload, Content content) throws InterruptedException {
        return write(path, payload, cluster.getServers(), content.getTraces());
    }

    private boolean write(final String path, final byte[] payload, Collection<String> servers, final Traces traces) throws InterruptedException {
        int quorum = getQuorum(servers.size());
        CountDownLatch quorumLatch = new CountDownLatch(quorum);
        AtomicBoolean reported = new AtomicBoolean();
        for (final String server : servers) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    String uri = HubHost.getScheme() + server + "/internal/spoke/payload/" + path;
                    traces.add(new Trace(uri));
                    try {
                        ClientResponse response = write_client.resource(uri).put(ClientResponse.class, payload);
                        long complete = System.currentTimeMillis();
                        traces.add(new Trace(server, response.getEntity(String.class)));
                        if (response.getStatus() == 201) {
                            if (reported.compareAndSet(false, true)) {
                                sender.send("heisenberg", complete - traces.getStart());
                            }
                            quorumLatch.countDown();
                            logger.trace("server {} path {} response {}", server, path, response);
                        } else {
                            logger.info("write failed: server {} path {} response {}", server, path, response);
                        }
                        response.close();
                    } catch (Exception e) {
                        traces.add(new Trace(server, e.getMessage()));
                        logger.warn("write failed: " + server + " " + path, e);
                    }

                }
            });
        }
        quorumLatch.await(stableSeconds, TimeUnit.SECONDS);
        sender.send("consistent", System.currentTimeMillis() - traces.getStart());
        return quorumLatch.getCount() != quorum;
    }

    static int getQuorum(int size) {
        return (int) Math.max(1, Math.ceil(size / 2.0));
    }

    public Content read(String path, ContentKey key) {
        Collection<String> servers = cluster.getRandomServers();
        for (String server : servers) {
            try {
                ClientResponse response = query_client.resource(HubHost.getScheme() + server + "/internal/spoke/payload/" + path)
                        .get(ClientResponse.class);
                logger.trace("server {} path {} response {}", server, path, response);
                if (response.getStatus() == 200) {
                    byte[] entity = response.getEntity(byte[].class);
                    return SpokeMarshaller.toContent(entity, key);
                }
            } catch (JsonMappingException e) {
                logger.info("JsonMappingException for " + path);
            } catch (Exception e) {
                logger.warn("unable to get content " + path, e);
            }
        }
        return null;
    }

    public SortedSet<ContentKey> readTimeBucket(String channel, String timePath, Traces traces) throws InterruptedException {
        Collection<String> servers = cluster.getServers();
        CountDownLatch countDownLatch = new CountDownLatch(servers.size());
        String path = channel + "/" + timePath;
        SortedSet<ContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        for (final String server : servers) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        traces.add("spoke calling", server, path);
                        ClientResponse response = query_client.resource(HubHost.getScheme() + server + "/internal/spoke/time/" + path)
                                .get(ClientResponse.class);
                        traces.add("server response", server, response);
                        if (response.getStatus() == 200) {
                            SortedSet<ContentKey> keySet = new TreeSet<>();
                            String keysString = response.getEntity(String.class);
                            ContentKeyUtil.convertKeyStrings(keysString, keySet);
                            traces.add(server, keySet);
                            orderedKeys.addAll(keySet);
                        }
                    } catch (ClientHandlerException e) {
                        logger.warn("ClientHandlerException " + e.getMessage());
                        traces.add("ClientHandlerException", e.getMessage());
                    } catch (Exception e) {
                        logger.warn("unable to handle " + server + " " + path, e);
                        traces.add("unable to handle ", server, path, e);
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            });
        }
        countDownLatch.await(20, TimeUnit.SECONDS);
        return orderedKeys;
    }

    public Optional<ContentKey> getLatest(String channel, String path, Traces traces) throws InterruptedException {
        Collection<String> servers = cluster.getServers();
        CountDownLatch countDownLatch = new CountDownLatch(servers.size());
        SortedSet<ContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        for (final String server : servers) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        traces.add("spoke calling", server, channel);
                        ClientResponse response = query_client.resource(HubHost.getScheme() + server + "/internal/spoke/latest/" + path)
                                .get(ClientResponse.class);
                        traces.add("server response", server, response);
                        if (response.getStatus() == 200) {
                            String key = response.getEntity(String.class);
                            if (StringUtils.isNotEmpty(key)) {
                                orderedKeys.add(ContentKeyUtil.convertKey(key).get());
                            }
                            traces.add(server, key);
                        }
                    } catch (ClientHandlerException e) {
                        logger.warn("ClientHandlerException " + e.getMessage());
                        traces.add("ClientHandlerException", e.getMessage());
                    } catch (Exception e) {
                        logger.warn("unable to handle " + server + " " + channel, e);
                        traces.add("unable to handle ", server, channel, e);
                    } finally {
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

    public boolean delete(String path) throws Exception {
        Collection<String> servers = cluster.getServers();
        int quorum = servers.size();
        CountDownLatch countDownLatch = new CountDownLatch(quorum);
        for (final String server : servers) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    ClientResponse response = query_client.resource(HubHost.getScheme() + server + "/internal/spoke/payload/" + path)
                            .delete(ClientResponse.class);
                    if (response.getStatus() < 400) {
                        countDownLatch.countDown();
                    }
                    logger.trace("server {} path {} response {}", server, path, response);
                }
            });
        }

        return countDownLatch.await(60, TimeUnit.SECONDS);
    }
}
