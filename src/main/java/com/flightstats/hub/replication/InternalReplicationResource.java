package com.flightstats.hub.replication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.SecondPath;
import com.flightstats.hub.rest.RestClient;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/internal/repls/{channel}")
public class InternalReplicationResource {

    private final static Logger logger = LoggerFactory.getLogger(InternalReplicationResource.class);

    private static final ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private static final ChannelService channelService = HubProvider.getInstance(ChannelService.class);
    private static final LastContentPath lastReplicated = HubProvider.getInstance(LastContentPath.class);

    private static boolean getAndWriteBatch(String channel, ContentPath path,
                                            String batchUrl, int expectedItems) throws Exception {
        BulkContent bulkContent = getBulkContent(channel, path, batchUrl);
        if (bulkContent == null) {
            logger.warn("unable to get a result {} {} {}", channel, path, expectedItems);
            bulkContent = getBulkContent(channel, path, batchUrl);
        } else if (bulkContent.getItems().size() < expectedItems) {
            logger.warn("incorrect number of items {} {} {} {}", channel, path, expectedItems, bulkContent.getItems().size());
            getBulkContent(channel, path, batchUrl);
        }
        ActiveTraces.getLocal().add("getAndWriteBatch completed");
        return bulkContent != null;
    }

    private static BulkContent getBulkContent(String channel, ContentPath path, String batchUrl) {
        try {
            return doWork(channel, path, batchUrl);
        } catch (Exception e) {
            logger.warn("unexpected " + channel + " " + path, e);
        }
        return null;
    }

    private static BulkContent doWork(String channel, ContentPath path,
                                      String batchUrl) throws Exception {
        ActiveTraces.getLocal().add("getAndWriteBatch", path);
        logger.trace("path {} {}", path, batchUrl);
        ClientResponse response = RestClient.gzipClient()
                .resource(batchUrl)
                .accept("multipart/mixed")
                .get(ClientResponse.class);
        logger.trace("response.getStatus() {}", response.getStatus());
        if (response.getStatus() != 200) {
            logger.warn("unable to get data for {} {}", channel, response);
            return null;
        }
        ActiveTraces.getLocal().add("getAndWriteBatch got response", response.getStatus());

        BulkContent bulkContent = BulkContent.builder()
                .stream(response.getEntityInputStream())
                .contentType(response.getHeaders().getFirst("Content-Type"))
                .channel(channel)
                .isNew(false)
                .build();
        channelService.insert(bulkContent);
        return bulkContent;
    }

    @POST
    public Response putPayload(@PathParam("channel") String channel, String data) {
        logger.trace("incoming {} {}", channel, data);
        try {
            logger.debug("processing {} {}", channel, data);
            JsonNode node = mapper.readTree(data);
            SecondPath path = SecondPath.fromUrl(node.get("id").asText()).get();
            int expectedItems = node.get("uris").size();
            if (expectedItems > 0) {
                if (!getAndWriteBatch(channel, path, node.get("batchUrl").asText(), expectedItems)) {
                    return Response.status(500).build();
                }
            }
            lastReplicated.updateIncrease(path, channel, ChannelReplicator.REPLICATED_LAST_UPDATED);
            return Response.ok().build();

        } catch (Exception e) {
            logger.warn("unable to handle " + channel + " " + data, e);
        }
        return Response.status(500).build();
    }

}
