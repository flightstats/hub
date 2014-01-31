package com.flightstats.hub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 *
 */
@Path("/zookeeper/")
public class ZookeeperResource
{
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperResource.class);

    private final CuratorFramework curator;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Context
    UriInfo uriInfo;

    @Inject
    public ZookeeperResource(CuratorFramework curator) {
        this.curator = curator;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoot() {
        return returnData("");
    }

    @GET
    @Path("/{path:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPath(@PathParam("path") String path) {
        return returnData(path);
    }

    private Response returnData(String path) {
        try {
            path = StringUtils.removeEnd(path, "/");
            if (!path.isEmpty()) {
                path = "/" + path;
            }
            if (curator.checkExists().forPath(path) == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            List<String> children = curator.getChildren().forPath(path);
            ObjectNode root = mapper.createObjectNode();
            ObjectNode links = root.putObject("_links");
            ObjectNode self = links.putObject("self");
            String href = uriInfo.getRequestUri().toString();
            self.put("href", href);
            ArrayNode ids = links.putArray("children");
            for (String child : children) {
                ids.add(href + "/" +child);
            }
            //todo - gfm - 1/12/14 - include data for path?
            return Response.ok(root).build();
        } catch (Exception e) {
            logger.warn("unable to get path " + path, e);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

    }




}
