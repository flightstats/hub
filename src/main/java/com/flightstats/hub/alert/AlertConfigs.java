package com.flightstats.hub.alert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.GuiceContext;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.rest.RestClient;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AlertConfigs {

    private final static Logger logger = LoggerFactory.getLogger(AlertConfigs.class);

    private final static ObjectMapper mapper = GuiceContext.mapper;

    private static String getAlertConfigName() {
        return HubProperties.getProperty("alert.channel.config", "zomboAlertsConfig");
    }

    public static void create() {
        RestClient.defaultClient().resource(getUrl())
                .type(MediaType.APPLICATION_JSON)
                .put("{\"ttlDays\":1000, \"tags\":[\"alerts\"], \"description\":\"Configuration for hub alerts\"}");
    }

    private static String getUrl() {
        return HubProperties.getAppUrl() + "channel/" + getAlertConfigName();
    }

    public static Map<String, AlertConfig> getLatest() {
        Map<String, AlertConfig> alertConfigs = new HashMap<>();
        ClientResponse response = RestClient.defaultClient()
                .resource(getUrl() + "/latest?stable=false").get(ClientResponse.class);
        if (response.getStatus() >= 400) {
            logger.warn("unable to get latest from {} {}", getAlertConfigName(), response);
        } else {
            String config = response.getEntity(String.class);
            logger.debug("config {}", config);
            try {
                JsonNode rootNode = mapper.readTree(config);
                readType(alertConfigs, rootNode.get("insertAlerts"));

            } catch (IOException e) {
                logger.warn("unable to parse", e);
            }
        }
        return alertConfigs;
    }

    public static void upsert(AlertConfig alertConfig) {
        Map<String, AlertConfig> latest = getLatest();
        latest.put(alertConfig.getName(), alertConfig);
        update(latest);
    }

    public static void delete(String name) {
        Map<String, AlertConfig> latest = getLatest();
        latest.remove(name);
        update(latest);
    }

    private static void update(Map<String, AlertConfig> latest) {
        ObjectNode rootNode = mapper.createObjectNode();
        ObjectNode insertAlerts = rootNode.putObject("insertAlerts");
        for (AlertConfig config : latest.values()) {
            config.writeJson(insertAlerts.putObject(config.getName()));
        }
        logger.info("config {}", rootNode.toString());
        ClientResponse response = RestClient.defaultClient().resource(getUrl())
                .type(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class, rootNode.toString());
        logger.info("response {}", response);
    }

    private static void readType(Map<String, AlertConfig> alertConfigs, JsonNode node) {
        if (node == null) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            AlertConfig alertConfig = AlertConfig.fromJson(entry.getKey(), entry.getValue().toString());
            alertConfigs.put(entry.getKey(), alertConfig);
        }
    }
}
