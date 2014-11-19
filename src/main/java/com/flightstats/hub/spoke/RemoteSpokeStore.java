package com.flightstats.hub.spoke;

import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.*;

public class RemoteSpokeStore {

    private final static Logger logger = LoggerFactory.getLogger(RemoteSpokeStore.class);

    private final static Client client = create();

    private final SpokeCluster cluster;
    private final ExecutorService executorService;

    @Inject
    public RemoteSpokeStore(SpokeCluster cluster) {
        this.cluster = cluster;
        //todo - gfm - 11/13/14 - name this executorService
        executorService = Executors.newCachedThreadPool();
    }

    private static Client create() {
        Client client = Client.create();
        client.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(5));
        client.setReadTimeout((int) TimeUnit.SECONDS.toMillis(5));
        return client;
    }

    public boolean write(String path, byte[] payload) throws InterruptedException {
        List<String> servers = cluster.getServers();
        int quorum = Math.max(1, servers.size() - 1);
        CountDownLatch countDownLatch = new CountDownLatch(quorum);

        for (final String server : servers) {
            //todo - gfm - 11/13/14 - we need to upgrade to Jersey 2.x or Spark for lambdas
            //noinspection Convert2Lambda
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        ClientResponse response = client.resource("http://" + server + "/spoke/payload/" + path)
                                .put(ClientResponse.class, payload);
                        if (response.getStatus() == 201) {
                            countDownLatch.countDown();
                            logger.trace("server {} path {} response {}", server, path, response);
                        } else {
                            logger.info("write failed: server {} path {} response {}", server, path, response);
                        }
                    } catch (Exception e) {
                        logger.warn("write failed: " + server + " " + path, e);
                    }

                }
            });
        }
        //todo - gfm - 11/13/14 - this should be smarter with waiting.  should we return success if one succeeds?
        return countDownLatch.await(60, TimeUnit.SECONDS);
    }

    public com.flightstats.hub.model.Content read(String path, ContentKey key) {
        //todo - gfm - 11/13/14 - this could do read repair
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
            } catch (Exception e) {
                logger.warn("unable to get content " + path, e);
            }
        }
        return null;
    }

    // read from 3 servers and do set intersection and sort
    public Collection<String> readTimeBucket(String path)throws InterruptedException {
        List<String> servers = cluster.getServers();
        // TODO bc 11/17/14: Can we make this read from a subset of the cluster and get all results?
        int serverCount = servers.size();

        CompletionService<List<String>> compService = new ExecutorCompletionService<>(
                Executors.newFixedThreadPool(serverCount));

        SortedSet<String> keySet = new TreeSet<>();  // result accumulator

        // Futures for all submitted Callables that have not yet been checked
        Set<Future<List<String>>> futures = new HashSet<>();

        for (final String server : servers) {
            // keep track of the futures that get created so we can cancel them if necessary
            futures.add(compService.submit(new Callable<List<String>>(){
                @Override public List<String> call(){
                    ClientResponse response = client.resource("http://" + server + "/spoke/time/" + path)
                            .get(ClientResponse.class);
                    logger.trace("server {} path {} response {}", server, path, response);

                    if (response.getStatus() == 200) {
                        String keysString = response.getEntity(String.class);
                        String[] keys = keysString.split(",");
                        return Arrays.asList(keys);
                    }
                    logger.trace("server {} path {} response {}", server, path, response);
                    return new ArrayList<>(); // TODO bc 11/17/14: should this be an exception?
                }
            }));
        }

        int received = 0;
        boolean errors = false;

        while(received < serverCount && !errors) {
            Future<List<String>> resultFuture = compService.take(); //blocks if none available
            try {
                List<String> keys = resultFuture.get();
                keySet.addAll(keys);
                received ++;
            }
            catch(Exception e) {
                //log
                errors = true;
            }
        }
        ArrayList<String> contentKeys = new ArrayList<>();
        for(String key : keySet){
            contentKeys.add(key);
        }
        return contentKeys;
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
        //todo - gfm - 11/13/14 - this could be merged with some of the write code
        List<String> servers = cluster.getServers();
        int quorum = Math.max(1, servers.size() - 1);
        CountDownLatch countDownLatch = new CountDownLatch(quorum);
        for (final String server : servers) {
            //todo - gfm - 11/13/14 - we need to upgrade to Jersey 2.x for lambdas
            //noinspection Convert2Lambda
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    ClientResponse response = client.resource("http://" + server + "/spoke/payload/" + path)
                            .delete(ClientResponse.class);
                    if (response.getStatus() < 400) {
                        countDownLatch.countDown();
                    }
                    logger.trace("server {} path {} response {}", server, path, response);
                }
            });
        }

        //todo - gfm - 11/13/14 - this should be smarter with waiting.  should we return success if one succeeds?
        return countDownLatch.await(60, TimeUnit.SECONDS);
    }
}
