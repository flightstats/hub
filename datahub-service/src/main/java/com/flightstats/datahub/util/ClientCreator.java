package com.flightstats.datahub.util;

import com.flightstats.datahub.rest.RetryClientFilter;
import com.sun.jersey.api.client.Client;

import java.util.concurrent.TimeUnit;

/**
 * Should this use Guice?
 */
public class ClientCreator {

    private static final Client cachedClient = create();

    /**
     * Creating clients is relatively expensive, so you may want to cache these locally.
     * This Client will retry by default.
     *
     * @return
     */
    public static Client create()
    {
        int connectTimeoutMillis = (int) TimeUnit.SECONDS.toMillis(30);
        int readTimeoutMillis = (int) TimeUnit.SECONDS.toMillis(60);

        Client client = Client.create();
        client.setConnectTimeout(connectTimeoutMillis);
        client.setReadTimeout(readTimeoutMillis);
        client.addFilter(new RetryClientFilter());
        client.setFollowRedirects(false);
        return client;
    }

    /**
     * This returns a cached common version of create().
     * You should not make any changes to this client.
     *
     * @return
     */
    public static Client cached()
    {
        return cachedClient;
    }

}
