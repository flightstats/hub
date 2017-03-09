package com.flightstats.hub.channel;

import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.DocumentationDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/channel/{channel}/doc")
public class ChannelDocumentationResource {

    private final static Logger logger = LoggerFactory.getLogger(ChannelDocumentationResource.class);
    private final static DocumentationDao documentationDao = HubProvider.getInstance(DocumentationDao.class);

    @GET
    public Response get(@PathParam("channel") String channel) {
        String documentation = documentationDao.get(channel);
        if (documentation == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            String markedDownDoc = markdown(documentation);
            return Response.ok(markedDownDoc).build();
        }
    }

    private String markdown(String raw) {
        // TODO: parse the raw data and apply mark down rules
        logger.debug("marking down {}", raw);
        return raw;
    }

    @PUT
    public Response put(@PathParam("channel") String channel, String content) {
        boolean success = documentationDao.upsert(channel, content.getBytes());
        if (success) {
            return Response.accepted().build();
        } else {
            return Response.serverError().build();
        }
    }
}
