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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
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
            path = "/" + path;
            if (curator.checkExists().forPath(path) == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            ObjectNode root = mapper.createObjectNode();
            ObjectNode links = root.putObject("_links");
            ObjectNode self = links.putObject("self");
            self.put("href", uriInfo.getRequestUri().toString());
            self.put("description", DESCRIPTION);
            handleData(path, root);
            handleChildren(path, root);
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

    private void handleChildren(String path, ObjectNode root) throws Exception {
        List<String> children = curator.getChildren().forPath(path);
        Collections.sort(children);
        ArrayNode ids = root.putArray("children");
        for (String child : children) {
            ids.add(uriInfo.getRequestUri().toString() + "/" + child);
        }
    }


}
