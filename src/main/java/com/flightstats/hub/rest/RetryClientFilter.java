package com.flightstats.hub.rest;

import com.flightstats.hub.util.Sleeper;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;

/**
 * RetryClientFilter assumes that connection issues may be transient, so retry is a good idea.
 */
public class RetryClientFilter extends ClientFilter {
    private static final Logger logger = LoggerFactory.getLogger(RetryClientFilter.class);

    public ClientResponse handle(ClientRequest clientRequest) throws ClientHandlerException {
        //todo - gfm - 2/28/14 - change this to use com.github.rholder.retry.Retryer
        //todo - gfm - 1/26/14 - add these to config
        int maxRetries = 5;
        int sleep = 1000;
        ClientHandlerException lastCause = null;

        int attempt = 0;
        while (attempt < maxRetries) {
            attempt++;
            try {
                ClientResponse response = getNext().handle(clientRequest);
                if (response.getStatus() >= 500) {
                    //todo - gfm - 1/26/14 - look at Retry-After header
                    logger.info("500 level response {}  attempt={}", response, attempt);
                    if (attempt >= maxRetries) {
                        return response;
                    }
                } else {
                    return response;
                }
            } catch (ClientHandlerException e) {
                if (e.getCause() == null) {
                    throw e;
                }
                if (UnknownHostException.class.isAssignableFrom(e.getCause().getClass())) {
                    throw e;
                }
                lastCause = e;

                logger.info("exception {} retry count {} ", clientRequest.getURI().toString(), attempt);
                logger.debug(clientRequest.getURI().toString() + " stacktrace ", e);
            }
            Sleeper.sleep(sleep * 2 ^ attempt);
        }
        String msg = "Connection retries limit " + maxRetries + " exceeded for uri " + clientRequest.getURI();
        logger.warn(msg);
        throw lastCause;
    }
}

