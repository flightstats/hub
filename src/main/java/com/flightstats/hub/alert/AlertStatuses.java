package com.flightstats.hub.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.rest.RestClient;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

public class AlertStatuses {

    private final static Logger logger = LoggerFactory.getLogger(AlertStatuses.class);

    private final String hubAppUrl;
    private final static Client client = RestClient.defaultClient();
    private final String alertStatusName;
    private final static ObjectMapper mapper = new ObjectMapper();

    public AlertStatuses(String hubAppUrl) {
        this.hubAppUrl = hubAppUrl;
        alertStatusName = HubProperties.getProperty("alert.channel.status", "zomboAlertStatus");
    }

    public void create() {
        client.resource(hubAppUrl + "channel/" + alertStatusName)
                .type(MediaType.APPLICATION_JSON)
                .put("{\"ttlDays\":7, \"tags\":[\"alerts\"], \"description\":\"Status for hub alerts\"}");
    }

    public Map<String, AlertStatus> getLatest() {
        ClientResponse response = client.resource(hubAppUrl + "channel/" + alertStatusName + "/latest?stable=false")
                .get(ClientResponse.class);
        if (response.getStatus() >= 400) {
            logger.warn("unable to get latest from {} {}", alertStatusName, response);
        } else {
            try {
                String entity = response.getEntity(String.class);
                return AlertStatus.fromJson(entity);
            } catch (Exception e) {
                logger.warn("unable to load status", e);
            }
        }
        return new HashMap<>();
    }

    public void saveStatus(Map<String, AlertStatus> statusMap) {
        if (statusMap.size() > 0) {
            String json = AlertStatus.toJson(statusMap);
            logger.trace("saving status {}", json);
            client.resource(hubAppUrl + "channel/" + alertStatusName)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .post(json);
        } else {
            logger.info("no status to save");
        }
    }
}
