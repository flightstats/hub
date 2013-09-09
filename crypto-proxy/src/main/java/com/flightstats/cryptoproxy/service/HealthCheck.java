package com.flightstats.cryptoproxy.service;

import com.google.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/health")
public class HealthCheck {

    @Inject
    public HealthCheck() {
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String check() {
        return "OK";
    }

}
