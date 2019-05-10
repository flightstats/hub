package com.flightstats.hub.dao.aws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.rest.RestClient;
import com.sun.jersey.api.client.ClientResponse;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipInputStream;

@Slf4j
@Path("/internal/s3Batch/{channel}")
public class S3BatchResource {

    private final ContentDao s3BatchContentDao;
    private final StatsdReporter statsdReporter;
    private final ObjectMapper objectMapper;

    @Inject
    public S3BatchResource(ContentDao s3BatchContentDao,
                           StatsdReporter statsdReporter,
                           ObjectMapper objectMapper) {
        this.s3BatchContentDao = s3BatchContentDao;
        this.statsdReporter = statsdReporter;
        this.objectMapper = objectMapper;
    }

    private boolean getAndWriteBatch(ContentDao contentDao, String channel, MinutePath path,
                                     Collection<ContentKey> keys, String batchUrl) {
        ActiveTraces.getLocal().add("S3BatchResource.getAndWriteBatch", path);
        final ClientResponse response = RestClient.defaultClient()
                .resource(batchUrl + "&location=CACHE_WRITE")
                .accept("application/zip")
                .get(ClientResponse.class);
        if (response.getStatus() != 200) {
            log.warn("unable to get data for {} {}", channel, response);
            return false;
        }
        ActiveTraces.getLocal().add("S3BatchResource.getAndWriteBatch got response");
        final byte[] bytes = response.getEntity(byte[].class);

        if(!verifyZipBytes(bytes)) {
            this.statsdReporter.increment("batch.invalid_zip");
            log.warn("S3BatchResource failed zip verification for keys: {}, channel: {}", keys, channel);
            return false;
        }

        contentDao.writeBatch(channel, path, keys, bytes);
        ActiveTraces.getLocal().add("S3BatchResource.getAndWriteBatch completed");
        return true;
    }

    private boolean verifyZipBytes(byte[] bytes) {
        final ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes));
        try {
            while (zis.getNextEntry() != null) ;
        } catch (Exception exception) {
            return false;
        }
        return true;
    }

    /**
     * This gets called back for channels to support S3 batching.
     */
    @POST
    public Response post(@PathParam("channel") String channel, String data) {
        try {
            log.debug("processing {} {}", channel, data);
            final JsonNode node = objectMapper.readTree(data);
            final ArrayNode uris = (ArrayNode) node.get("uris");
            if (uris.size() == 0) {
                return Response.ok().build();
            }
            final List<ContentKey> keys = new ArrayList<>();
            for (JsonNode uri : uris) {
                keys.add(ContentKey.fromFullUrl(uri.asText()));
            }

            final String id = node.get("id").asText();
            final MinutePath path = MinutePath.fromUrl(id).get();
            final String batchUrl = node.get("batchUrl").asText();
            if (!getAndWriteBatch(s3BatchContentDao, channel, path, keys, batchUrl)) {
                return Response.status(400).build();
            }
            return Response.ok().build();

        } catch (Exception e) {
            log.warn("unable to handle " + channel + " " + data, e);
        }
        return Response.status(400).build();
    }
}