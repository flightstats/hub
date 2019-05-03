package com.flightstats.hub.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.primitives.Longs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.List;

import static com.flightstats.hub.util.Constants.ZOOKEEPER_DESCRIPTION;

@Slf4j
@Path("/internal/zookeeper/")
public class InternalZookeeperResource {

    private final CuratorFramework curator;
    private final ObjectMapper objectMapper;

    @Context
    private UriInfo uriInfo;

    @Inject
    public InternalZookeeperResource(CuratorFramework curator,
                                     ObjectMapper objectMapper) {
        this.curator = curator;
        this.objectMapper = objectMapper;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoot(@QueryParam("depth") @DefaultValue("1") int depth) {
        return returnData("", depth, 0);
    }

    @GET
    @Path("/{path:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPath(@PathParam("path") String path,
                            @QueryParam("depth") @DefaultValue("1") int depth,
                            @QueryParam("olderThanDays") @DefaultValue("0") int olderThanDays) {
        return returnData(path, depth, olderThanDays);
    }

    private Response returnData(String path, int depth, int olderThanDays) {
        log.info("path {}, depth {}, olderThanDays {}", path, depth, olderThanDays);
        depth = Math.max(1, Math.min(2, depth));
        try {
            path = StringUtils.removeEnd(path, "/");
            path = "/" + path;
            if (curator.checkExists().forPath(path) == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode links = root.putObject("_links");
            ObjectNode self = links.putObject("self");
            self.put("href", uriInfo.getRequestUri().toString());
            self.put("description", ZOOKEEPER_DESCRIPTION);
            ObjectNode depthLink = links.putObject("depth");
            depthLink.put("href", uriInfo.getAbsolutePathBuilder().replaceQueryParam("depth", "2").build().toString());
            depthLink.put("description", "Use depth=2 to see child counts two levels deep.");
            ObjectNode olderThanLink = links.putObject("olderThanDays");
            olderThanLink.put("href", uriInfo.getAbsolutePathBuilder().replaceQueryParam("olderThanDays", "14").build().toString());
            olderThanLink.put("description", "Use olderThanDays to limit the results to leaf nodes modfied more than olderThanDays ago.");
            handleData(path, root);
            handleChildren(path, root, depth, olderThanDays);
            return Response.ok(root).build();
        } catch (Exception e) {
            log.warn("unable to get path " + path, e);
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
            log.info("unable to convert to string ", path);
        }
        ObjectNode stats = root.putObject("stats");
        stats.put("created", new DateTime(stat.getCtime()).toString());
        stats.put("modified", new DateTime(stat.getMtime()).toString());
        stats.put("changes", stat.getVersion());
        if (stat.getEphemeralOwner() != 0) {
            stats.put("sessionId", stat.getEphemeralOwner());
        }
    }

    private void handleChildren(String path, ObjectNode root, int depth, int olderThanDays) throws Exception {
        DateTime olderThanTime = TimeUtil.now().minusDays(olderThanDays);
        List<String> children = curator.getChildren().forPath(path);
        Collections.sort(children);
        ArrayNode childNodes = root.putArray("children");
        for (String child : children) {
            if (olderThanDays > 0) {
                Stat stat = new Stat();
                curator.getData().storingStatIn(stat).forPath(getPath(path, child));
                if (olderThanTime.isAfter(stat.getMtime())) {
                    ObjectNode childNode = childNodes.addObject();
                    String link = getPath(uriInfo.getAbsolutePath().toString(), child);
                    childNode.put("href", link);
                    childNode.put("modified", new DateTime(stat.getMtime()).toString());
                }
            } else {
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
    }

    private String getPath(String path, String child) {
        if (path.equals("/")) {
            return "/" + child;
        }
        return path + "/" + child;
    }


}
