package com.flightstats.hub.app;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.concurrent.Callable;

@Slf4j
public class LocalHostOnly {

    public static Response getResponse(UriInfo uriInfo, Callable callable) throws Exception {
        if (isLocalhost(uriInfo)) {
            Object called = callable.call();
            if (callable instanceof Response) {
                return (Response) called;
            }
            return Response.ok().build();
        } else {
            String msg = "only calls from localhost are allowed";
            log.warn(msg);
            return Response.status(Response.Status.FORBIDDEN).entity(msg).build();
        }
    }

    public static boolean isLocalhost(UriInfo uriInfo) {
        return uriInfo.getBaseUri().toString().contains("localhost");
    }

}