package com.flightstats.hub.spoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.rest.Headers;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * want to know:
 * content size
 * content type
 * compression rate
 * frequency
 */
public class MarhallSampler {
    private final static Logger logger = LoggerFactory.getLogger(MarhallSampler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        Client client = getClient();
        ExecutorService threadPool = Executors.newCachedThreadPool();

        JsonNode channelsNode = mapper.readTree(client.resource("http://hub.svc.prod/channel").get(String.class));
        JsonNode channels = channelsNode.get("_links").get("channels");
        for (JsonNode channel : channels) {
            logger.info("channel {}", channel.get("name"));
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        getLatest(channel, client);
                    } catch (IOException e) {
                        logger.warn(channel.asText(), e);
                    }

                }
            });
        }

    }

    private static void getLatest(JsonNode channel, Client client) throws IOException {
        String name = channel.get("name").asText();
        String latestUrl = channel.get("href").asText() + "/latest";
        logger.info("calling " + latestUrl);
        ClientResponse latest = client.resource(latestUrl)
                .accept(MediaType.WILDCARD_TYPE)
                .header(HttpHeaders.ACCEPT_ENCODING, "gzip")
                .get(ClientResponse.class);
        if (latest.getStatus() != 200) {
            System.out.println(name + "," + latest.getStatus());
            return;
        }
        Content content = Content.builder()
                .withContentType(latest.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE))
                .withContentLanguage(latest.getHeaders().getFirst(Headers.LANGUAGE))
                .withUser(latest.getHeaders().getFirst(Headers.USER))
                .withData(latest.getEntity(byte[].class))
                .build();

        byte[] bytes = SpokeMarshaller.toBytes(content);
        int dataLength = content.getData().length +
                content.getContentType().or("").getBytes().length +
                content.getContentLanguage().or("").getBytes().length +
                content.getUser().or("").getBytes().length;
        System.out.println(name + "," + latest.getStatus() + "," + content.getContentType().or("none") + "," +
                dataLength + "," + bytes.length);
    }

    private static Client getClient() {
        Client client = Client.create();
        client.setConnectTimeout(30 * 1000);
        client.setReadTimeout(30 * 1000);
        client.addFilter(new com.sun.jersey.api.client.filter.GZIPContentEncodingFilter());
        client.setFollowRedirects(true);
        return client;
    }
}
