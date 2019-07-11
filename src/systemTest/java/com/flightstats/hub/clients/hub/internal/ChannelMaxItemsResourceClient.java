package com.flightstats.hub.clients.hub.internal;

import retrofit2.Call;
import retrofit2.http.PATCH;
import retrofit2.http.Path;


public interface ChannelMaxItemsResourceClient {

    @PATCH("/internal/max-items/{channel}")
    Call<Object> enforceMaxItems(@Path("channel") String channel);
}
