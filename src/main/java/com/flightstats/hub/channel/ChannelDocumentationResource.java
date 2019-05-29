package com.flightstats.hub.channel;

import com.flightstats.hub.app.PermissionsChecker;
import com.flightstats.hub.dao.DocumentationDao;
import com.flightstats.hub.dao.aws.ContentRetriever;
import javax.inject.Inject;
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

@Slf4j
@Path("/channel/{channel}/doc")
public class ChannelDocumentationResource {

    private final static String READ_ONLY_FAILURE_MESSAGE = "attempted to %s against /channel documentation on read-only node %s";
    private final static Parser markdownParser = Parser.builder().build();
    private final static HtmlRenderer markdownRenderer = HtmlRenderer.builder().build();

    private final DocumentationDao documentationDao;
    private final ContentRetriever contentRetriever;
    private final PermissionsChecker permissionsChecker;

    @Inject
    public ChannelDocumentationResource(DocumentationDao documentationDao,
                                        ContentRetriever contentRetriever,
                                        PermissionsChecker permissionsChecker) {
        this.documentationDao = documentationDao;
        this.contentRetriever = contentRetriever;
        this.permissionsChecker = permissionsChecker;
    }

    @GET
    public Response get(@PathParam("channel") String channel,
                        @HeaderParam("accept") String accept) {
        if (!contentRetriever.isExistingChannel(channel)) {
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
        permissionsChecker.checkReadOnlyPermission(String.format(READ_ONLY_FAILURE_MESSAGE, "put", channel));
        if (!contentRetriever.isExistingChannel(channel)) {
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
        permissionsChecker.checkReadOnlyPermission(String.format(READ_ONLY_FAILURE_MESSAGE, "delete", channel));
        if (!contentRetriever.isExistingChannel(channel)) {
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
