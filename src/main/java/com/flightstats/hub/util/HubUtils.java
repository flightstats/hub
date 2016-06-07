package com.flightstats.hub.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.group.Group;
import com.flightstats.hub.model.*;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

@Singleton
public class HubUtils {

    private final static Logger logger = LoggerFactory.getLogger(HubUtils.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Client noRedirectsClient;
    private final Client followClient;

    @Inject
    public HubUtils(@Named("NoRedirects") Client noRedirectsClient, Client followClient) {
        this.noRedirectsClient = noRedirectsClient;
        this.followClient = followClient;
    }

    public Optional<String> getLatest(String channelUrl) {
        channelUrl = appendSlash(channelUrl);
        ClientResponse response = noRedirectsClient.resource(channelUrl + "latest")
                .accept(MediaType.WILDCARD_TYPE)
                .head();
        if (response.getStatus() != Response.Status.SEE_OTHER.getStatusCode()) {
            logger.info("latest not found for " + channelUrl + " " + response);
            return Optional.absent();
        }
        return Optional.of(response.getLocation().toString());
    }

    private String appendSlash(String channelUrl) {
        if (!channelUrl.endsWith("/")) {
            channelUrl += "/";
        }
        return channelUrl;
    }

    public ClientResponse startGroupCallback(Group group) {
        String groupUrl = getSourceUrl(group.getChannelUrl()) + "/group/" + group.getName();
        String json = group.toJson();
        logger.info("starting {} with {}", groupUrl, json);
        ClientResponse response = followClient.resource(groupUrl)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .put(ClientResponse.class, json);
        logger.info("start group response {}", response);
        return response;
    }

    public void stopGroupCallback(String groupName, String sourceChannel) {
        String groupUrl = getSourceUrl(sourceChannel) + "/group/" + groupName;
        logger.info("stopping {} ", groupUrl);
        ClientResponse response = followClient.resource(groupUrl)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .delete(ClientResponse.class);
        logger.debug("stop group response {}", response);

    }

    private String getSourceUrl(String sourceChannel) {
        return StringUtils.substringBefore(sourceChannel, "/channel/");
    }

    public boolean putChannel(String channelUrl, ChannelConfig channelConfig) {
        logger.debug("putting {} {}", channelUrl, channelConfig);
        ClientResponse response = followClient.resource(channelUrl)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .put(ClientResponse.class, channelConfig.toJson());
        logger.info("put channel response {} {}", channelConfig, response);
        return response.getStatus() < 400;
    }

    public ChannelConfig getChannel(String channelUrl) {
        logger.debug("getting {} {}", channelUrl);
        ClientResponse response = followClient.resource(channelUrl)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);
        logger.debug("get channel response {} {}", response);
        if (response.getStatus() >= 400) {
            return null;
        } else {
            return ChannelConfig.fromJson(response.getEntity(String.class));
        }
    }

    public ContentKey insert(String channelUrl, Content content) {
        WebResource.Builder resource = followClient.resource(channelUrl).getRequestBuilder();
        if (content.getContentType().isPresent()) {
            resource = resource.type(content.getContentType().get());
        }
        ClientResponse response = resource.post(ClientResponse.class, content.getData());
        logger.trace("got repsonse {}", response);
        if (response.getStatus() == 201) {
            return ContentKey.fromFullUrl(response.getLocation().toString());
        } else {
            return null;
        }
    }

    public Content get(String channelUrl, ContentKey contentKey) {
        ClientResponse response = followClient.resource(channelUrl + "/" + contentKey.toUrl())
                .get(ClientResponse.class);
        if (response.getStatus() == 200) {
            Content.Builder builder = Content.builder()
                    .withStream(response.getEntityInputStream())
                    .withContentKey(contentKey);
            MultivaluedMap<String, String> headers = response.getHeaders();
            if (headers.containsKey("Content-Type")) {
                builder.withContentType(headers.getFirst("Content-Type"));
            }
            return builder.build();
        } else {
            logger.info("unable to get {} {} {}", channelUrl, contentKey, response);
            return null;
        }
    }

    public Collection<ContentKey> insert(String channelUrl, BulkContent content) {
        try {
            ClientResponse response = followClient.resource(channelUrl + "/bulk")
                    .type(content.getContentType())
                    .post(ClientResponse.class, ByteStreams.toByteArray(content.getStream()));
            logger.trace("got response {}", response);
            if (response.getStatus() == 201) {
                return parseContentKeys(response);
            }
        } catch (IOException e) {
            logger.warn("unable to insert bulk " + channelUrl, e);
        }
        return Collections.emptyList();
    }

    private Collection<ContentKey> parseContentKeys(ClientResponse response) throws IOException {
        Set<ContentKey> keys = new TreeSet<>();
        String entity = response.getEntity(String.class);
        JsonNode rootNode = mapper.readTree(entity);
        JsonNode uris = rootNode.get("_links").get("uris");
        for (JsonNode uri : uris) {
            keys.add(ContentKey.fromFullUrl(uri.asText()));
        }
        return keys;
    }

    public Collection<ContentKey> query(String channelUrl, Query query) {
        try {
            String queryUrl = channelUrl + query.getUrlPath();
            logger.debug("calling {}", queryUrl);
            ClientResponse response = followClient.resource(queryUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .get(ClientResponse.class);
            logger.trace("got response {}", response);
            if (response.getStatus() == 200) {
                return parseContentKeys(response);
            }
        } catch (IOException e) {
            logger.warn("unable to query" + channelUrl + " " + query, e);
        }
        return Collections.emptyList();
    }

    public boolean delete(String channelUrl) {
        try {
            logger.info("deleting {}", channelUrl);
            ClientResponse response = followClient.resource(channelUrl).delete(ClientResponse.class);
            logger.trace("got response {}", response);
            if (response.getStatus() == 202) {
                return true;
            }
        } catch (Exception e) {
            logger.warn("unable to delete " + channelUrl, e);
        }
        return false;
    }
}
