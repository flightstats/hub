package com.flightstats.hub.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.cluster.Cluster;
import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.dao.ContentMarshaller;
import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.Query;
import com.flightstats.hub.webhook.Webhook;
import com.google.common.io.ByteStreams;
import javax.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

@Slf4j
@Singleton
public class HubUtils {

    private final Client noRedirectsClient;
    private final Client followClient;
    private final Cluster hubCluster;
    private final ObjectMapper objectMapper;
    private final String uriScheme;

    @Inject
    public HubUtils(@Named("NoRedirects") Client noRedirectsClient,
                    Client followClient,
                    @Named("HubCluster") Cluster hubCluster,
                    ObjectMapper objectMapper,
                    LocalHostProperties localHostProperties) {
        this.noRedirectsClient = noRedirectsClient;
        this.followClient = followClient;
        this.hubCluster = hubCluster;
        this.objectMapper = objectMapper;
        this.uriScheme = localHostProperties.getUriScheme();
    }

    public static void close(ClientResponse response) {
        if (response != null) {
            try {
                response.close();
            } catch (Exception e) {
                log.warn("unable to close " + e);
            }
        }
    }

    public static void closeQuietly(final Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (final IOException ioe) {
            // ignore
        }
    }

    public Optional<String> getLatest(String channelUrl) {
        ClientResponse response = null;
        try {
            response = noRedirectsClient.resource(appendSlash(channelUrl) + "latest")
                    .accept(MediaType.WILDCARD_TYPE)
                    .head();
            if (response.getStatus() != Response.Status.SEE_OTHER.getStatusCode()) {
                log.warn("latest not found for " + channelUrl + " " + response);
                return Optional.empty();
            }
            return Optional.of(response.getLocation().toString());
        } finally {
            HubUtils.close(response);
        }
    }

    private String appendSlash(String channelUrl) {
        if (!channelUrl.endsWith("/")) {
            channelUrl += "/";
        }
        return channelUrl;
    }

    public void startWebhook(Webhook webhook) {
        String groupUrl = getSourceUrl(webhook.getChannelUrl()) + "/group/" + webhook.getName();
        String json = webhook.toJson();
        log.debug("starting {} with {}", groupUrl, json);
        ClientResponse response = null;
        try {
            response = followClient.resource(groupUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON)
                    .put(ClientResponse.class, json);
            log.debug("start group response {}", response);
        } finally {
            HubUtils.close(response);
        }
    }

    public void stopGroupCallback(String groupName, String sourceChannel) {
        String groupUrl = getSourceUrl(sourceChannel) + "/group/" + groupName;
        log.debug("stopping {} ", groupUrl);
        ClientResponse response = null;
        try {
            response = followClient.resource(groupUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON)
                    .delete(ClientResponse.class);
            log.debug("stop group response {}", response);
        } finally {
            HubUtils.close(response);
        }

    }

    private String getSourceUrl(String sourceChannel) {
        return StringUtils.substringBefore(sourceChannel, "/channel/");
    }

    void putChannel(String channelUrl, ChannelConfig channelConfig) {
        log.debug("putting {} {}", channelUrl, channelConfig);
        ClientResponse response = null;
        try {
            response = followClient.resource(channelUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON)
                    .put(ClientResponse.class, channelConfig.toJson());
            log.trace("put channel response {} {}", channelConfig, response);
        } finally {
            HubUtils.close(response);
        }
    }

    public ChannelConfig getChannel(String channelUrl) {
        log.debug("getting {}", channelUrl);
        ClientResponse response = null;
        try {
            response = followClient.resource(channelUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON)
                    .get(ClientResponse.class);
            log.trace("get channel response {}", response);
            if (response.getStatus() >= 400) {
                return null;
            } else {
                return ChannelConfig.createFromJson(response.getEntity(String.class));
            }
        } finally {
            HubUtils.close(response);
        }
    }

    public ContentKey insert(String channelUrl, Content content) {
        WebResource.Builder resource = followClient.resource(channelUrl).getRequestBuilder();
        if (content.getContentType().isPresent()) {
            resource = resource.type(content.getContentType().get());
        }
        ClientResponse response = null;
        try {
            response = resource.post(ClientResponse.class, content.getData());
            log.trace("got response {}", response);
            if (response.getStatus() == 201) {
                return ContentKey.fromFullUrl(response.getLocation().toString());
            } else {
                return null;
            }
        } finally {
            HubUtils.close(response);
        }
    }

    public Content get(String channelUrl, ContentKey contentKey) {
        return getContent(channelUrl + "/" + contentKey.toUrl());
    }

    private Content getContent(String uri) {
        return getContent(uri, (response) -> createContent(uri, response, true));
    }

    public Content getContent(String uri, Function<ClientResponse, Content> handler) {
        ClientResponse response = null;
        try {
            response = followClient.resource(uri).get(ClientResponse.class);
            if (response.getStatus() == 200) {
                return handler.apply(response);
            } else {
                log.debug("unable to get {} {}", uri, response);
                return null;
            }
        } finally {
            HubUtils.close(response);
        }
    }

    private long getItemLength(Content content) {
        try {
            Content contentClone = Content.copy(content);
            return ContentMarshaller.toBytes(contentClone).length;
        } catch (IOException e) {
            return content.getSize();
        }
    }

    public Content createContent(String uri, ClientResponse response, boolean loadData) {
        Content.Builder builder = Content.builder()
                .withStream(response.getEntityInputStream())
                .withContentKey(ContentKey.fromFullUrl(uri));
        MultivaluedMap<String, String> headers = response.getHeaders();
        if (headers.containsKey("Content-Type")) {
            builder.withContentType(headers.getFirst("Content-Type"));
        }
        if (headers.containsKey("X-LargeItem")) {
            builder.withLarge(true);
        }

        Content content = builder.build();
        content.setContentLength(getItemLength(content));
        if (loadData) {
            content.getData();
        }
        return content;
    }

    public Collection<ContentKey> insert(String channelUrl, BulkContent content) {
        ClientResponse response = null;
        try {
            response = followClient.resource(channelUrl + "/bulk")
                    .type(content.getContentType())
                    .post(ClientResponse.class, ByteStreams.toByteArray(content.getStream()));
            log.trace("got response {}", response);
            if (response.getStatus() == 201) {
                return parseContentKeys(response);
            }
        } catch (IOException e) {
            log.warn("unable to insert bulk " + channelUrl, e);
        } finally {
            HubUtils.close(response);
        }
        return Collections.emptyList();
    }

    private Collection<ContentKey> parseContentKeys(ClientResponse response) throws IOException {
        Set<ContentKey> keys = new TreeSet<>();
        String entity = response.getEntity(String.class);
        JsonNode rootNode = objectMapper.readTree(entity);
        JsonNode uris = rootNode.get("_links").get("uris");
        for (JsonNode uri : uris) {
            keys.add(ContentKey.fromFullUrl(uri.asText()));
        }
        return keys;
    }

    public Collection<ContentKey> query(String channelUrl, Query query) {
        try {
            String queryUrl = channelUrl + query.getUrlPath();
            log.debug("calling {}", queryUrl);
            ClientResponse response = followClient.resource(queryUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .get(ClientResponse.class);
            log.trace("got response {}", response);
            if (response.getStatus() == 200) {
                return parseContentKeys(response);
            }
        } catch (IOException e) {
            log.warn("unable to query" + channelUrl + " " + query, e);
        }
        return Collections.emptyList();
    }

    public boolean delete(String channelUrl) {
        ClientResponse response = null;
        try {
            log.debug("deleting {}", channelUrl);
            response = followClient.resource(channelUrl).delete(ClientResponse.class);
            log.trace("got response {}", response);
            if (response.getStatus() == 202) {
                return true;
            }
        } catch (Exception e) {
            log.warn("unable to delete {}", channelUrl, e);
        } finally {
            HubUtils.close(response);
        }
        return false;
    }

    public ObjectNode refreshAll() {
        ObjectNode root = objectMapper.createObjectNode();
        Set<String> servers = this.hubCluster.getAllServers();
        for (String server : servers) {
            refreshServer(root, server);
        }
        return root;
    }

    private void refreshServer(ObjectNode root, String server) {
        try {
            String url = uriScheme + server + "/internal/channel/refresh?all=false";
            ClientResponse response = followClient.resource(url).get(ClientResponse.class);
            if (response.getStatus() == 200) {
                root.put(response.getEntity(String.class), "success");
            } else {
                root.put(response.getEntity(String.class), "failure");
            }
        } catch (Exception e) {
            log.warn("unable to refresh {}", server, e);
        }
    }

}
