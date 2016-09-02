package com.flightstats.hub.cluster;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.metrics.InternalTracesResource;
import com.flightstats.hub.util.TimeUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.joda.time.DateTime;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

@SuppressWarnings("WeakerAccess")
@Path("/internal/curator")
public class InternalCuratorResource {

    private static final CuratorFramework curator = HubProvider.getInstance(CuratorFramework.class);
    public static final String DESCRIPTION = "See Curator Leaders with links to other hubs in the cluster.";

    @Context
    private UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLeaders() throws Exception {
        ObjectNode root = InternalTracesResource.serverAndServers("/internal/curator");
        ArrayNode leadersArray = root.withArray("leaders");
        Collection<InternalLeader> leaders = getLeadersData();

        for (InternalLeader leader : leaders) {
            ObjectNode leaderNode = leadersArray.addObject();
            leaderNode.put("name", leader.name);
            leaderNode.put("href", getZkUri(leader.name));
            leaderNode.put("count", leader.count);
            leaderNode.put("oldest", leader.oldest.toString());
        }
        return Response.ok(root).build();
    }

    private String getZkUri(String path) {
        return uriInfo.getBaseUriBuilder()
                .path("internal/zookeeper")
                .path(path)
                .build().toString();
    }

    private Collection<InternalLeader> getLeadersData() throws Exception {
        TreeSet<InternalLeader> internalLeaders = new TreeSet<>();
        Collection<CuratorLeader> leaders = LeaderRotator.getLeaders();
        for (CuratorLeader leader : leaders) {
            InternalLeader internalLeader = new InternalLeader();
            internalLeader.name = leader.getLeaderPath();
            List<String> children = curator.getChildren().forPath(leader.getLeaderPath());
            internalLeader.count = children.size();
            DateTime oldest = TimeUtil.now();
            for (String child : children) {
                String childPath = leader.getLeaderPath() + "/" + child;
                Stat stat = new Stat();
                curator.getData().storingStatIn(stat).forPath(childPath);
                DateTime created = new DateTime(stat.getCtime());
                if (created.isBefore(oldest)) {
                    oldest = created;
                }
            }
            internalLeader.oldest = oldest;
            internalLeaders.add(internalLeader);
        }
        return internalLeaders;
    }

    static class InternalLeader implements Comparable<InternalLeader> {
        String name;
        int count;
        DateTime oldest;

        @Override
        public int compareTo(InternalLeader o) {
            int diff = o.count - count;
            if (diff == 0) {
                diff = oldest.compareTo(o.oldest);
            }
            if (diff == 0) {
                diff = name.compareTo(o.name);
            }
            return diff;
        }
    }

}
