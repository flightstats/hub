package com.flightstats.datahub.dao.s3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.datahub.dao.timeIndex.TimeIndex;
import com.sun.jersey.api.client.Client;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;

/**
 *
 */
public class TimeIndexCoordinatorVerifier {
    private final static Logger logger = LoggerFactory.getLogger(TimeIndexCoordinatorVerifier.class);
    public static final String BASE_URL = "http://deihub.svc.dev/channel/testy";
    private static ObjectMapper mapper;
    private static Client client;

    public static void main(String[] args) throws IOException {
        client = Client.create();
        mapper = new ObjectMapper();
        for (int i = 1; i <= 100; i++) {
            runChannel(i);
        }
    }

    private static void runChannel(int i) throws IOException {
        DateTime endTime = new DateTime();
        DateTime dateTime = new DateTime(2014, 1, 7, 19, 28);
        String url = BASE_URL + i + "/ids/" + TimeIndex.getHash(dateTime);
        JsonNode startUris = getUris(url);
        String uri = startUris.elements().next().asText();
        logger.info(uri);
        int id = Integer.parseInt(StringUtils.removeStart(uri, BASE_URL + i + "/"));
        logger.info("starting at " + id + " url " + url);
        while (dateTime.isBefore(endTime)) {
            url = BASE_URL + i + "/ids/" + TimeIndex.getHash(dateTime);
            JsonNode uris = getUris(url);
            Iterator<JsonNode> elements = uris.elements();
            while (elements.hasNext()) {
                JsonNode node = elements.next();
                if (!node.asText().endsWith("" + id)) {
                    logger.warn("out of order " + id + " " + url);
                }
                id++;
            }
            dateTime = dateTime.plusMinutes(1);
        }
        logger.info("completed " + dateTime + " for " + i);
    }

    private static JsonNode getUris(String url) throws IOException {
        String get = client.resource(url).get(String.class);
        JsonNode jsonNode = mapper.readTree(get);
        return jsonNode.get("_links").get("uris");
    }
}
