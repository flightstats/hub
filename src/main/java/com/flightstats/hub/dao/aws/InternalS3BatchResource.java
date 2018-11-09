package com.flightstats.hub.dao.aws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.LocalHostOnly;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.rest.RestClient;
import com.sun.jersey.api.client.ClientResponse;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipInputStream;

@Slf4j
@Path("/internal/s3Batch")
public class InternalS3BatchResource {

    public static final String DESCRIPTION = "Perform administrative tasks against batch payloads";

    private final ObjectMapper objectMapper;
    private final S3BatchContentDao s3BatchContentDao;
    private final StatsdReporter statsdReporter;

    @Context
    UriInfo uriInfo;

    @Inject
    public InternalS3BatchResource(S3BatchContentDao s3BatchContentDao, ObjectMapper objectMapper, StatsdReporter statsdReporter) {
        this.s3BatchContentDao = s3BatchContentDao;
        this.objectMapper = objectMapper;
        this.statsdReporter = statsdReporter;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response index() {
        ObjectNode root = objectMapper.createObjectNode();

        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());

        root.put("description", DESCRIPTION);

        ObjectNode directions = root.putObject("directions");
        directions.put("archive/{batchPath}", "HTTP POST to /internal/s3Batch/archive/{channel}/{year}/{month}/{day}/{hour}/{minute}");

        return Response.ok(root).build();
    }

    @POST
    @Path("/archive/{channel}/{year}/{month}/{day}/{hour}/{minute}")
    public Response archiveBatch(@PathParam("channel") String channel,
                                 @PathParam("year") int year,
                                 @PathParam("month") int month,
                                 @PathParam("day") int day,
                                 @PathParam("hour") int hour,
                                 @PathParam("minute") int minute) {
        if (HubProperties.isProtected() && !LocalHostOnly.isLocalhost(uriInfo)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } else {
            ContentKey key = new ContentKey(year, month, day, hour, minute);
            s3BatchContentDao.archiveBatch(channel, key);
            return Response.status(Response.Status.OK).build();
        }
    }


    /**
     * This gets called back for channels to support S3 batching.
     */
    @POST
    @Path("/{channel}")
    public Response post(@PathParam("channel") String channel, String data) {
        try {
            log.debug("processing {} {}", channel, data);
            JsonNode node = objectMapper.readTree(data);
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
            if (!getAndWriteBatch(s3BatchContentDao, channel, path, keys, batchUrl)) {
                return Response.status(400).build();
            }
            return Response.ok().build();

        } catch (Exception e) {
            log.warn("unable to handle " + channel + " " + data, e);
        }
        return Response.status(400).build();
    }

    private boolean getAndWriteBatch(ContentDao contentDao, String channel, MinutePath path,
                                     Collection<ContentKey> keys, String batchUrl) {
        ActiveTraces.getLocal().add("InternalS3BatchResource.getAndWriteBatch", path);
        ClientResponse response = RestClient.defaultClient()
                .resource(batchUrl + "&location=CACHE_WRITE")
                .accept("application/zip")
                .get(ClientResponse.class);
        if (response.getStatus() != 200) {
            log.warn("unable to get data for {} {}", channel, response);
            return false;
        }
        ActiveTraces.getLocal().add("InternalS3BatchResource.getAndWriteBatch got response");
        byte[] bytes = response.getEntity(byte[].class);

        if(!verifyZipBytes(bytes)) {
            statsdReporter.increment("batch.invalid_zip");
            log.warn("InternalS3BatchResource failed zip verification for keys: {}, channel: {}", keys, channel);
            return false;
        }

        contentDao.writeBatch(channel, path, keys, bytes);
        ActiveTraces.getLocal().add("InternalS3BatchResource.getAndWriteBatch completed");
        return true;
    }

    private boolean verifyZipBytes(byte[] bytes) {
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes));
        try {
            while (zis.getNextEntry() != null) ;
        } catch (Exception exception) {
            return false;
        }
        return true;
    }
}
