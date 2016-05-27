package com.flightstats.hub.alert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.group.Group;
import com.flightstats.hub.group.GroupStatus;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.rest.RestClient;
import com.google.common.base.Optional;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class GroupState {

    private final static Logger logger = LoggerFactory.getLogger(GroupState.class);

    private final static Client client = RestClient.defaultClient();
    private final static ObjectMapper mapper = new ObjectMapper();

    static GroupStatus getGroupStatus(AlertConfig alertConfig) {
        String url = alertConfig.getHubDomain() + "group/" + alertConfig.getSource();
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

    private static GroupStatus parse(String config) throws IOException {
        JsonNode jsonNode = mapper.readTree(config);

        GroupStatus.GroupStatusBuilder builder = GroupStatus.builder();

        Optional<ContentKey> latestKey = ContentKey.fromFullUrl(jsonNode.get("channelLatest").asText());
        if (latestKey.isPresent()) {
            builder.channelLatest(latestKey.get());
        }
        Optional<ContentPath> lastCompletedCallback = ContentPath.fromFullUrl(jsonNode.get("lastCompletedCallback").asText());
        if (lastCompletedCallback.isPresent()) {
            builder.lastCompleted(lastCompletedCallback.get());
        }
        Group group = Group.builder()
                .channelUrl(jsonNode.get("channelUrl").asText())
                .build();
        builder.group(group);
        return builder.build();
    }


}
