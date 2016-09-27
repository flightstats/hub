package com.flightstats.hub.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.concurrent.Callable;

public class LocalHostOnly {
    private final static Logger logger = LoggerFactory.getLogger(LocalHostOnly.class);

    public static Response getResponse(UriInfo uriInfo, Callable callable) throws Exception {
        if (isLocalhost(uriInfo)) {
            Object called = callable.call();
            if (callable instanceof Response) {
                return (Response) called;
            }
            return Response.ok().build();
        } else {
            String msg = "only calls from localhost are allowed";
            logger.warn(msg);
            return Response.status(405).entity(msg).build();
        }
    }

    public static boolean isLocalhost(UriInfo uriInfo) {
        return uriInfo.getBaseUri().toString().contains("localhost");
    }
}