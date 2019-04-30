package com.flightstats.hub.channel;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.DocumentationDao;
import com.flightstats.hub.exception.ForbiddenRequestException;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/channel/{channel}/doc")
@Slf4j
public class ChannelDocumentationResource {
    private final static DocumentationDao documentationDao = HubProvider.getInstance(DocumentationDao.class);
    private final static ChannelService channelService = HubProvider.getInstance(ChannelService.class);
    private final static Parser markdownParser = Parser.builder().build();
    private final static HtmlRenderer markdownRenderer = HtmlRenderer.builder().build();

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
        log.debug("marking down {}", raw);
        Node document = markdownParser.parse(raw);
        return markdownRenderer.render(document);
    }

    @PUT
    public Response put(@PathParam("channel") String channel, String content) {
        checkPermission("put", channel);
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
        checkPermission("delete", channel);
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

    private void checkPermission(String task, String name) {
        if (HubProperties.isReadOnly()) {
            String msg = String.format("attempted to %s against /channel documentation on read-only node %s", task, name);
            log.warn(msg);
            throw new ForbiddenRequestException(msg);
        }
    }
}
