package com.flightstats.hub.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.rest.RestClient;
import com.google.common.base.Optional;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class AlertStatuses {

    private final static Logger logger = LoggerFactory.getLogger(AlertStatuses.class);

    private final static Client client = RestClient.defaultClient();
    private final static ObjectMapper mapper = new ObjectMapper();

    public static void create() {
        client.resource(HubProperties.getAppUrl() + "channel/" + getAlertStatusName())
                .type(MediaType.APPLICATION_JSON)
                .put("{\"ttlDays\":7, \"tags\":[\"alerts\"], \"description\":\"Status for hub alerts\"}");
    }

    public static Map<String, AlertStatus> getLatestMap() {
        ClientResponse response = getLatestResponse();
        if (response.getStatus() >= 400) {
            logger.warn("unable to get latest from {} {}", getAlertStatusName(), response);
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

    private static ClientResponse getLatestResponse() {
        String url = HubProperties.getAppUrl() + "channel/" + getAlertStatusName() + "/latest?stable=false";
        logger.trace("calling {}", url);
        return client.resource(url).get(ClientResponse.class);
    }

    public static Optional<ContentKey> getLatestKey() {
        ClientResponse response = getLatestResponse();
        URI location = response.getLocation();
        if (location == null) {
            return Optional.absent();
        }
        return ContentKey.fromFullUrl(location.toString());
    }

    public static void saveStatus(Map<String, AlertStatus> statusMap) {
        if (statusMap.size() > 0) {
            String json = AlertStatus.toJson(statusMap);
            logger.trace("saving status {}", json);
            client.resource(HubProperties.getAppUrl() + "channel/" + getAlertStatusName())
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .post(json);
        } else {
            logger.info("no status to save");
        }
    }

    private static String getAlertStatusName() {
        return HubProperties.getProperty("alert.channel.status", "zomboAlertStatus");
    }
}
