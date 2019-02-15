package com.flightstats.hub.replication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.SecondPath;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.HubUtils;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/internal/repls/{channel}")
public class InternalReplicationResource {

    private final static Logger logger = LoggerFactory.getLogger(InternalReplicationResource.class);

    private static final ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private static final ChannelService channelService = HubProvider.getInstance(ChannelService.class);
    private static final LastContentPath lastReplicated = HubProvider.getInstance(LastContentPath.class);
    private static final HubUtils hubUtils = HubProvider.getInstance(HubUtils.class);

    @POST
    public Response putPayload(@PathParam("channel") String channel, String data) {
        try {
            JsonNode node = readData(channel, data);
            SecondPath path = SecondPath.fromUrl(node.get("id").asText()).get();
            JsonNode uris = node.get("uris");
            logger.trace("incoming {} {} ", channel, uris);
            int expectedItems = uris.size();
            if (expectedItems == 1) {
                if (!attemptSingle(channel, uris)) {
                    logger.warn("unable to handle " + channel + " " + uris);
                    return Response.status(500).build();
                }
            } else if (expectedItems > 1) {
                if (!attemptBatch(channel, path, node.get("batchUrl").asText())) {
                    if (!attemptSingle(channel, uris)) {
                        logger.warn("unable to handle " + channel + " " + uris);
                        return Response.status(500).build();
                    }
                }
            }
            lastReplicated.updateIncrease(path, channel, ChannelService.REPLICATED_LAST_UPDATED);
            logger.trace("handled {} {} ", channel, uris);
            return Response.ok().build();
        } catch (Exception e) {
            logger.warn("unable to handle " + channel + " " + data, e);
            return Response.status(500).build();
        } catch (Throwable e) {
            logger.error("Throwable unable to handle " + channel + " " + data, e);
            throw e;
        }
    }

    private boolean attemptSingle(String channel, JsonNode uris) {
        try {
            for (JsonNode jsonNode : uris) {
                String uri = jsonNode.asText();
                hubUtils.getContent(uri, (response) -> {
                    try {
                        Content content = hubUtils.createContent(uri, response, false);
                        content.replicated();
                        ContentKey inserted = channelService.insert(channel, content);
                        if (inserted == null) {
                            logger.warn("unable to process {} {}", channel, uri);
                            return null;
                        }
                        return content;
                    } catch (Exception e) {
                        logger.warn("issue with handling " + channel + " " + uri, e);
                        throw new RuntimeException(e);
                    }
                });
            }
            return true;
        } catch (Exception e) {
            logger.warn("what happened? " + channel + " " + uris, e);
            return false;
        }
    }

    JsonNode readData(String channel, String data) throws IOException {
        logger.trace("reading {} {}", channel, data);
        return mapper.readTree(data);
    }

    private static boolean attemptBatch(String channel, ContentPath path, String batchUrl) {
        BulkContent bulkContent = null;
        try {
            bulkContent = getAndWriteBatch(channel, path, batchUrl);
        } catch (Exception e) {
            logger.warn("unexpected " + channel + " " + path, e);
        }
        ActiveTraces.getLocal().add("attemptBatch completed", bulkContent);
        return bulkContent != null;
    }

    private static BulkContent getAndWriteBatch(String channel, ContentPath path,
                                                String batchUrl) throws Exception {
        ActiveTraces.getLocal().add("attemptBatch", path);
        logger.trace("path {} {}", path, batchUrl);
        ClientResponse response = null;
        BulkContent bulkContent;
        try {
            response = RestClient.gzipClient()
                    .resource(batchUrl)
                    .accept("multipart/mixed")
                    .get(ClientResponse.class);
            logger.trace("response.getStatus() {}", response.getStatus());
            if (response.getStatus() != 200) {
                logger.warn("unable to get data for {} {}", channel, response);
                return null;
            }
            ActiveTraces.getLocal().add("attemptBatch got response", response.getStatus());
            bulkContent = BulkContent.builder()
                    .stream(response.getEntityInputStream())
                    .contentType(response.getHeaders().getFirst("Content-Type"))
                    .channel(channel)
                    .isNew(false)
                    .build();
            channelService.insert(bulkContent);
        } finally {
            HubUtils.close(response);
        }
        return bulkContent;
    }


}
