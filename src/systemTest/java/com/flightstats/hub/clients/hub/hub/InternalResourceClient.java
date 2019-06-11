package com.flightstats.hub.clients.hub.hub;

import com.flightstats.hub.model.Internal;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;

public interface InternalResourceClient {
    @GET("/health")
    Call<ResponseBody> getHealth();

    @GET("/internal/properties")
    Call<Internal> getProperties();
}
