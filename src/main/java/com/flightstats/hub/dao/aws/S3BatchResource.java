package com.flightstats.hub.dao.aws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.MinutePath;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/internal/s3Batch/{channel}")
public class S3BatchResource {
    private final static Logger logger = LoggerFactory.getLogger(S3BatchResource.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    @Inject
    private Client client;

    @Inject
    @Named(ContentDao.BATCH_LONG_TERM)
    private ContentDao s3BatchContentDao;

    @Inject
    private ChannelService channelService;

    /**
     * This gets called back for channels to support S3 batching.
     */
    @POST
    public Response post(@PathParam("channel") String channel, String data) {
        try {
            logger.debug("processing {} {}", channel, data);
            JsonNode node = mapper.readTree(data);
            ArrayNode uris = (ArrayNode) node.get("uris");
            if (uris.size() == 0) {
                return Response.ok().build();
            }
            List<ContentKey> keys = new ArrayList<>();
            for (JsonNode uri : uris) {
                keys.add(ContentKey.fromFullUrl(uri.asText()).get());
            }

            String id = node.get("id").asText();
            MinutePath path = MinutePath.fromUrl(id).get();
            String batchUrl = node.get("batchUrl").asText();
            ClientResponse response = client.resource(batchUrl + "&location=CACHE")
                    .accept("application/zip")
                    .get(ClientResponse.class);
            if (response.getStatus() != 200) {
                logger.warn("unable to get data for {} {}", channel, response);
                return Response.status(response.getStatus()).build();
            }
            byte[] bytes = response.getEntity(byte[].class);
            s3BatchContentDao.writeBatch(channel, path, keys, bytes);
            return Response.ok().build();

        } catch (Exception e) {
            logger.warn("unable to handle " + channel + " " + data, e);
        }
        return Response.status(400).build();
    }
}
