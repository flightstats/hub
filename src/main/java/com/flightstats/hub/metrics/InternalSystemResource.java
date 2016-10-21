package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.util.Commander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@SuppressWarnings("WeakerAccess")
@Path("/internal/system")
public class InternalSystemResource {
    private final static Logger logger = LoggerFactory.getLogger(InternalSystemResource.class);

    private static final ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    public static final String DESCRIPTION = "Data about the system with links to other hubs in the cluster.";

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getTraces() {
        ObjectNode root = InternalTracesResource.serverAndServers("/internal/system");
        //todo gfm - return links for
        // cpu
        // log disk
        // spoke disk
        // ntp
        ObjectNode data = root.putObject("data");
        //data.put("cpu",)
        String s = Commander.run(new String[]{"rm", HubProperties.getSpokePath()}, 1);

        return Response.ok(root).build();
    }


}
