package com.flightstats.hub.alert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProperties;
import com.sun.jersey.api.client.Client;
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

    private final String hubAppUrl;
    private final Client client;
    private final String alertConfigName;
    private final static ObjectMapper mapper = new ObjectMapper();

    public AlertConfigs(String hubAppUrl, Client client) {
        this.hubAppUrl = hubAppUrl;
        this.client = client;
        alertConfigName = HubProperties.getProperty("alert.channel.config", "zomboAlertsConfig");
    }

    public void create() {
        client.resource(hubAppUrl + "channel/" + alertConfigName)
                .type(MediaType.APPLICATION_JSON)
                .put("{\"ttlDays\":1000, \"description\":\"Configuration for hub alerts\"}");
    }

    public List<AlertConfig> getLatest() {
        List<AlertConfig> alertConfigs = new ArrayList<>();
        ClientResponse response = client.resource(hubAppUrl + "channel/" + alertConfigName + "/latest?stable=false")
                .get(ClientResponse.class);
        if (response.getStatus() >= 400) {
            logger.warn("unable to get latest from {} {}", alertConfigName, response);
        } else {
            String config = response.getEntity(String.class);
            logger.debug("config {}", config);
            try {
                JsonNode insertAlerts = mapper.readTree(config).get("insertAlerts");
                Iterator<Map.Entry<String, JsonNode>> fields = insertAlerts.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    AlertConfig alertConfig = AlertConfig.fromJson(entry.getKey(), hubAppUrl, entry.getValue().toString());
                    alertConfigs.add(alertConfig);
                }
            } catch (IOException e) {
                logger.warn("unable to parse", e);
            }
        }
        return alertConfigs;
    }
}
