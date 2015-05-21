package com.flightstats.hub.alert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProperties;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AlertStatuses {
    private final static Logger logger = LoggerFactory.getLogger(AlertStatuses.class);

    private final String hubAppUrl;
    private final Client client;
    private final String alertStatusName;
    private final static ObjectMapper mapper = new ObjectMapper();


    public AlertStatuses(String hubAppUrl, Client client) {
        this.hubAppUrl = hubAppUrl;
        this.client = client;
        alertStatusName = HubProperties.getProperty("alert.channel.status", "zomboAlertStatus");
    }

    public void create() {
        client.resource(hubAppUrl + "channel/" + alertStatusName)
                .type(MediaType.APPLICATION_JSON)
                .put("{\"ttlDays\":7, \"description:\"Status for hub alerts\"}");
    }

    public void getLatest() {
        Map<String, AlertStatus> statusMap = new HashMap<>();

    }

    Map<String, Boolean> loadStatus() {

        Map<String, Boolean> alertStates = new HashMap<>();
        ClientResponse response = client.resource(hubAppUrl + "channel/" + alertStatusName + "/latest")
                .get(ClientResponse.class);
        if (response.getStatus() == 200) {
            try {
                String entity = response.getEntity(String.class);
                JsonNode node = mapper.readTree(entity);
                Iterator<String> names = node.fieldNames();
                while (names.hasNext()) {
                    String name = names.next();
                    JsonNode jsonNode = node.get(name);
                    if (jsonNode.isObject()) {
                        boolean status = jsonNode.get("alert").asBoolean();
                        logger.trace("alert {} {}", name, status);
                        alertStates.put(name, status);
                    }
                }
            } catch (Exception e) {
                logger.warn("unable to load status", e);
            }
        }

        return alertStates;
    }


    //todo - gfm - 5/20/15 -
    /*private void saveStatus() {
        ObjectNode status = mapper.createObjectNode();
        for (AlertChecker alertChecker : configCheckerMap.values()) {
            if (alertChecker.inProcess()) {
                logger.info("ignoring inProcess " + alertChecker.getAlertConfig().getName());
            } else {
                alertChecker.toJson(status);
            }
        }
        if (status.size() > 0) {
            String entity = status.toString();
            logger.info("saving status size {}", entity.length());
            logger.trace("saving status {}", entity);
            client.resource(hubAppUrl + "channel/" + alertStatusName)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .post(entity);
        } else {
            logger.info("no status to save");
        }
    }*/
}
