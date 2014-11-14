package com.flightstats.hub.spoke;

import com.flightstats.hub.model.ContentKey;
import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
        String[] servers = cluster.getServers();
        //todo - gfm - 11/13/14 - change this to be cluster aware
        int quorum = Math.max(1, servers.length - 1);
        CountDownLatch countDownLatch = new CountDownLatch(quorum);

        for (final String server : servers) {
            //todo - gfm - 11/13/14 - we need to upgrade to Jersey 2.x for lambdas
            //noinspection Convert2Lambda
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    ClientResponse response = client.resource("http://" + server + "/spoke/payload/" + path)
                            .put(ClientResponse.class, payload);
                    if (response.getStatus() == 201) {
                        countDownLatch.countDown();
                    }
                    logger.trace("server {} path {} response {}", server, path, response);
                }
            });
        }
        //todo - gfm - 11/13/14 - this should be smarter with waiting.  should we return success if one succeeds?
        return countDownLatch.await(60, TimeUnit.SECONDS);
    }

    public com.flightstats.hub.model.Content read(String path, ContentKey key) {
        //todo - gfm - 11/13/14 - this could do read repair
        String[] servers = cluster.getServers();
        for (String server : servers) {
            ClientResponse response = client.resource("http://" + server + "/spoke/payload/" + path)
                    .get(ClientResponse.class);
            logger.trace("server {} path {} response {}", server, path, response);
            if (response.getStatus() == 200) {
                byte[] entity = response.getEntity(byte[].class);
                try {
                    return SpokeMarshaller.toContent(entity, key);
                } catch (IOException e) {
                    logger.warn("unable to parse content " + path);
                }
            }
        }
        return null;
    }

    public boolean delete(String path) throws Exception {
        //todo - gfm - 11/13/14 - this could be merged with some of the write code
        String[] servers = cluster.getServers();
        //todo - gfm - 11/13/14 - change this to be cluster aware
        int quorum = Math.max(1, servers.length - 1);
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
