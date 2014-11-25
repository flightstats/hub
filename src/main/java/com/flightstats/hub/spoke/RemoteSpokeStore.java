package com.flightstats.hub.spoke;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.flightstats.hub.metrics.HostedGraphiteSender;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.Trace;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

@SuppressWarnings({"Convert2streamapi", "Convert2Lambda"})
public class RemoteSpokeStore {

    private final static Logger logger = LoggerFactory.getLogger(RemoteSpokeStore.class);

    private final static Client client = create();

    private final SpokeCluster cluster;
    private final FileSpokeStore localSpokeStore;
    private final HostedGraphiteSender sender;
    private final ExecutorService executorService;

    @Inject
    public RemoteSpokeStore(SpokeCluster cluster, FileSpokeStore localSpokeStore, HostedGraphiteSender sender) {
        this.cluster = cluster;
        this.localSpokeStore = localSpokeStore;
        this.sender = sender;
        executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("RemoteSpokeStore-%d").build());
    }

    private static Client create() {
        Client client = Client.create();
        client.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(5));
        client.setReadTimeout((int) TimeUnit.SECONDS.toMillis(5));
        return client;
    }

    public boolean write(String path, byte[] payload, Content content) throws InterruptedException {
        localSpokeStore.write(path, payload);
        sender.send("heisenberg", System.currentTimeMillis() - content.getContentKey().get().getMillis());
        content.getTraces().add(new Trace("wrote to localSpokeStore"));

        List<String> servers = cluster.getOtherServers();
        if (servers.size() == 0) return true;
        int quorum = servers.size() - 1;
        CountDownLatch countDownLatch = new CountDownLatch(quorum);
        for (final String server : servers) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    content.getTraces().add(new Trace(server));
                    try {
                        ClientResponse response = client.resource("http://" + server + "/spoke/payload/" + path)
                                .put(ClientResponse.class, payload);
                        content.getTraces().add(new Trace(response));
                        if (response.getStatus() == 201) {
                            countDownLatch.countDown();
                            logger.trace("server {} path {} response {}", server, path, response);
                        } else {
                            logger.info("write failed: server {} path {} response {}", server, path, response);
                        }
                    } catch (Exception e) {
                        content.getTraces().add(new Trace(server, e.getMessage()));
                        logger.warn("write failed: " + server + " " + path, e);
                    }

                }
            });
        }
        //todo - gfm - 11/13/14 - this could be smarter with waiting.  should we return success if one succeeds?
        boolean awaited = countDownLatch.await(30, TimeUnit.SECONDS);
        sender.send("consistent", System.currentTimeMillis() - content.getContentKey().get().getMillis());
        return awaited;
    }

    public com.flightstats.hub.model.Content read(String path, ContentKey key) {
        List<String> servers = cluster.getRandomServers();
        for (String server : servers) {
            try {
                ClientResponse response = client.resource("http://" + server + "/spoke/payload/" + path)
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

    public Collection<ContentKey> readTimeBucket(String channel, String timePath) throws InterruptedException {
        List<String> servers = cluster.getServers();
        CountDownLatch countDownLatch = new CountDownLatch(servers.size());
        String path = channel + "/" + timePath;
        Set<ContentKey> results = Sets.newConcurrentHashSet();
        for (final String server : servers) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        ClientResponse response = client.resource("http://" + server + "/spoke/time/" + path)
                                .get(ClientResponse.class);
                        logger.trace("server {} path {} response {}", server, path, response);
                        if (response.getStatus() == 200) {
                            String keysString = response.getEntity(String.class);
                            if (StringUtils.isNotEmpty(keysString)) {
                                logger.trace("entity '{}'", keysString);
                                String[] keys = keysString.split(",");
                                for (String key : keys) {
                                    results.add(ContentKey.fromUrl(StringUtils.substringAfter(key, "/")).get());
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("unable to handle " + server + " " + path, e);
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            });
        }
        countDownLatch.await(30, TimeUnit.SECONDS);
        return results;
    }

    public String readAdjacent(String path, boolean readNext) throws InterruptedException{
        // TODO bc 11/18/14: use lambdas
        // read from as many servers as we can
        // put results into a sorted set
        // read and return the payload
        List<String> servers = cluster.getServers();
        int serverCount = servers.size();

        CompletionService<String> compService = new ExecutorCompletionService<>(
                Executors.newFixedThreadPool(serverCount));

        SortedSet<String> keySet = new TreeSet<>();  // result accumulator

        // Futures for all submitted Callables that have not yet been checked
        Set<Future<String>> futures = new HashSet<>();

        for (final String server : servers) {
            // keep track of the futures that get created so we can cancel them if necessary
            futures.add(compService.submit(new Callable<String>(){
                @Override public String call(){
                    ClientResponse response = client.resource("http://" + server + "/spoke/next/" + path )
                            .get(ClientResponse.class);
                    logger.trace("server {} path {} response {}", server, path, response);

                    if (response.getStatus() == 200) {
                        response.bufferEntity();
                        return response.getEntity(String.class);
                        }
                    logger.trace("server {} path {} response {}", server, path, response);
                    return null; // TODO bc 11/17/14: should this be an exception?
                }
            }));
        }

        int received = 0;
        boolean errors = false;

        while(received < serverCount && !errors) {
            Future<String> resultFuture = compService.take(); //blocks if none available
            try {
                String key = resultFuture.get();
                if(key != null) keySet.add(key);
                received ++;
            }
            catch(Exception e) {
                //log
                errors = true;
            }
        }
        if(readNext) return keySet.first();
        return keySet.last();
    }

    public String readNext(String path) throws InterruptedException{
        return readAdjacent(path, true);
    }

    public String readPrevious(String path) throws InterruptedException{
        return readAdjacent(path, false);
    }

    public boolean delete(String path) throws Exception {
        //todo - gfm - 11/25/14 - //do nothing, let data fall off the TTL window?
        return true;
    }
}
