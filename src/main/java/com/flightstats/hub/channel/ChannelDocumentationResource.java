package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.DocumentationDao;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/channel/{channel}/doc")
public class ChannelDocumentationResource {

    private final static Logger logger = LoggerFactory.getLogger(ChannelDocumentationResource.class);

    private final DocumentationDao documentationDao;
    private final ChannelService channelService;
    private final Parser markdownParser = Parser.builder().build();
    private final HtmlRenderer markdownRenderer = HtmlRenderer.builder().build();

    @Inject
    ChannelDocumentationResource(DocumentationDao documentationDao, ChannelService channelService) {
        this.documentationDao = documentationDao;
        this.channelService = channelService;
    }

    @GET
    public Response get(@PathParam("channel") String channel, @HeaderParam("accept") String accept) {
        if (!channelService.channelExists(channel)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        String documentation = documentationDao.get(channel);
        if (documentation == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (accept.contains("text/html")) {
            String markedDownDoc = markdown(documentation);
            return Response.ok(markedDownDoc).build();
        } else {
            return Response.ok(documentation).build();
        }
    }

    private String markdown(String raw) {
        logger.debug("marking down {}", raw);
        Node document = markdownParser.parse(raw);
        return markdownRenderer.render(document);
    }

    @PUT
    public Response put(@PathParam("channel") String channel, String content) {
        if (!channelService.channelExists(channel)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        boolean success = documentationDao.upsert(channel, content.getBytes());
        if (success) {
            return Response.ok().entity(content).build();
        } else {
            return Response.serverError().build();
        }
    }

    @DELETE
    public Response delete(@PathParam("channel") String channel) {
        if (!channelService.channelExists(channel)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        boolean success = documentationDao.delete(channel);
        if (success) {
            return Response.noContent().build();
        } else {
            return Response.serverError().build();
        }
    }
}
