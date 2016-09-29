package com.flightstats.hub.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import com.google.common.primitives.Longs;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("WeakerAccess")
@Path("/internal/zookeeper/")
public class InternalZookeeperResource {
    private static final Logger logger = LoggerFactory.getLogger(InternalZookeeperResource.class);

    private static final ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private static final CuratorFramework curator = HubProvider.getInstance(CuratorFramework.class);
    public static final String DESCRIPTION = "Read-only interface into the ZooKeeper hierarchy.";

    @Context
    private UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoot(@QueryParam("depth") @DefaultValue("1") int depth) {
        return returnData("", depth);
    }

    @GET
    @Path("/{path:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPath(@PathParam("path") String path, @QueryParam("depth") @DefaultValue("1") int depth) {
        return returnData(path, depth);
    }

    private Response returnData(String path, int depth) {
        depth = Math.max(1, Math.min(2, depth));
        try {
            path = StringUtils.removeEnd(path, "/");
            path = "/" + path;
            if (curator.checkExists().forPath(path) == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            ObjectNode root = mapper.createObjectNode();
            ObjectNode links = root.putObject("_links");
            ObjectNode self = links.putObject("self");
            URI requestUri = uriInfo.getRequestUri();
            self.put("href", requestUri.toString());
            self.put("description", DESCRIPTION);
            ObjectNode depthLink = links.putObject("depth");
            depthLink.put("href", UriBuilder.fromUri(requestUri).replaceQueryParam("depth", "2").build().toString());
            depthLink.put("description", "Use depth=2 to see child counts two levels deep.");
            handleData(path, root);
            handleChildren(path, root, depth);
            return Response.ok(root).build();
        } catch (Exception e) {
            logger.warn("unable to get path " + path, e);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

    }

    private void handleData(String path, ObjectNode root) throws Exception {
        Stat stat = new Stat();
        byte[] bytes = curator.getData().storingStatIn(stat).forPath(path);
        ObjectNode data = root.putObject("data");
        try {
            data.put("bytes", bytes);
            data.put("string", new String(bytes));
            data.put("long", Longs.fromByteArray(bytes));
        } catch (Exception e) {
            logger.info("unable to convert to string ", path);
        }
        ObjectNode stats = root.putObject("stats");
        stats.put("created", new DateTime(stat.getCtime()).toString());
        stats.put("modified", new DateTime(stat.getMtime()).toString());
        stats.put("changes", stat.getVersion());
        if (stat.getEphemeralOwner() != 0) {
            stats.put("sessionId", stat.getEphemeralOwner());
        }
    }

    private void handleChildren(String path, ObjectNode root, int depth) throws Exception {
        List<String> children = curator.getChildren().forPath(path);
        Collections.sort(children);
        ArrayNode childNodes = root.putArray("children");
        for (String child : children) {
            String link = getPath(uriInfo.getAbsolutePath().toString(), child) + "?depth=" + depth;
            ObjectNode childNode = childNodes.addObject();
            childNode.put("href", link);
            if (depth >= 1) {
                List<String> depthOne = curator.getChildren().forPath(getPath(path, child));
                if (depthOne.size() > 0) {
                    childNode.put("children", depthOne.size());
                    if (depth >= 2) {
                        int subNodes = 0;
                        for (String subChildren : depthOne) {
                            List<String> depthTwo = curator.getChildren().forPath(getPath(path, child) + "/" + subChildren);
                            subNodes += depthTwo.size();
                        }
                        if (subNodes > 0) {
                            childNode.put("subChildren", subNodes);
                        }
                    }
                }
            }

        }
    }

    private String getPath(String path, String child) {
        if (path.equals("/")) {
            return "/" + child;
        }
        return path + "/" + child;
    }


}
