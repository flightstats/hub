package com.flightstats.datahub.service;

import com.flightstats.datahub.app.DataHubMain;
import com.flightstats.jerseyguice.jetty.JettyServer;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * The goal of this is to test the interface from end to end, using all of the real technologies we can,
 * such as HazelCast, Cassandra, Jersey, etc.
 */
public class DataHubIntegration {
    private final static Logger logger = LoggerFactory.getLogger(DataHubIntegration.class);

    public static final String CHANNEL_URL = "http://localhost:8080/channel/";
    public static final String CHANNEL_NAME = "testLifecycle";
    private static JettyServer server;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        //todo - gfm - 11/4/13 - does this need to use a custom yaml?
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();
        //todo - gfm - 11/4/13 - this path needs to be generic :)
        String[] args = {"/Users/gmoulliet/code/datahub/datahub-service/src/test/conf/datahub.properties"};
        server = DataHubMain.startServer(args);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
        server.halt();
    }

    @Test
    public void testLifecyle() throws Exception {
        Client client = Client.create();
        //create a channel
        ObjectNode channelNode = mapper.createObjectNode();
        channelNode.put("name", CHANNEL_NAME);
        channelNode.put("ttlMillis", 3600000);
        ClientResponse response = client.resource(CHANNEL_URL)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, channelNode.toString());
        assertEquals(201, response.getStatus());
        WebResource channel = client.resource(CHANNEL_URL + CHANNEL_NAME);
        ClientResponse channelResponse = channel.get(ClientResponse.class);
        assertEquals(200, channelResponse.getStatus());
        logger.info(channelResponse.getEntity(String.class));
        //post 100 items
        for (int i = 0; i < 100; i++) {
            ClientResponse post = channel.type(MediaType.TEXT_PLAIN_TYPE)
                    .post(ClientResponse.class, "content value " + i);
            assertEquals(201, post.getStatus());
        }
        //pull latest
        WebResource latest = client.resource(CHANNEL_URL + CHANNEL_NAME + "/latest");
        ClientResponse head = latest.accept(MediaType.TEXT_PLAIN_TYPE).head();
        //todo - gfm - 11/4/13 - how to verify this?
        //200  {Creation-Date=[2013-11-05T01:57:05.919Z], Link=[<http://localhost:8080/channel/testLifecycle/100>;rel="next", <http://localhost:8080/channel/testLifecycle/98>;rel="previous"], Vary=[Accept-Encoding], Content-Length=[16], Content-Type=[text/plain], Server=[Jetty(9.0.3.v20130506)]}
        logger.info(head.getStatus() + " " + head.getEntity(String.class) + " " + head.getHeaders());
        //iterate through all 100 items

        for (int i = 0; i < 100; i++) {
            WebResource data = client.resource(CHANNEL_URL + CHANNEL_NAME + "/" + i);
            ClientResponse get = data.accept(MediaType.TEXT_PLAIN_TYPE)
                    .get(ClientResponse.class);
            assertEquals(200, get.getStatus());
            assertEquals("content value " + i, get.getEntity(String.class));
            List<String> links = get.getHeaders().get("Link");
            if (i == 0) {
                assertEquals(1, links.size());
                assertEquals("<http://localhost:8080/channel/testLifecycle/1>;rel=\"next\"", links.get(0));
            } else if (i == 99) {
                assertEquals(1, links.size());
                assertEquals("<http://localhost:8080/channel/testLifecycle/98>;rel=\"previous\"", links.get(0));
            } else {
                assertEquals(2, links.size());
                assertEquals("<http://localhost:8080/channel/testLifecycle/" + (i + 1) + ">;rel=\"next\"", links.get(0));
                assertEquals("<http://localhost:8080/channel/testLifecycle/" + (i - 1) + ">;rel=\"previous\"", links.get(1));
            }


        }

    }


}
