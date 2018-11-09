package com.flightstats.hub.dao.aws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.rest.RestClient;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipInputStream;

@SuppressWarnings("WeakerAccess")
@Path("/internal/s3Batch/{channel}")
public class S3BatchResource {
    private final static Logger logger = LoggerFactory.getLogger(S3BatchResource.class);

    private static final boolean dropSomeWrites = HubProperties.getProperty("s3.dropSomeWrites", false);
    private static final ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private static final ContentDao s3BatchContentDao = HubProvider.getInstance(ContentDao.class, ContentDao.BATCH_LONG_TERM);
    private static final MetricsService metricsService = HubProvider.getInstance(MetricsService.class);

    public static boolean getAndWriteBatch(ContentDao contentDao, String channel, MinutePath path,
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

        if(!verifyZipBytes(bytes)) {
            metricsService.increment("batch.invalid_zip");
            logger.warn("S3BatchResource failed zip verification for keys: {}, channel: {}", keys, channel);
            return false;
        }

        contentDao.writeBatch(channel, path, keys, bytes);
        ActiveTraces.getLocal().add("S3BatchResource.getAndWriteBatch completed");
        return true;
    }

    private static boolean verifyZipBytes(byte[] bytes)  {
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes));
        try {
            while (zis.getNextEntry() != null) ;
        }catch(Exception exception) {
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
}
