package com.flightstats.hub.spoke;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.metrics.MetricsSender;
import com.flightstats.hub.model.*;
import com.flightstats.hub.rest.RestClient;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
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

    private final static Client client = RestClient.createClient(5, 5);

    private final SpokeCluster cluster;
    private final MetricsSender sender;
    private final ExecutorService executorService;

    @Inject
    public RemoteSpokeStore(SpokeCluster cluster, MetricsSender sender) {
        this.cluster = cluster;
        this.sender = sender;
        executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("RemoteSpokeStore-%d").build());
    }

    public boolean write(String path, byte[] payload, Content content) throws InterruptedException {
        List<String> servers = cluster.getServers();
        int quorum = getQuorum(servers.size());
        CountDownLatch countDownLatch = new CountDownLatch(quorum);
        AtomicBoolean reported = new AtomicBoolean();
        for (final String server : servers) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    String uri = HubHost.getScheme() + server + "/spoke/payload/" + path;
                    content.getTraces().add(new Trace(uri));
                    try {
                        ClientResponse response = client.resource(uri).put(ClientResponse.class, payload);
                        long complete = System.currentTimeMillis();
                        content.getTraces().add(new Trace(server, response.getEntity(String.class)));
                        if (response.getStatus() == 201) {
                            if (reported.compareAndSet(false, true)) {
                                sender.send("heisenberg", complete - content.getTraces().getStart());
                            }
                            countDownLatch.countDown();
                            logger.trace("server {} path {} response {}", server, path, response);
                        } else {
                            logger.info("write failed: server {} path {} response {}", server, path, response);
                        }
                        response.close();
                    } catch (Exception e) {
                        content.getTraces().add(new Trace(server, e.getMessage()));
                        logger.warn("write failed: " + server + " " + path, e);
                    }

                }
            });
        }
        boolean awaited = countDownLatch.await(30, TimeUnit.SECONDS);
        sender.send("consistent", System.currentTimeMillis() - content.getTraces().getStart());
        return awaited;
    }

    static int getQuorum(int size) {
        return (int) Math.max(1, Math.ceil(size / 2.0));
    }

    public com.flightstats.hub.model.Content read(String path, ContentKey key) {
        List<String> servers = cluster.getRandomServers();
        for (String server : servers) {
            try {
                ClientResponse response = client.resource(HubHost.getScheme() + server + "/spoke/payload/" + path)
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
        List<String> servers = cluster.getServers();
        CountDownLatch countDownLatch = new CountDownLatch(servers.size());
        String path = channel + "/" + timePath;
        SortedSet<ContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        for (final String server : servers) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        traces.add("spoke calling", server, path);
                        ClientResponse response = client.resource(HubHost.getScheme() + server + "/spoke/time/" + path)
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
        countDownLatch.await(30, TimeUnit.SECONDS);
        return orderedKeys;
    }

    public Optional<ContentKey> getLatest(String channel, String path, Traces traces) throws InterruptedException {
        List<String> servers = cluster.getServers();
        CountDownLatch countDownLatch = new CountDownLatch(servers.size());
        SortedSet<ContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        for (final String server : servers) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        traces.add("spoke calling", server, channel);
                        ClientResponse response = client.resource(HubHost.getScheme() + server + "/spoke/latest/" + path)
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
        List<String> servers = cluster.getServers();
        int quorum = servers.size();
        CountDownLatch countDownLatch = new CountDownLatch(quorum);
        for (final String server : servers) {
            //noinspection Convert2Lambda
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    ClientResponse response = client.resource(HubHost.getScheme() + server + "/spoke/payload/" + path)
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
