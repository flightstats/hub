package com.flightstats.hub.dao.aws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.rest.RestClient;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Path("/internal/s3Batch/{channel}")
public class S3BatchResource {

    private final static Logger logger = LoggerFactory.getLogger(S3BatchResource.class);

    private final ContentDao s3BatchContentDao;
    private final ObjectMapper mapper;
    private final boolean dropSomeWrites;

    @Inject
    S3BatchResource(@Named(ContentDao.BATCH_LONG_TERM) ContentDao s3BatchContentDao, ObjectMapper mapper, HubProperties hubProperties) {
        this.s3BatchContentDao = s3BatchContentDao;
        this.mapper = mapper;
        this.dropSomeWrites = hubProperties.getProperty("s3.dropSomeWrites", false);
    }

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
                keys.add(ContentKey.fromFullUrl(uri.asText()));
            }

            String id = node.get("id").asText();
            MinutePath path = MinutePath.fromUrl(id).get();
            String batchUrl = node.get("batchUrl").asText();
            if (dropSomeWrites && Math.random() > 0.90) {
                logger.debug("ignoring {} {}", channel, data);
                return Response.status(400).build();
            } else if (!getAndWriteBatch(s3BatchContentDao, channel, path, keys, batchUrl)) {
                return Response.status(400).build();
            }
            return Response.ok().build();

        } catch (Exception e) {
            logger.warn("unable to handle " + channel + " " + data, e);
        }
        return Response.status(400).build();
    }

    private boolean getAndWriteBatch(ContentDao contentDao, String channel, MinutePath path,
                                     Collection<ContentKey> keys, String batchUrl) {
        ActiveTraces.getLocal().add("S3BatchResource.getAndWriteBatch", path);
        ClientResponse response = RestClient.defaultClient()
                .resource(batchUrl + "&location=CACHE_WRITE")
                .accept("application/zip")
                .get(ClientResponse.class);
        if (response.getStatus() != 200) {
            logger.warn("unable to get data for {} {}", channel, response);
            return false;
        }
        ActiveTraces.getLocal().add("S3BatchResource.getAndWriteBatch got response");
        byte[] bytes = response.getEntity(byte[].class);
        contentDao.writeBatch(channel, path, keys, bytes);
        ActiveTraces.getLocal().add("S3BatchResource.getAndWriteBatch completed");
        return true;
    }
}
