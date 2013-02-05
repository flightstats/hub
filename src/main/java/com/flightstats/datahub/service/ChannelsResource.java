package com.flightstats.datahub.service;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/channels")
public class ChannelsResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getChannels(){
        throw new RuntimeException("Metadata is not yet implemented");
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public void createChannel(){

    }
}
