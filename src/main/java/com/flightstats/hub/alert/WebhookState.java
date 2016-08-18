package com.flightstats.hub.alert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.webhook.Webhook;
import com.flightstats.hub.webhook.WebhookStatus;
import com.google.common.base.Optional;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class WebhookState {

    private final static Logger logger = LoggerFactory.getLogger(WebhookState.class);

    private final static Client client = RestClient.defaultClient();
    private final static ObjectMapper mapper = new ObjectMapper();

    static WebhookStatus getStatus(AlertConfig alertConfig) {
        String url = alertConfig.getHubDomain() + "webhook/" + alertConfig.getSource();
        logger.debug("calling {}", url);
        ClientResponse response = client.resource(url).get(ClientResponse.class);
        if (response.getStatus() >= 400) {
            logger.warn("unable to get latest from {} {}", alertConfig.getSource(), response);
        } else {
            String config = response.getEntity(String.class);
            logger.trace("{} config {}", alertConfig.getSource(), config);
            try {
                return parse(config);
            } catch (IOException e) {
                logger.warn("unable to parse", e);
            }
        }
        return null;
    }

    private static WebhookStatus parse(String config) throws IOException {
        JsonNode jsonNode = mapper.readTree(config);

        WebhookStatus.WebhookStatusBuilder builder = WebhookStatus.builder();

        builder.channelLatest(ContentKey.fromFullUrl(jsonNode.get("channelLatest").asText()));
        Optional<ContentPath> lastCompletedCallback = ContentPath.fromFullUrl(jsonNode.get("lastCompletedCallback").asText());
        if (lastCompletedCallback.isPresent()) {
            builder.lastCompleted(lastCompletedCallback.get());
        }
        Webhook webhook = Webhook.builder()
                .channelUrl(jsonNode.get("channelUrl").asText())
                .build();
        builder.webhook(webhook);
        return builder.build();
    }


}
