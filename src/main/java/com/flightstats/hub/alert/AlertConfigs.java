package com.flightstats.hub.alert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.rest.RestClient;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AlertConfigs {

    private final static Logger logger = LoggerFactory.getLogger(AlertConfigs.class);

    private final static ObjectMapper mapper = new ObjectMapper();

    private static String getAlertConfigName() {
        return HubProperties.getProperty("alert.channel.config", "zomboAlertsConfig");
    }

    public static void create() {
        RestClient.defaultClient().resource(HubProperties.getAppUrl() + "channel/" + getAlertConfigName())
                .type(MediaType.APPLICATION_JSON)
                .put("{\"ttlDays\":1000, \"tags\":[\"alerts\"], \"description\":\"Configuration for hub alerts\"}");
    }

    public static List<AlertConfig> getLatest() {
        List<AlertConfig> alertConfigs = new ArrayList<>();
        String url = HubProperties.getAppUrl() + "channel/" + getAlertConfigName() + "/latest?stable=false";
        ClientResponse response = RestClient.defaultClient().resource(url).get(ClientResponse.class);
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

    private static void readType(List<AlertConfig> alertConfigs, JsonNode node) {
        if (node == null) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            AlertConfig alertConfig = AlertConfig.fromJson(entry.getKey(), HubProperties.getAppUrl(),
                    entry.getValue().toString());
            alertConfigs.add(alertConfig);
        }
    }
}
