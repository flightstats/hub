package com.flightstats.hub.clients.hub.internal;

import com.flightstats.hub.model.InternalProperties;
import retrofit2.Call;
import retrofit2.http.GET;

public interface InternalPropertiesResourceClient {
    @GET("/internal/properties")
    Call<InternalProperties> get();
}
