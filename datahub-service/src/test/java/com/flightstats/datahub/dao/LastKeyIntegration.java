package com.flightstats.datahub.dao;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;

/**
 * This is useful for testing the cold restart scenario with Hazelcast, Cassandra and sequential keys.
 * To run on a single development machine:
 *  run DataHubCassandra - let this run the entire time
 *  run DataHubMain
 *  run LastKeyIntegration
 *  verify that http://localhost:8080/channel/LastKeyIntegration/latest
 *      redirects to http://localhost:8080/channel/LastKeyIntegration/2999
 *  restart DataHubMain (clearing the hazelcast data)
 *  run LastKeyIntegration
 *  verify that http://localhost:8080/channel/LastKeyIntegration/latest
 *      redirects to http://localhost:8080/channel/LastKeyIntegration/4999
 *
 */
public class LastKeyIntegration {
    private final static Logger logger = LoggerFactory.getLogger(LastKeyIntegration.class);

    public static final String CHANNEL_URL = "http://localhost:8080/channel/";
    public static final String CHANNEL_NAME = "LastKeyIntegration";
    public static final long LOOPS = SequenceRowKeyStrategy.INCREMENT * 2;
    private final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        new LastKeyIntegration().testLifecyle();
    }


    public void testLifecyle() throws Exception {
        Client client = Client.create();
        //create a channel
        ObjectNode channelNode = mapper.createObjectNode();
        channelNode.put("name", CHANNEL_NAME);
        channelNode.put("ttlMillis", 3600000);
        ClientResponse response = client.resource(CHANNEL_URL)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, channelNode.toString());
        logger.info("channel creation response " + response.getStatus());
        WebResource channel = client.resource(CHANNEL_URL + CHANNEL_NAME);
        //post items
        for (int i = 0; i < LOOPS; i++) {
            ClientResponse post = channel.type(MediaType.TEXT_PLAIN_TYPE)
                    .post(ClientResponse.class, "content value " + i);
            assertEquals(201, post.getStatus());
        }

        //pull latest
        WebResource latest = client.resource(CHANNEL_URL + CHANNEL_NAME + "/latest");
        ClientResponse head = latest.accept(MediaType.TEXT_PLAIN_TYPE).head();
        logger.info(head.getStatus() + " " + head.getEntity(String.class) + " " + head.getHeaders());


    }

    private void assertEquals(int i1, int i2) {
        if (i1 != i2) throw new RuntimeException("expected " + i1 + " but got " + i2);
    }

}
